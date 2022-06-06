package org.mcphackers.rdi.injector.transform;

import java.util.HashMap;
import java.util.Map;

import org.mcphackers.rdi.injector.Constants;
import org.mcphackers.rdi.injector.Generics;
import org.mcphackers.rdi.injector.Injector;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class AddGenerics implements Injection {
	private final Map<String, Boolean> cachedLists = new HashMap<>();
	
	private final Generics context;
	private final Injector injector;

	public AddGenerics(Injector injector, Generics context) {
		this.context = context;
		this.injector = injector;
	}
	
	public void transform() {
		for (ClassNode node : injector.getClasses()) {
			for(MethodNode method : node.methods) {
				String sig = context.getMethodSignature(node.name, method.name, method.desc);
				if(sig != null) {
					method.signature = sig;
				}
			}
			for(FieldNode field : node.fields) {
				String sig = context.getFieldSignature(node.name, field.name);
				if(sig != null) {
					field.signature = sig;
					if(isList(field.desc)) {
						
					}
				}
			}
		}
		for (ClassNode node : injector.getClasses()) {
			for(MethodNode method : node.methods) {
				
			}
		}
	}
	
	private boolean isList(String desc) {
		if(cachedLists.containsKey(desc)) {
			return cachedLists.get(desc);
		}
		boolean check = checkList(desc);
		cachedLists.put(desc, check);
		return check;
	}

	private boolean checkList(String desc) {
		Type t = Type.getType(desc);
		if(t.getSort() == Type.ARRAY) {
			return false;
		}
		ClassNode checkedNode = injector.getClass(t.getInternalName());
		while(checkedNode != null) {
			if(checkedNode.interfaces.contains("java/util/List")) {
				return true;
			}
			if(Constants.LISTS.contains("L" + checkedNode.superName + ";")) {
				return true;
			}
			checkedNode = injector.getClass(checkedNode.superName);
		}
		return false;
	}
}
