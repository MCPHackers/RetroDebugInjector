package org.mcphackers.rdi.injector.visitors;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;

import java.util.LinkedList;
import java.util.List;

import org.mcphackers.rdi.injector.Injector;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class ClassInitAdder extends ClassVisitor {

	private Injector injector;
	private String superName;
	private List<String> constructors = new LinkedList<>();

	public ClassInitAdder(Injector injector, ClassVisitor cv) {
		super(cv);
		this.injector = injector;
	}

	@Override
	public void visitMethod(MethodNode node) {
		super.visitMethod(node);
		if ("<init>".equals(node.name)) {
			constructors.add(node.desc);
		}
	}

	@Override
	public void visitEnd(ClassNode node) {
		super.visitEnd(node);
		this.superName = node.superName;

		if(!constructors.isEmpty()) { // No implicit constructor needed
			return;
		}
		ClassNode superClass = injector.getClass(superName);
		for(MethodNode method : superClass.methods) {
			// Adding implicit constructor
			if("<init>".equals(method.name)) {
				MethodNode mn = new MethodNode(ACC_PRIVATE, "<init>", method.desc, null, null);
				mn.visitVarInsn(ALOAD, 0);
				for(int i = 0; i < Type.getArgumentTypes(method.desc).length; i++) {
					mn.visitVarInsn(ALOAD, i + 1);
				}
				mn.visitMethodInsn(INVOKESPECIAL, superName, "<init>", method.desc, false);
				node.methods.add(0, mn);
				break;
			}
		}
	}
}
