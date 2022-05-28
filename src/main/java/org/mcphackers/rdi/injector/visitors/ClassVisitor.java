package org.mcphackers.rdi.injector.visitors;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class ClassVisitor {
	
	protected ClassVisitor cv;
	
	public ClassVisitor() {
	}

	public ClassVisitor(ClassVisitor classVisitor) {
		cv = classVisitor;
	}
	
	public void visit(ClassNode node) {
		if (cv != null) {
	      cv.visit(node);
	    }
		for(InnerClassNode inner : node.innerClasses) {
			visitInner(inner);
		}
		for(MethodNode method : node.methods) {
			visitMethod(method);
		}
		for(FieldNode field : node.fields) {
			visitField(field);
		}
			
	}

	protected void visitInner(InnerClassNode node) {
		if (cv != null) {
	      cv.visitInner(node);
	    }
	}

	protected void visitMethod(MethodNode node) {
		if (cv != null) {
	      cv.visitMethod(node);
	    }
	}

	protected void visitField(FieldNode node) {
		if (cv != null) {
	      cv.visitField(node);
	    }
	}
}
