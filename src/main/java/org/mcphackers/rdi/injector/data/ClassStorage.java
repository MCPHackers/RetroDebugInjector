package org.mcphackers.rdi.injector.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

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
	
}
