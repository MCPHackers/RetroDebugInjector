package org.mcphackers.rdi.injector.transform;

import org.mcphackers.rdi.injector.Generics;
import org.mcphackers.rdi.injector.Injector;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class AddGenerics implements Injection {
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
				}
			}
		}
	}
}
