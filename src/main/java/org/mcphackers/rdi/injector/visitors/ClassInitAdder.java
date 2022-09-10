package org.mcphackers.rdi.injector.visitors;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.RETURN;

import java.util.ArrayList;
import java.util.List;

import org.mcphackers.rdi.injector.data.ClassStorage;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class ClassInitAdder extends ClassVisitor {

	private final ClassStorage storage;

	public ClassInitAdder(ClassStorage storage, ClassVisitor cv) {
		super(cv);
		this.storage = storage;
	}

	@Override
	public void visitClass(ClassNode node) {
		String superName = node.superName;
		List<String> constructors = new ArrayList<>();
		for(MethodNode method : node.methods) {
			if("<init>".equals(method.name)) {
				constructors.add(method.desc);
			}
		}

		ClassNode superClass = storage.getClass(superName);
		if(superClass == null) {
			return;
		}
		if(!constructors.isEmpty()) {
			return;
		}
		for(MethodNode method : superClass.methods) {
			// Adding implicit constructor
			if("<init>".equals(method.name)) {
				int maxLocals = 1;
				MethodNode mn = new MethodNode(ACC_PRIVATE, "<init>", method.desc, null, null);
				mn.visitVarInsn(ALOAD, 0);
				for(int i = 0; i < Type.getArgumentTypes(method.desc).length; i++) {
					mn.visitVarInsn(ALOAD, i + 1);
					maxLocals++;
				}
				mn.visitMethodInsn(INVOKESPECIAL, superName, "<init>", method.desc, false);
				mn.visitInsn(RETURN);
				mn.visitMaxs(maxLocals, maxLocals);
				node.methods.add(0, mn);
				break;
			}
		}
	}
}
