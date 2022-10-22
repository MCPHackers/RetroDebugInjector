package org.mcphackers.rdi.injector.visitors;

import java.util.ArrayList;
import java.util.List;

import org.mcphackers.rdi.injector.data.ClassStorage;
import org.objectweb.asm.Opcodes;
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
				node.methods.add(0, createMethod(superName, method.name, method.desc));
				break;
			}
		}
	}
	
	public static MethodNode createMethod(String superClass, String name, String desc) {
		MethodNode mn = new MethodNode(Opcodes.ACC_PRIVATE, name, desc, null, null);
		int maxLocals = 1;
		mn.visitVarInsn(Opcodes.ALOAD, 0);
		Type[] types = Type.getArgumentTypes(desc);
		int varIndex = 1;
		for(int i = 0; i < types.length; i++) {
			mn.visitVarInsn(types[i].getOpcode(Opcodes.ILOAD), varIndex);
			maxLocals++;
			varIndex += types[i].getSize();
		}
		mn.visitMethodInsn(Opcodes.INVOKESPECIAL, superClass, name, desc, false);
		mn.visitInsn(Opcodes.RETURN);
		mn.visitMaxs(varIndex, maxLocals);
		return mn;
	}
}
