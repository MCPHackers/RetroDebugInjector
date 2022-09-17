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
import org.mcphackers.rdi.injector.transform.GuessGenericsFromBridges.ClassTree.TreeNode;
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

public class GuessGenericsFromBridges implements Injection {
	
	private final ClassStorage storage;

	private ClassTree classTree = new ClassTree();
	private MethodRenameMap renameMap = new MethodRenameMap();

	public GuessGenericsFromBridges(ClassStorage storage) {
		this.storage = storage;
	}

	@Override
	public void transform() {
		for(ClassNode node : storage.getClasses()) {
			findBridges(node);
		}
		classTree.build();
		NodeDelegate delegate1 = (treeNode) -> {
			if(treeNode.children == null) {
				return;
			}
			Map<MethodNode, int[]> methodIndexes = new HashMap<>();
			for(TreeNode child : treeNode.children) {
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
				String[] childGenerics = new String[1];
				String[] generics = new String[1];
				for(Bridge bridge : child.bridges) {
					int[] indexes = methodIndexes.get(bridge.parentInvokeMethod);
					if(indexes == null) continue;
					bridge.genericIndexes = indexes;
					for(int i = 0; i < indexes.length; i++) {
			            if (indexes[i] > 0) {
			            	childGenerics = childGenerics.length < indexes[i] ? childGenerics : Arrays.copyOf(childGenerics, indexes[i]);
			            	childGenerics[indexes[i] - 1] = bridge.generics[i];
			            	if(verifyGenerics) {
			            		generics = generics.length < indexes[i] ? generics : Arrays.copyOf(generics, indexes[i]);
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
				child.generics = Arrays.asList(childGenerics);
				if(verifyGenerics) {
					treeNode.generics = Arrays.asList(generics);
					verifyGenerics = false;
				}
			}
		};
		classTree.walk(delegate1);
		NodeDelegate delegate = (treeNode) -> {
			applySignatures(treeNode);
		};
		classTree.walk(delegate);
//		renameReferences();
	}
	
	private void applySignatures(TreeNode treeNode) {
		ClassNode node = treeNode.classNode;
		List<Bridge> bridges = treeNode.bridges;
		List<String> generics = treeNode.generics;
		if(generics == null /*|| generics.size() > 1 FIXME generics with more than one type are hard to guess properly*/)
			return;
		String interfaces = "";
		for(String itf : node.interfaces) {
			interfaces += "L" + itf + ";";
		}
		node.version = Math.max(50 /*JAVA 6*/, node.version);
		if(treeNode.parent != null) {
			treeNode.parent.classNode.version = Math.max(50, treeNode.parent.classNode.version);
		}
		if(treeNode.parent != null && treeNode.children != null) {
			if(bridges != null) {
				node.signature = getGenerics(generics, true) + "L" + node.superName + getGenerics(generics.size()) + ";" + interfaces;
				for(Bridge bridge : bridges) {
					bridge.invokeMethod.signature = getMethodSig(bridge.genericIndexes, bridge.invokeMethod.desc);
					if(bridge.parentInvokeMethod != null) {
						bridge.parentInvokeMethod.signature = getMethodSig(bridge.genericIndexes, bridge.parentInvokeMethod.desc);
					}
				}
			}
		}
		else if(treeNode.children == null) {
			node.signature = "L" + node.superName + getGenerics(generics, false) + ";" + interfaces;
			if(bridges != null)
			for(Bridge bridge : bridges) {
				if(bridge.parentInvokeMethod != null) {
					bridge.parentInvokeMethod.signature = getMethodSig(bridge.genericIndexes, bridge.parentInvokeMethod.desc);
				}
			}
		}
		else if(treeNode.parent == null) {
			node.signature = getGenerics(generics, true) + "L" + node.superName + ";" + interfaces;
		}
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
		char startChar = 'T';
		StringBuilder genericsSelf = new StringBuilder("<");
		for(int i = 0; i < size; i++) {
			genericsSelf.append('T').append(startChar).append(";");
			startChar += 1;
		}
		genericsSelf.append(">");
		return genericsSelf.toString();
	}
	
	public static String getGenerics(List<String> generics, boolean parametrized) {
		char startChar = 'T';
		StringBuilder genericsSelf = new StringBuilder("<");
		for(String s : generics) {
			if(parametrized) {
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
	
	private void findBridges(ClassNode node) {
		ClassNode superClass = storage.getClass(node.superName);
		List<Bridge> bridges = new ArrayList<>();
		Set<String> generics = new HashSet<>();
		Set<String> genericsSuper = new HashSet<>();
		nextMethod:
		for (MethodNode method : node.methods) {
			if ((method.access & Opcodes.ACC_SYNTHETIC) == 0) {
				continue;
			}
			// May not have bridge modifier? WTF
//			if ((method.access & Opcodes.ACC_BRIDGE) == 0) {
//				continue;
//			}
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
					genericsSuper.add(type.getDescriptor());
					insn = insn.getNext();
				}
				i++;
			}
			if(insn.getOpcode() != Opcodes.INVOKEVIRTUAL) {
				continue;
			}
			MethodNode invokeParentMethod = null;
			MethodInsnNode invokevirtual = (MethodInsnNode) insn;
			if(superClass != null) {
				for(MethodNode m : superClass.methods) {
					if (m.name.equals(method.name) && m.desc.equals(method.desc)) {
						invokeParentMethod = m;
					}
				}
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
				if (m.name.equals(invokevirtual.name) && m.desc.equals(invokevirtual.desc)) {
					invokeMethod = m;
					break;
				}
			}
			if(invokeMethod == null) continue;
			bridges.add(new Bridge(method, invokeMethod, invokeParentMethod, genericTypes));
		}
		if(!bridges.isEmpty()) {
			for(Bridge bridge : bridges) {
				bridge.genericIndexes = bridge.getIndexes(new ArrayList<>(generics));
			}
			// Building class tree
			if(superClass != null) {
				List<ClassNode> nodes = classTree.processedTree.get(superClass);
				if(nodes != null) {
					nodes.add(node);
				}
				else {
					nodes = new ArrayList<>();
					nodes.add(node);
					classTree.processedTree.put(superClass, nodes);
				}
			}
			else {
				classTree.processedTree.put(node, null);
			}
			classTree.bridges.put(node, bridges);
			classTree.generics.put(node, new ArrayList<>(generics));
			if(superClass != null && !genericsSuper.isEmpty()) {
				classTree.generics.put(superClass, new ArrayList<>(genericsSuper));
			}
		}
	}
	
	public void renameReferences() {
//		List<Bridge> bridges = node.bridges;
//		if(bridges == null) {
//			return;
//		}
//		for(Bridge pair : bridges) {
//			String rename = pair.invokeMethod.name;
//			renameMap.put(node.classNode.name, pair.bridgeMethod.desc, pair.bridgeMethod.name, rename);
//			pair.bridgeMethod.name = rename;
//		}
		for(ClassNode node : storage.getClasses()) {
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
	
	private interface NodeDelegate {
		void call(TreeNode node);
	}
	
	public class ClassTree {
		
		List<TreeNode> root = new ArrayList<>();
		Map<ClassNode, TreeNode> cache = new HashMap<>();
		
		Map<ClassNode, List<ClassNode>> processedTree = new HashMap<>();
		Map<ClassNode, List<Bridge>> bridges = new HashMap<>();
		Map<ClassNode, List<String>> generics = new HashMap<>();
		
		public class TreeNode {
			private final ClassNode classNode;
			
			TreeNode parent;
			List<TreeNode> children;
			
			List<Bridge> bridges;
			List<String> generics;
			
			public TreeNode(ClassNode node) {
				classNode = node;
				cache.put(node, this);
			}
		}

		public void build() {
			for (Entry<ClassNode, List<ClassNode>> classNodes : processedTree.entrySet()) {
				TreeNode treeNode = new TreeNode(classNodes.getKey());
				treeNode.generics = generics.get(treeNode.classNode);
				treeNode.bridges = bridges.get(treeNode.classNode);
			}
			for (Entry<ClassNode, List<ClassNode>> classNodes : processedTree.entrySet()) {
				ClassNode parent = null;
				for (Entry<ClassNode, List<ClassNode>> classNodes2 : processedTree.entrySet()) {
					if(classNodes2.getValue() != null && classNodes2.getValue().contains(classNodes.getKey())) {
						parent = classNodes2.getKey();
					}
				}
				TreeNode node = cache.get(classNodes.getKey());
				TreeNode parentNode = cache.get(parent);
				node.parent = parentNode;
				List<TreeNode> children = null;
				if(processedTree.get(node.classNode) != null) {
					children = new ArrayList<>();
					for(ClassNode classNode : processedTree.get(node.classNode)) {
						TreeNode cachedNode = cache.get(classNode);
						if(cachedNode == null) {
							cachedNode = new TreeNode(classNode);
							cachedNode.parent = node;
							cachedNode.generics = generics.get(classNode);
							cachedNode.bridges = bridges.get(classNode);
							children.add(cachedNode);
						}
						else {
							children.add(cachedNode);
						}
					}
				}
				node.children = children;
				if(parentNode == null) {
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
		
		public ClassNode getParent(ClassNode node) {
			TreeNode treeNode = cache.get(node);
			if(treeNode != null && treeNode.parent != null) {
				return treeNode.parent.classNode;
			}
			return null;
		}
		
		public List<ClassNode> getChildren(ClassNode node) {
			TreeNode treeNode = cache.get(node);
			if(treeNode != null && treeNode.children != null) {
				List<ClassNode> nodes = new ArrayList<>();
				for(TreeNode treeNode2 : treeNode.children) {
					nodes.add(treeNode2.classNode);
				}
				return nodes;
			}
			return null;
		}
	}
	
	public class Bridge {
		public final MethodNode bridgeMethod;
		public final MethodNode invokeMethod;
		public final MethodNode parentInvokeMethod;
		public final String[] generics;
		public int[] genericIndexes;
		public Bridge(MethodNode bridge, MethodNode invoke, MethodNode parentInvoke, String[] generics) {
			bridgeMethod = bridge;
			invokeMethod = invoke;
			parentInvokeMethod = parentInvoke;
			this.generics = generics;
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
