package org.mcphackers.rdi.injector.visitors;

import java.util.List;

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
	
	public final void visit(List<ClassNode> nodes) {
		for(ClassNode node : nodes) {
			visit(node);
		}
	}
	
	public final void visit(ClassNode node) {
		visitClass(node);
		for(InnerClassNode inner : node.innerClasses) {
			visitInner(inner);
		}
		for(MethodNode method : node.methods) {
			visitMethod(method);
		}
		for(FieldNode field : node.fields) {
			visitField(field);
		}
		visitEnd(node);
	}
	
	protected void visitClass(ClassNode node) {
		if (cv != null) {
	      cv.visitClass(node);
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
	
	protected void visitEnd(ClassNode node) {
		if (cv != null) {
	      cv.visitEnd(node);
	    }
	}
}
