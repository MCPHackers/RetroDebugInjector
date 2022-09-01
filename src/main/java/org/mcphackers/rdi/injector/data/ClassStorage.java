package org.mcphackers.rdi.injector.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mcphackers.rdi.util.FieldReference;
import org.mcphackers.rdi.util.MethodReference;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class ClassStorage {
	
	private Map<String, ClassNode> cachedNodes = new HashMap<>();
	private List<ClassNode> nodes = new ArrayList<>();
	
	public ClassStorage(List<ClassNode> classNodes) {
		nodes.addAll(classNodes);
		updateCache();
	}
	
	public List<ClassNode> getClasses() {
		return nodes;
	}
	
	public ClassNode getClass(String name) {
		return cachedNodes.get(name);
	}
	
	public FieldNode getField(FieldReference ref) {
		ClassNode node = getClass(ref.getOwner());
		if(node != null) {
			for(FieldNode field : node.fields) {
				if(field.name.equals(ref.getName()) && field.desc.equals(ref.getDesc())) {
					return field;
				}
			}
		}
		return null;
	}
	
	public MethodNode getMethod(MethodReference ref) {
		ClassNode node = getClass(ref.getOwner());
		if(node != null) {
			for(MethodNode method : node.methods) {
				if(method.name.equals(ref.getName()) && method.desc.equals(ref.getDesc())) {
					return method;
				}
			}
		}
		return null;
	}
	
	public void addClass(ClassNode node) {
		nodes.add(node);
		cachedNodes.put(node.name, node);
	}
	
	public void removeClass(ClassNode node) {
		nodes.remove(node);
		cachedNodes.remove(node.name);
	}
	
	public void removeClass(String name) {
		ClassNode nodeToRemove = cachedNodes.get(name);
		nodes.remove(nodeToRemove);
		cachedNodes.remove(nodeToRemove.name);
	}
	
	public void updateCache() {
		cachedNodes.clear();
		for(ClassNode node : nodes) {
			cachedNodes.put(node.name, node);
		}
	}
	
	public List<String> getAllClasses() {
		return nodes.stream().map(node -> node.name).collect(Collectors.toList());
	}
	
	public static boolean inOnePackage(ClassNode node, ClassNode node2) {
		return inOnePackage(node.name, node2.name);
	}
	
	public static boolean inOnePackage(String node, String node2) {
		int lastIndexOfSlash = node.lastIndexOf('/');
		int lastIndexOfSlash2 = node2.lastIndexOf('/');
		if(lastIndexOfSlash != lastIndexOfSlash2) {
			return false;
		}
		if(lastIndexOfSlash == -1 && lastIndexOfSlash2 == -1) {
			return true;
		}
		String packageName = node.substring(0, lastIndexOfSlash);
		String packageName2 = node2.substring(0, lastIndexOfSlash2);
		if (packageName.equals(packageName2)) {
			return true;
		}
		return false;
	}
	
}
