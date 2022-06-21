package org.mcphackers.rdi.injector.transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mcphackers.rdi.injector.data.ClassStorage;
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

/**
 * This class is a nightmare and really scares me, you too probably
 *
 */
public class FixBridges implements Injection {
	
	private ClassStorage storage;

	private Map<ClassNode, List<ClassNode>> genericsTree = new HashMap<>();
	private Map<ClassNode, List<String>> globalGuessedGenerics = new HashMap<>();
	private Map<ClassNode, Set<BridgePair>> bridges = new HashMap<>();

	private List<ClassNode> getParametrizedChildren(ClassNode node) {
		return genericsTree.get(node);
	}

	private ClassNode getParametrizedParent(ClassNode node) {
		for (Entry<ClassNode, List<ClassNode>> classNodes : genericsTree.entrySet()) {
			if(classNodes.getValue() != null && classNodes.getValue().contains(node)) {
				return classNodes.getKey();
			}
		}
		return null;
		
	}

	public FixBridges(ClassStorage storage) {
		this.storage = storage;
	}
	
	private void removeAndCollectRenamedNodes(ClassNode node, Map<String, String> collectedNames) {
		Set<BridgePair> forRemoval = bridges.get(node);
		Map<String, String> methodRenames = new HashMap<>();
		if(forRemoval != null) {
			for(BridgePair pair : forRemoval) {
				// Removing bridges
				if(!collectedNames.containsKey(pair.renamedMethod.name)) {
					collectedNames.put(pair.renamedMethod.name, pair.removedMethod.name);
				}
				String rename = pair.removedMethod.name;
				while (collectedNames.containsKey(rename) && collectedNames.get(rename) != null && !collectedNames.get(rename).equals(rename)) {
					rename = collectedNames.get(rename);
				}
				methodRenames.put(pair.renamedMethod.name, rename);
				pair.renamedMethod.name = rename;
				node.methods.remove(pair.removedMethod);
			}
		}
		renamingMap.put(node.name, methodRenames);
	}
	
	private void deleteBridges(ClassNode currentNode, Map<String, String> collectedNames) {
		setGenerics(currentNode);
		removeAndCollectRenamedNodes(currentNode, collectedNames);
		if(getParametrizedChildren(currentNode) == null) {
			return;
		}
		for(ClassNode node : getParametrizedChildren(currentNode)) {
			deleteBridges(node, collectedNames);
		}
	}
	
	private Map<String, Map<String, String>> renamingMap = new HashMap<>();

	@Override
	public void transform() {
		for(ClassNode node : storage.getClasses()) {
			if(!fixComparator(node)) {
				fixOtherBridges(node);
			}
		}
		a:
		for (Entry<ClassNode, List<ClassNode>> entry : genericsTree.entrySet()) {
			ClassNode startNode = entry.getKey();
			if(getParametrizedParent(startNode) != null || !startNode.interfaces.isEmpty()) {
				continue;
			}
			if(getParametrizedChildren(startNode) != null) {
				for(ClassNode cn : getParametrizedChildren(startNode)) {
					//FIXME temp fix for decompiling mc beta
					if(!cn.interfaces.isEmpty()) continue a;
				}
			}
			Map<String, String> renamingMap = new HashMap<>();
			for(MethodNode method : startNode.methods) {
				// I know this look dumb, but it's there to prevent other bridge methods occupying renaming keys of their parent
				renamingMap.put(method.name, null);
			}
			deleteBridges(startNode, renamingMap);
		}
		for(ClassNode node : storage.getClasses()) {
			for(MethodNode method : node.methods) {
				// Update method references
				for(AbstractInsnNode insn = method.instructions.getFirst(); insn != null ; insn = insn.getNext()) {
					if(!(insn instanceof MethodInsnNode)) {
						continue;
					}
					MethodInsnNode invoke = (MethodInsnNode) insn;
					Map<String, String> renameMap = renamingMap.get(invoke.owner);
					String rename = null;
					if(renameMap != null) {
						rename = renameMap.get(invoke.name);
					}
					if(rename != null) {
						invoke.name = rename;
					}
				}
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
		String interfaces = "";
		for(String itf : node.interfaces) {
			interfaces += "L" + itf + ";";
		}
		if(getParametrizedParent(node) != null && getParametrizedChildren(node) != null) {
			node.signature = getGenerics(generics, true) + "L" + node.superName + getGenerics(generics.size()) + ";" + interfaces;
			for(MethodNode method : node.methods) {
				method.signature = getMethodSig(generics, method.desc);
			}
		}
		else if(getParametrizedParent(node) != null) {
			node.signature = "L" + node.superName + getGenerics(generics, false) + ";" + interfaces;
		}
		else if(getParametrizedChildren(node) != null) {
			node.signature = getGenerics(generics, true) + "L" + node.superName + ";" + interfaces;
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
		ClassNode superClass = storage.getClass(node.superName);
		if (superClass != null && !node.interfaces.isEmpty()) {
			superClass = storage.getClass(node.interfaces.get(0));
		}
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
					//TODO save method signature
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
			// Account for different return types
			Type typeRet = Type.getReturnType(method.desc);
			Type typeRet2 = Type.getReturnType(invokevirtual.desc);
			if(typeRet.getSort() == Type.OBJECT && typeRet2.getSort() == Type.OBJECT && !typeRet.equals(typeRet2) &&
					!typeRet2.getInternalName().equals(node.name)) {
				String desc = typeRet2.getDescriptor();
				if(!guessedGenerics.contains(desc)) {
					guessedGenerics.add(desc);
				}
			}
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
				bridges.add(new BridgePair(node, method, renamedMethod));
			}
		}
		if(!bridges.isEmpty()) {
			globalGuessedGenerics.put(node, guessedGenerics);
			if(superClass != null && !guessedGenericsSuper.isEmpty()) {
				globalGuessedGenerics.put(superClass, guessedGenericsSuper);
			}
			ClassNode supercl = storage.getClass(node.superName);
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
			else {
				genericsTree.put(node, null);
			}
			this.bridges.put(node, bridges);
		}
	}
	
	public class BridgePair {
		public final ClassNode ownerClass;
		public final MethodNode renamedMethod;
		public final MethodNode removedMethod;
		public BridgePair(ClassNode owner, MethodNode node1, MethodNode node2) {
			removedMethod = node1;
			renamedMethod = node2;
			ownerClass = owner;
		}
	}
	
	/**
	 * Remove bridges and add generics for comparator class
	 * @param node
	 * @return true if processed node is a comparator 
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
//					if(node.signature != null && !node.signature.equals("Ljava/lang/Object;Ljava/util/Comparator<Ljava/lang/Object;>")) {
//						node.methods.remove(method);
//						return true;
//					}
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
							renamingMap.put(node.name, Collections.singletonMap(m.name, method.name));
							m.name = method.name;
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
