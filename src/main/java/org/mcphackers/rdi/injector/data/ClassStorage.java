package org.mcphackers.rdi.injector.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mcphackers.rdi.util.FieldReference;
import org.mcphackers.rdi.util.MethodReference;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class ClassStorage implements Iterable<ClassNode> {
	
	private final Map<String, ClassNode> cachedNodes = new HashMap<String, ClassNode>();
	private final List<ClassNode> nodes = new ArrayList<ClassNode>();
	
	public ClassStorage(List<ClassNode> classNodes) {
		nodes.addAll(classNodes);
		updateCache();
	}

	public Iterator<ClassNode> iterator() {
		return nodes.iterator();
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
		removeClass(cachedNodes.get(name));
	}
	
	public void updateCache() {
		cachedNodes.clear();
		for(ClassNode node : nodes) {
			cachedNodes.put(node.name, node);
		}
	}
	
	public List<String> getAllClasses() {
		List<String> allNames = new ArrayList<String>();
		for(ClassNode node : nodes) {
			allNames.add(node.name);
		}
		return allNames;
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
