package org.mcphackers.rdi.injector.visitors;

import java.util.List;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class ClassVisitor {
	
	protected ClassVisitor cv;

	public ClassVisitor(ClassVisitor classVisitor) {
		cv = classVisitor;
	}
	
	public final void visit(List<ClassNode> nodes) {
		for(ClassNode node : nodes) {
			visit(node);
		}
	}
	
	public final void visit(ClassNode node) {
		visitClass1(node);
		for(InnerClassNode inner : node.innerClasses) {
			visitInner1(inner);
		}
		for(MethodNode method : node.methods) {
			visitMethod1(method);
		}
		for(FieldNode field : node.fields) {
			visitField1(field);
		}
		visitEnd1(node);
	}
	
	private void visitClass1(ClassNode node) {
		if (cv != null) {
		  cv.visitClass1(node);
		}
		visitClass(node);
	}

	private void visitInner1(InnerClassNode node) {
		if (cv != null) {
		  cv.visitInner1(node);
		}
		visitInner(node);
	}

	private void visitMethod1(MethodNode node) {
		if (cv != null) {
		  cv.visitMethod1(node);
		}
		visitMethod(node);
	}

	private void visitField1(FieldNode node) {
		if (cv != null) {
		  cv.visitField1(node);
		}
		visitField(node);
	}
	
	private void visitEnd1(ClassNode node) {
		if (cv != null) {
		  cv.visitEnd1(node);
		}
		visitEnd(node);
	}
	
	protected void visitClass(ClassNode node) {
	}

	protected void visitInner(InnerClassNode node) {
	}

	protected void visitMethod(MethodNode node) {
	}

	protected void visitField(FieldNode node) {
	}
	
	protected void visitEnd(ClassNode node) {
	}
}
