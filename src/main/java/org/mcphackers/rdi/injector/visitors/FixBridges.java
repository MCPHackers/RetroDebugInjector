package org.mcphackers.rdi.injector.visitors;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class FixBridges extends ClassVisitor {
	private static final boolean DEBUG = true;
	
	public FixBridges(ClassVisitor cv) {
		super(cv);
	}

	protected void visitClass(ClassNode node) {
		super.visitClass(node);
		boolean resolveTRArtifact = true;
        if (node.interfaces.size() != 1) {
            return;
        }
        if (!node.interfaces.get(0).equals("java/util/Comparator")) {
        	return;
        }
        // Ljava/lang/Object;Ljava/util/Comparator<Lorg/junit/runner/Description;>;
        for (MethodNode method : node.methods) {
        	try {
				if ((method.access & Opcodes.ACC_SYNTHETIC) == 0) {
		            continue;
		        }
		        if (method.name.equals("compare") && method.desc.equals("(Ljava/lang/Object;Ljava/lang/Object;)I")) {
		        	if(node.signature != null && !node.signature.equals("Ljava/lang/Object;Ljava/util/Comparator<Ljava/lang/Object;>")) {
		        		node.methods.remove(method);
		        		break;
		        	}
		            AbstractInsnNode insn = method.instructions.getFirst();
		            while (insn instanceof LabelNode || insn instanceof LineNumberNode) {
		                insn = insn.getNext();
		            }
		            if (insn.getOpcode() != Opcodes.ALOAD) {
		                throw new IllegalStateException("invalid bridge method: unexpected opcode");
		            }
		            VarInsnNode aloadThis = (VarInsnNode) insn;
		            if (aloadThis.var != 0) {
		                throw new IllegalStateException("invalid bridge method: unexpected variable loaded");
		            }
		            insn = insn.getNext();
		            if (insn.getOpcode() != Opcodes.ALOAD) {
		                throw new IllegalStateException("invalid bridge method: unexpected opcode");
		            }
		            insn = insn.getNext();
		            if (insn.getOpcode() != Opcodes.CHECKCAST) {
		                throw new IllegalStateException("invalid bridge method: unexpected opcode");
		            }
		            insn = insn.getNext();
		            if (insn.getOpcode() != Opcodes.ALOAD) {
		                throw new IllegalStateException("invalid bridge method: unexpected opcode");
		            }
		            insn = insn.getNext();
		            if (insn.getOpcode() != Opcodes.CHECKCAST) {
		                throw new IllegalStateException("invalid bridge method: unexpected opcode");
		            }
		            insn = insn.getNext();
		            if (insn.getOpcode() != Opcodes.INVOKEVIRTUAL) {
		                throw new IllegalStateException("invalid bridge method: unexpected opcode");
		            }
		            MethodInsnNode invokevirtual = (MethodInsnNode) insn;
		            insn = insn.getNext();
		            if (insn.getOpcode() != Opcodes.IRETURN) {
		                throw new IllegalStateException("invalid bridge method: unexpected opcode");
		            }
		            boolean methodCallIsInvalid = true;
		            for (MethodNode m : node.methods) {
		                if (m.name.equals(invokevirtual.name) && m.desc.equals(invokevirtual.desc)) {
		                    methodCallIsInvalid = false;
		                    break;
		                }
		            }
		            if (methodCallIsInvalid) {
		                if (resolveTRArtifact) {
		                    // Tiny remapper artifact
		                    invokevirtual.name = "compare";
		                } else {
		                    throw new IllegalStateException("invalid bridge method: method does not exist (consider setting resolveTRArtifact to true)");
		                }
		            }
		            String generics = invokevirtual.desc.substring(1, invokevirtual.desc.indexOf(';'));
		            node.signature = "Ljava/lang/Object;Ljava/util/Comparator<" + generics + ";>;";
		            method.access |= Opcodes.ACC_BRIDGE;
		            break;
		        }
        	} catch (IllegalStateException e) {
	            method.access &= ~Opcodes.ACC_BRIDGE;
	        	if(DEBUG) e.printStackTrace();
        	}
	    }
	}

}
