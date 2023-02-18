package org.mcphackers.rdi.injector.visitors;

import java.util.ArrayList;
import java.util.List;

import org.mcphackers.rdi.injector.data.ClassStorage;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ClassInitAdder extends ClassVisitor {

	private final ClassStorage storage;

	public ClassInitAdder(ClassStorage storage, ClassVisitor cv) {
		super(cv);
		this.storage = storage;
	}

	@Override
	public void visitClass(ClassNode node) {
		String superName = node.superName;
		List<String> constructors = new ArrayList<String>();
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
		InsnList insns = mn.instructions;
		int maxLocals = 1;
		insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
		Type[] types = Type.getArgumentTypes(desc);
		int varIndex = 1;
		for(int i = 0; i < types.length; i++) {
			insns.add(new VarInsnNode(types[i].getOpcode(Opcodes.ILOAD), varIndex));
			maxLocals++;
			varIndex += types[i].getSize();
		}
		insns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, superClass, name, desc));
		insns.add(new InsnNode(Type.getReturnType(desc).getOpcode(Opcodes.IRETURN)));
		mn.instructions = insns;
		mn.visitMaxs(varIndex, maxLocals);
		return mn;
	}
}
