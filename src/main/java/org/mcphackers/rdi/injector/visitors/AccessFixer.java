package org.mcphackers.rdi.injector.visitors;

import org.mcphackers.rdi.injector.data.Access;
import org.mcphackers.rdi.injector.data.Access.Level;
import org.mcphackers.rdi.util.FieldReference;
import org.mcphackers.rdi.util.MethodReference;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class AccessFixer extends ClassVisitor {
	private final Access context;
	private ClassNode classNode;

	public AccessFixer(Access context, ClassVisitor cv) {
		super(cv);
		this.context = context;
	}

	@Override
	public void visitClass(ClassNode node) {
		this.classNode = node;
		Level old = Level.getFromBytecode(node.access);
		Level _new = context.getLevel(node.name);
		if (_new != null && old != _new) {
			node.access = _new.setAccess(node.access);
		}
	}

	@Override
	public void visitField(FieldNode node) {
		Level old = Level.getFromBytecode(node.access);
		Level _new = context.getLevel(new FieldReference(classNode.name, node));
		if (_new != null && old != _new) {
			node.access = _new.setAccess(node.access);
		}
	}

	@Override
	public void visitMethod(MethodNode node) {
		Level old = Level.getFromBytecode(node.access);
		Level _new = context.getLevel(new MethodReference(classNode.name, node));
		if (_new != null && old != _new) {
			node.access = _new.setAccess(node.access);
		}
	}
}
