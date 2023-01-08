package org.mcphackers.rdi.injector.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mcphackers.rdi.injector.data.ClassStorage;
import org.mcphackers.rdi.injector.remapper.MethodRenameMap;
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

public class GuessGenerics implements Injection {
	
	private ClassStorage storage;
	private ClassTree classTree;
	private MethodRenameMap renameMap = new MethodRenameMap();

	public GuessGenerics(ClassStorage storage) {
		this.storage = storage;
	}

	@Override
	public void transform(ClassStorage classStorage) {
		storage = classStorage;
		findBridges();
		verifyIndexes();
		List<String> interfaces = new ArrayList<>();
		NodeDelegate delegate = (treeNode) -> {
			applySignatures(treeNode, interfaces);
		};
		classTree.walk(delegate);
		renameReferences();
	}

	private void applySignatures(TreeNode treeNode, List<String> interfaces) {
		ClassNode node = treeNode.classNode;
		interfaces.addAll(node.interfaces);
		List<Bridge> bridges = treeNode.bridges;
		if(treeNode.generics == null)
			return;
		List<String> generics = treeNode.generics.get(node.superName);
		String interfacesDesc = getInterfacesSig(node.interfaces, treeNode.generics);
		node.version = Math.max(Opcodes.V1_6, node.version);
		if(treeNode.hasParent()) {
			treeNode.parent.classNode.version = Math.max(Opcodes.V1_6, treeNode.parent.classNode.version);
		}
		if(treeNode.hasParent() && treeNode.hasChildren()) {
			if(bridges != null) {
				node.signature = getGenerics(generics, true) + "L" + node.superName + getGenerics(generics.size()) + ";" + interfacesDesc;
				for(Bridge bridge : bridges) {
					bridge.invokeMethod.signature = getMethodSig(bridge.genericIndexes, bridge.invokeMethod.desc);
					if(bridge.parentInvokeMethod != null) {
						bridge.parentInvokeMethod.signature = getMethodSig(bridge.genericIndexes, bridge.parentInvokeMethod.desc);
					}
				}
			}
		}
		else if(treeNode.hasParent()) {
			node.signature = "L" + node.superName + getGenerics(generics, false) + ";" + interfacesDesc;
			if(bridges != null)
			for(Bridge bridge : bridges) {
				if(bridge.parentInvokeMethod != null) {
					bridge.parentInvokeMethod.signature = getMethodSig(bridge.genericIndexes, bridge.parentInvokeMethod.desc);
				}
			}
		}
		else if(treeNode.hasChildren()) {
			node.signature = getGenerics(generics, true) + "L" + node.superName + ";" + interfacesDesc;
		}
	}
	
	private static String getInterfacesSig(List<String> interfaces, Map<String, List<String>> generics) {
		StringBuilder desc = new StringBuilder();
		for(int i = 0; i < interfaces.size(); i++) {
			String itf = interfaces.get(i);
			desc.append("L").append(itf);
			if(generics != null) {
				List<String> generic = generics.get(itf);
				if(generic != null) {
					desc.append(getGenerics(generic, false));
				}
			}
			desc.append(";");
		}
		return desc.toString();
	}

	private static String getMethodSig(int[] genericIndexes, String desc) {
		char startChar = 'T';
		StringBuilder sig = new StringBuilder("(");
		Type[] types = Type.getArgumentTypes(desc);
		for(int i = 0; i < types.length; i++) {
			int index = genericIndexes[i];
			if(index == 0) {
				sig.append(types[i].getDescriptor());
			} else {
				sig.append("T").append((char)(startChar + index - 1)).append(";");
			}
		}
		sig.append(")");
		Type type = Type.getReturnType(desc);
		int index = genericIndexes[genericIndexes.length - 1];
		if(index == 0) {
			sig.append(type.getDescriptor());
		} else {
			sig.append("T").append((char)(startChar + index - 1)).append(";");
		}
		return sig.toString();
	}
	
	public static String getGenerics(int size) {
		if(size == 0) return "";
		char startChar = 'T';
		StringBuilder genericsSelf = new StringBuilder("<");
		for(int i = 0; i < size; i++) {
			genericsSelf.append('T').append(startChar).append(";");
			startChar += 1;
		}
		genericsSelf.append(">");
		return genericsSelf.toString();
	}
	
	public static String getGenerics(List<String> generics, boolean parameterized) {
		if(generics == null || generics.isEmpty()) return "";
		char startChar = 'T';
		StringBuilder genericsSelf = new StringBuilder("<");
		for(String s : generics) {
			if(parameterized) {
				genericsSelf.append(startChar).append(":").append(s);
				startChar += 1;
			}
			else {
				genericsSelf.append(s);
			}
		}
		genericsSelf.append(">");
		return genericsSelf.toString();
	}
	
	private void findBridges() {
		Map<ClassNode, TreeNode> treeMap = new HashMap<>();
		for(ClassNode node : storage) {
			List<Bridge> bridges = new ArrayList<>();
			Map<ClassNode, Set<String>> allGenerics = new HashMap<>();
			Map<ClassNode, Set<String>> allGenericsParent = new HashMap<>();
			nextMethod:
			for (MethodNode method : node.methods) {
				if ((method.access & Opcodes.ACC_SYNTHETIC) == 0) {
					continue;
				}
				// May not have bridge modifier? WTF
//				if ((method.access & Opcodes.ACC_BRIDGE) == 0) {
//					continue;
//				}
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
				Set<String> generics = new HashSet<>();
				Set<String> genericsParent = new HashSet<>();
				insn = insn.getNext();
				Type[] types = Type.getArgumentTypes(method.desc);
				String[] genericTypes = new String[types.length + 1];
				int i = 0;
				for(Type type : types) {
					if(type.getOpcode(Opcodes.ILOAD) != insn.getOpcode()) {
						continue nextMethod;
					}
					insn = insn.getNext();
					if(insn.getOpcode() == Opcodes.CHECKCAST) {
						TypeInsnNode typeinsn = (TypeInsnNode)insn;
						String desc = "L" + typeinsn.desc + ";";
						genericTypes[i] = desc;
						generics.add(desc);
						genericsParent.add(type.getDescriptor());
						insn = insn.getNext();
					}
					i++;
				}
				if(insn.getOpcode() != Opcodes.INVOKEVIRTUAL) {
					continue;
				}
				MethodNode invokeParentMethod = null;
				MethodInsnNode invokevirtual = (MethodInsnNode) insn;
				ClassNode parentClass = storage.getClass(node.superName);
				if(parentClass != null) {
					for(MethodNode m : parentClass.methods) {
						if (m.name.equals(method.name) && m.desc.equals(method.desc) && (m.access & Opcodes.ACC_SYNTHETIC) == 0) {
							invokeParentMethod = m;
							break;
						}
					}
				}
				if(invokeParentMethod == null) {
					searchItf:
					for(String itf : node.interfaces) {
						ClassNode itfNode = storage.getClass(itf);
						if(itfNode == null) {
							continue;
						}
						for(MethodNode m : itfNode.methods) {
							if (m.name.equals(method.name) && m.desc.equals(method.desc) && (m.access & Opcodes.ACC_SYNTHETIC) == 0) {
								invokeParentMethod = m;
								parentClass = itfNode;
								break searchItf;
							}
						}
					}
				}
				if(invokeParentMethod == null) {
					parentClass = null;
				}
				insn = insn.getNext();
				Type retType = Type.getReturnType(method.desc);
				if (retType.getOpcode(Opcodes.IRETURN) != insn.getOpcode()) {
					continue;
				}
				Type retType2 = Type.getReturnType(invokevirtual.desc);
				if(retType.getSort() == Type.OBJECT && retType2.getSort() == Type.OBJECT && !retType.equals(retType2)) {
					String desc = retType2.getDescriptor();
					genericTypes[genericTypes.length - 1] = desc;
					generics.add(desc);
				}
				insn = insn.getNext();
				MethodNode invokeMethod = null;
				for (MethodNode m : node.methods) {
					if (m.name.equals(invokevirtual.name) && m.desc.equals(invokevirtual.desc) && (m.access & Opcodes.ACC_SYNTHETIC) == 0) {
						invokeMethod = m;
						break;
					}
				}

				if(invokeMethod == null) continue;
				
				Set<String> set = allGenericsParent.get(parentClass);
				if(set == null) {
					allGenericsParent.put(parentClass, genericsParent);
				} else {
					set.addAll(genericsParent);
				}
				set = allGenerics.get(parentClass);
				if(set == null) {
					allGenerics.put(parentClass, generics);
				} else {
					set.addAll(generics);
				}
				bridges.add(new Bridge(method, invokeMethod, parentClass, invokeParentMethod, genericTypes));
			}
			if(!bridges.isEmpty()) {
				TreeNode treeNode = new TreeNode(node);
				treeMap.put(node, treeNode);
				treeNode.bridges = bridges;
				ClassNode parent = storage.getClass(node.superName);
				Set<String> set = allGenerics.get(parent);
				if(parent != null && set != null) {
					List<String> genericsSelf = new ArrayList<>(set);
					treeNode.generics.put(parent.name, genericsSelf);
					for(Bridge bridge : treeNode.bridges) {
						if(bridge.parent == parent) {
							bridge.genericIndexes = bridge.getIndexes(genericsSelf);
						}
					}
					TreeNode treeNodeParent = treeMap.get(parent);
					if(treeNodeParent == null) treeNodeParent = new TreeNode(parent);
					treeNode.parent = treeNodeParent;
					List<String> genericsParent = new ArrayList<>(allGenericsParent.get(parent));
					treeNode.generics.put(parent.superName, genericsParent);
					treeMap.put(parent, treeNodeParent);
				}
				//TODO
				for(String itf : node.interfaces) {
					ClassNode parent2 = storage.getClass(itf);
					Set<String> set2 = allGenerics.get(parent2);
					if(parent2 == null || set2 == null) {
						continue;
					}
					List<String> genericsSelf = new ArrayList<>(set2);
					treeNode.generics.put(parent2.name, genericsSelf);
					for(Bridge bridge : treeNode.bridges) {
						if(bridge.parent == parent2) {
							bridge.genericIndexes = bridge.getIndexes(genericsSelf);
						}
					}
					TreeNode treeNodeParent = treeMap.get(parent2);
					if(treeNodeParent == null) treeNodeParent = new TreeNode(parent2);
					treeNode.interfaces.add(treeNodeParent);
					List<String> genericsParent = new ArrayList<>(allGenericsParent.get(parent2));
					treeNode.generics.put(parent2.superName, genericsParent);
					treeMap.put(parent2, treeNodeParent);
				}
			}
		}
		List<TreeNode> treeNodes = new ArrayList<>();
		for(Entry<ClassNode, TreeNode> entry : treeMap.entrySet()) {
			TreeNode treeNode = entry.getValue();
			treeNodes.add(treeNode);
		}
		for(ClassNode node : storage) {
			TreeNode treeNode = treeMap.get(node);
			ClassNode classNode = storage.getClass(node.superName);
			TreeNode parent = treeMap.get(classNode);
			if(parent != null) {
				if(treeNode != null) {
					parent.children.add(treeNode);
				} else {
					TreeNode newNode = new TreeNode(node);
					newNode.parent = parent;
					newNode.generics = new HashMap<>();
					List<String> val = parent.generics.get(parent.classNode.superName);
					if(val != null) {
						newNode.generics.put(node.superName, val);
					}
					parent.children.add(newNode);
					treeNodes.add(newNode);
				}
			}
		}
		classTree = new ClassTree(treeNodes);
	}
	
	public void renameReferences() {
		NodeDelegate delegate = (treeNode) -> {
			List<Bridge> bridges = treeNode.bridges;
			if(bridges == null) {
				return;
			}
			for(Bridge pair : bridges) {
				if(pair.parentInvokeMethod == null) {
					String rename = pair.bridgeMethod.name;
					renameMap.put(treeNode.classNode.name, pair.invokeMethod.desc, pair.invokeMethod.name, rename);
					pair.invokeMethod.name = rename;
					continue;
				}
				String rename = pair.parentInvokeMethod.name;
				renameMap.put(treeNode.classNode.name, pair.bridgeMethod.desc, pair.bridgeMethod.name, rename);
				pair.bridgeMethod.name = rename;
				renameMap.put(treeNode.classNode.name, pair.invokeMethod.desc, pair.invokeMethod.name, rename);
				pair.invokeMethod.name = rename;
			}
		};
		classTree.walk(delegate);
		for(ClassNode node : storage) {
			for(MethodNode method : node.methods) {
				for(AbstractInsnNode insn : method.instructions) {
					if(!(insn instanceof MethodInsnNode)) {
						continue;
					}
					MethodInsnNode invoke = (MethodInsnNode) insn;
					String rename = renameMap.get(invoke.owner, invoke.desc, invoke.name);
					if(rename != null) {
						invoke.name = rename;
					}
				}
			}
		}
	}
	
	private void verifyIndexes() {
		NodeDelegate delegate1 = (treeNode) -> {
			if(treeNode.children == null) {
				return;
			}
			Map<MethodNode, int[]> methodIndexes = new HashMap<>();
			for(TreeNode child : treeNode.children) {
				if(child.bridges == null) continue;
				for(Bridge bridge : child.bridges) {
					if(bridge.parentInvokeMethod == null) {
						continue;
					}
					int[] indexes = methodIndexes.get(bridge.parentInvokeMethod);
					if(indexes == null) {
						methodIndexes.put(bridge.parentInvokeMethod, bridge.genericIndexes);
					} else {
						for(int i = 0; i < indexes.length; i++) {
				            if (indexes[i] < bridge.genericIndexes[i]) {
				            	methodIndexes.put(bridge.parentInvokeMethod, bridge.genericIndexes);
				            }
						}
					}
				}
			}
			boolean verifyGenerics = true;
			for(TreeNode child : treeNode.children) {
				List<TreeNode> treeNodes = new ArrayList<>();
				treeNodes.add(treeNode);
				treeNodes.addAll(child.interfaces);
				for(TreeNode parent : treeNodes) {
					if(parent == null || child.bridges == null) continue;
					String[] childGenerics = new String[1];
					String[] generics = new String[1];
					for(Bridge bridge : child.bridges) {
						if(bridge.parent != parent.classNode) continue;
						int[] indexes = methodIndexes.get(bridge.parentInvokeMethod);
						if(indexes == null) continue;
						bridge.genericIndexes = indexes;
						for(int i = 0; i < indexes.length; i++) {
				            if (indexes[i] > 0) {
				            	childGenerics = childGenerics.length < indexes[i] ? Arrays.copyOf(childGenerics, indexes[i]) : childGenerics;
				            	childGenerics[indexes[i] - 1] = bridge.generics[i];
				            	if(verifyGenerics) {
				            		generics = generics.length < indexes[i] ? Arrays.copyOf(generics, indexes[i]) : generics;
					            	if(i == indexes.length - 1) {
						            	Type type = Type.getReturnType(bridge.parentInvokeMethod.desc);
						            	generics[indexes[i] - 1] = type.getDescriptor();
					            	} else {
						            	Type[] types = Type.getArgumentTypes(bridge.parentInvokeMethod.desc);
						            	generics[indexes[i] - 1] = types[i].getDescriptor();
					            	}
				            	}
				            }
						}
					}
					child.generics.put(parent.classNode.name, Arrays.asList(childGenerics));
					if(verifyGenerics) {
						parent.generics.put(parent.classNode.superName, Arrays.asList(generics));
						verifyGenerics = false;
					}
				}
			}
		};
		classTree.walk(delegate1);
	}
	
	private interface NodeDelegate {
		void call(TreeNode node);
	}

	public class TreeNode {
		private final ClassNode classNode;
		
		TreeNode parent;
		List<TreeNode> interfaces = new ArrayList<>();
		List<TreeNode> children = new ArrayList<>();
		
		List<Bridge> bridges;
		Map<String, List<String>> generics = new HashMap<>();
		
		public TreeNode(ClassNode node) {
			classNode = node;
		}
		
		public boolean hasParent() {
			return parent != null;
		}
		
		public boolean hasChildren() {
			return children != null && !children.isEmpty();
		}
	}
	
	public class ClassTree {
		
		List<TreeNode> root = new ArrayList<>();
				
		public ClassTree(List<TreeNode> treeNodes) {
			for(TreeNode node : treeNodes) {
				if(!node.hasParent()) {
					root.add(node);
				}
			}
		}

		public void walk(NodeDelegate delegate) {
			for(TreeNode node : root) {
				walk(node, delegate);
			}
		}
		
		public void walk(TreeNode node, NodeDelegate delegate) {
			delegate.call(node);
			if(node.children != null) {
				for(TreeNode node2 : node.children) {
					walk(node2, delegate);
				}
			}
		}
	}
	
	public class Bridge {
		public final MethodNode bridgeMethod;
		public final MethodNode invokeMethod;
		public final MethodNode parentInvokeMethod;
		public final ClassNode parent;
		public final String[] generics;
		public int[] genericIndexes;
		public Bridge(MethodNode bridge, MethodNode invoke, ClassNode parent, MethodNode parentInvoke, String[] generics) {
			bridgeMethod = bridge;
			invokeMethod = invoke;
			parentInvokeMethod = parentInvoke;
			this.generics = generics;
			this.parent = parent;
		}
		
		/**
		 * genericsList == List {Entity, Model}
		 * String[] generics == String[] {Entity, Model, null}
		 * int[] indexes == int[] {1, 2, 0}
		 * @param genericsList
		 */
		private int[] getIndexes(List<String> genericsList) {
			int[] indexes = new int[generics.length];
			for(int i = 0; i < generics.length; i++) {
				String val = generics[i];
				if(val != null) {
					indexes[i] = genericsList.indexOf(val) + 1;
				}
			}
			return indexes;
		}
	}

}
