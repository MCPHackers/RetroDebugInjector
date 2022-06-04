package org.mcphackers.rdi.injector.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mcphackers.rdi.injector.Injector;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class FixBridges implements Injection {
	
	private Injector injector;

	private Map<ClassNode, List<ClassNode>> genericsTree = new HashMap<>();
	private Map<ClassNode, List<String>> globalGuessedGenerics = new HashMap<>();
	private Map<ClassNode, Set<BridgePair>> bridges = new HashMap<>();

	private List<ClassNode> getParametrizedChildren(ClassNode node) {
		return genericsTree.get(node);
	}

	private ClassNode getParametrizedParent(ClassNode node) {
		for (Entry<ClassNode, List<ClassNode>> classNodes : genericsTree.entrySet()) {
			if(classNodes.getValue().contains(node)) {
				return classNodes.getKey();
			}
		}
		return null;
		
	}

	public FixBridges(Injector injector) {
		this.injector = injector;
	}
	
	private void deleteBridges(ClassNode currentNode, Map<String, String> cachedNames) {
		if(getParametrizedChildren(currentNode) != null) {
			for(ClassNode node : getParametrizedChildren(currentNode)) {
				Set<BridgePair> forRemoval = bridges.get(node);
				if(forRemoval != null) {
					for(BridgePair pair : forRemoval) {
						// Removing bridges
						if(!cachedNames.containsKey(pair.renamedMethod.name)) {
							cachedNames.put(pair.renamedMethod.name, pair.removedMethod.name);
						}
						String rename = pair.removedMethod.name;
						while (cachedNames.containsKey(rename) && cachedNames.get(rename) != null) {
							rename = cachedNames.get(rename);
						}
						for(MethodNode method : node.methods) {
				            for(AbstractInsnNode insn = method.instructions.getFirst(); insn != null ; insn = insn.getNext()) {
				                if(insn.getOpcode() != Opcodes.INVOKESPECIAL) {
				                    continue;
				                }
				            	MethodInsnNode invokespecial = (MethodInsnNode) insn;
				            	if(currentNode.name.equals(invokespecial.owner) && pair.removedMethod.name.equals(invokespecial.name)) {
				            		invokespecial.name = rename;
				            	}
				            }
						}
						pair.renamedMethod.name = rename;
						node.methods.remove(pair.removedMethod);
					}
				}
				deleteBridges(node, cachedNames);
			}
		}
	}
	
	@Override
	public void transform() {
		for(ClassNode node : injector.getClasses()) {
			if(!fixComparator(node)) {
				fixOtherBridges(node);
			}
		}
		for (Entry<ClassNode, List<ClassNode>> entry : genericsTree.entrySet()) {
			ClassNode current = entry.getKey();
			setGenerics(current);
			for(ClassNode cl : entry.getValue()) {
				setGenerics(cl);
			}
		}
		for (Entry<ClassNode, List<ClassNode>> entry : genericsTree.entrySet()) {
			ClassNode startNode = entry.getKey();
			if(getParametrizedParent(startNode) == null) {
				Map<String, String> renamingMap = new HashMap<>();
				for(MethodNode method : startNode.methods) {
					// I know this look dumb, but it's there to prevent other bridge methods occupying renaming keys of their parent
					renamingMap.put(method.name, null);
				}
				deleteBridges(startNode, renamingMap);
			}
		}
	}
	
	private static String getMethodSig(List<String> generics, String desc) {
		char startChar = 'T';
		String sig = desc;
		for(String s : generics) {
			sig = sig.replace(s, "T" + String.valueOf(startChar) + ";");
			startChar += 1;
		}
		return sig;
	}
	
	private void setGenerics(ClassNode node) {
		List<String> generics = globalGuessedGenerics.get(node);
		if(generics == null)
			return;
		if(getParametrizedParent(node) != null && getParametrizedChildren(node) != null) {
			node.signature = getGenerics(generics, true) + "L" + node.superName + getGenerics(generics.size()) + ";";
			for(MethodNode method : node.methods) {
				method.signature = getMethodSig(generics, method.desc);
			}
		}
		else if(getParametrizedParent(node) != null) {
			node.signature = "L" + node.superName + getGenerics(generics, false) + ";";
		}
		else if(getParametrizedChildren(node) != null) {
			node.signature = getGenerics(generics, true) + "L" + node.superName + ";";
			for(MethodNode method : node.methods) {
				method.signature = getMethodSig(generics, method.desc);
			}
		}
	}
	
	public static String getGenerics(int size) {
		char startChar = 'T';
		String genericsSelf = "<";
		for(int i = 0; i < size; i++) {
			genericsSelf += 'T' + String.valueOf(startChar) + ";";
			startChar += 1;
		}
		genericsSelf += ">";
		return genericsSelf;
	}
	
	public static String getGenerics(List<String> generics, boolean parametrized) {
		char startChar = 'T';
		String genericsSelf = "<";
		for(String s : generics) {
			if(parametrized) {
				genericsSelf += String.valueOf(startChar) + ":" + s;
				startChar += 1;
			}
			else {
				genericsSelf += s;
			}
		}
		genericsSelf += ">";
		return genericsSelf;
	}

	private List<MethodNode> visitedNodes = new ArrayList<>();
	
	private void fixOtherBridges(ClassNode node) {
		// Unsure how to handle class nodes with interfaces
        if (!node.interfaces.isEmpty()) {
        	return;
        }
		ClassNode superClass = injector.getClass(node.superName);
		List<String> guessedGenerics = new ArrayList<>();
		List<String> guessedGenericsSuper = new ArrayList<>();
		Set<BridgePair> bridges = new HashSet<>();
		nextMethod:
        for (MethodNode method : node.methods) {
			if (visitedNodes.contains(method)) {
	            continue;
	        }
			if ((method.access & Opcodes.ACC_SYNTHETIC) == 0) {
	            continue;
	        }
			if ((method.access & Opcodes.ACC_BRIDGE) == 0) {
	            continue;
	        }
            AbstractInsnNode insn = method.instructions.getFirst();
            while (insn instanceof LabelNode || insn instanceof LineNumberNode) {
                insn = insn.getNext();
            }
            if (insn.getOpcode() != Opcodes.ALOAD) {
            	continue;
            }
    		VarInsnNode aloadThis = (VarInsnNode) insn;
            if (aloadThis.var != 0) {
            	continue;
            }
            insn = insn.getNext();
            for(Type type : Type.getArgumentTypes(method.desc)) {
                if(type.getOpcode(Opcodes.ILOAD) != insn.getOpcode()) {
                	continue nextMethod;
                }
                insn = insn.getNext();
                if(insn.getOpcode() == Opcodes.CHECKCAST) {
                	TypeInsnNode typeinsn = (TypeInsnNode)insn;
                	String desc = "L" + typeinsn.desc + ";";
                	if(!guessedGenerics.contains(desc)) {
                		guessedGenerics.add(desc);
                	}
                	//Guess generics for superclass (it'll be corrected once it visits it's bridge methods)
                	if(superClass != null && globalGuessedGenerics.get(superClass) == null) {
	                	for(MethodNode m : superClass.methods) {
	                		if (m.name.equals(method.name) && m.desc.equals(method.desc) && (m.access & Opcodes.ACC_BRIDGE) == 0) {
	                        	if(!guessedGenericsSuper.contains(type.getDescriptor())) {
	                        		guessedGenericsSuper.add(type.getDescriptor());
	                        	}
	                		}
	                	}
                	}
                    insn = insn.getNext();
                }
            }
        	if(insn.getOpcode() != Opcodes.INVOKEVIRTUAL) {
        		continue;
        	}
        	MethodInsnNode invokevirtual = (MethodInsnNode) insn;
            insn = insn.getNext();
            Type retType = Type.getReturnType(method.desc);
            if (retType.getOpcode(Opcodes.IRETURN) != insn.getOpcode()) {
        		continue;
            }
            visitedNodes.add(method);
            //Collect bridges
            MethodNode renamedMethod = null;
            for (MethodNode m : node.methods) {
                if (m.name.equals(invokevirtual.name) && m.desc.equals(invokevirtual.desc)) {
                	renamedMethod = m;
                    break;
                }
            }
            if(renamedMethod != null) {
            	bridges.add(new BridgePair(method, renamedMethod));
            }
        }
		if(!bridges.isEmpty()) {
            globalGuessedGenerics.put(node, guessedGenerics);
            if(superClass != null && !guessedGenericsSuper.isEmpty()) {
            	globalGuessedGenerics.put(superClass, guessedGenericsSuper);
			}
			ClassNode supercl = injector.getClass(node.superName);
			if(supercl != null) {
				List<ClassNode> nodes = genericsTree.get(supercl);
		        if(nodes != null) {
		        	nodes.add(node);
		        }
		        else {
					nodes = new ArrayList<>();
		        	nodes.add(node);
		        	genericsTree.put(supercl, nodes);
		        }
			}
			this.bridges.put(node, bridges);
		}
	}
	
	public class BridgePair {
		public MethodNode renamedMethod;
		public MethodNode removedMethod;
		public BridgePair(MethodNode node1, MethodNode node2) {
			removedMethod = node1;
			renamedMethod = node2;
		}
	}
	
	/**
	 * Remove bridges and add generics for comparator class
	 * @param node
	 * @return true if processed node a comparator 
	 */
	private boolean fixComparator(ClassNode node) {

        if (node.interfaces.size() != 1) {
            return false;
        }
        if (!node.interfaces.get(0).equals("java/util/Comparator")) {
        	return false;
        }
        // Ljava/lang/Object;Ljava/util/Comparator<Lorg/junit/runner/Description;>;
        for (MethodNode method : node.methods) {
        	try {
				if ((method.access & Opcodes.ACC_SYNTHETIC) == 0) {
		            continue;
		        }
				if ((method.access & Opcodes.ACC_BRIDGE) == 0) {
		            continue;
		        }
		        if (method.name.equals("compare") && method.desc.equals("(Ljava/lang/Object;Ljava/lang/Object;)I")) {
//		        	if(node.signature != null && !node.signature.equals("Ljava/lang/Object;Ljava/util/Comparator<Ljava/lang/Object;>")) {
//		        		node.methods.remove(method);
//		        		return true;
//		        	}
		            AbstractInsnNode insn = method.instructions.getFirst();
		            while (insn instanceof LabelNode || insn instanceof LineNumberNode) {
		                insn = insn.getNext();
		            }
		            // List of expected instructions for compare's bridge
		            int[] opcodes = {
		            		Opcodes.ALOAD,
		            		Opcodes.ALOAD,
		            		Opcodes.CHECKCAST,
		            		Opcodes.ALOAD,
		            		Opcodes.CHECKCAST,
		            		Opcodes.INVOKEVIRTUAL,
		            		Opcodes.IRETURN
		            		};
		            MethodInsnNode invokevirtual = null;
		            for(int i = 0; i < opcodes.length; i++) {
		            	if(i != 0) {
				            insn = insn.getNext();
		            	}
			            if (insn.getOpcode() != opcodes[i]) {
			                throw new IllegalStateException("invalid bridge method: unexpected opcode");
			            }
		            	if(i == 0) {
		            		VarInsnNode aloadThis = (VarInsnNode) insn;
				            if (aloadThis.var != 0) {
				                throw new IllegalStateException("invalid bridge method: unexpected variable loaded");
				            }
		            	}
		            	if(insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
				            invokevirtual = (MethodInsnNode) insn;
		            	}
		            }
		            for (MethodNode m : node.methods) {
		                if (m.name.equals(invokevirtual.name) && m.desc.equals(invokevirtual.desc)) {
		                	//Rename called method
		                    m.name = invokevirtual.name = method.name;
		                    break;
		                }
		            }
		            //Add generics
		            String generics = invokevirtual.desc.substring(1, invokevirtual.desc.indexOf(';'));
		            node.signature = "Ljava/lang/Object;Ljava/util/Comparator<" + generics + ";>;";
		            //Remove bridge
                    node.methods.remove(method);
		            return true;
		        }
        	} catch (IllegalStateException e) {
        		// Not a bridge
	            method.access &= ~Opcodes.ACC_BRIDGE;
        	}
	    }
        return false;
	}

}
