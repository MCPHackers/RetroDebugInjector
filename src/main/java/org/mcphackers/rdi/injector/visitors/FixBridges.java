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
	private static final boolean DEBUG = false;
	
	public FixBridges(ClassVisitor cv) {
		super(cv);
	}

	protected void visitClass(ClassNode node) {
		super.visitClass(node);
		fixComparators(node);
	}
	
	private void fixComparators(ClassNode node) {

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
		            int[] opcodes = {
		            		Opcodes.ALOAD,
		            		Opcodes.ALOAD,
		            		Opcodes.CHECKCAST,
		            		Opcodes.ALOAD,
		            		Opcodes.CHECKCAST,
		            		Opcodes.INVOKEVIRTUAL,
		            		Opcodes.IRETURN
		            		};
		            MethodInsnNode invokevirtual = null;
		            for(int i = 0; i < opcodes.length; i++) {
		            	if(i != 0) {
				            insn = insn.getNext();
		            	}
			            if (insn.getOpcode() != opcodes[i]) {
			                throw new IllegalStateException("invalid bridge method: unexpected opcode");
			            }
		            	if(i == 0) {
		            		VarInsnNode aloadThis = (VarInsnNode) insn;
				            if (aloadThis.var != 0) {
				                throw new IllegalStateException("invalid bridge method: unexpected variable loaded");
				            }
		            	}
		            	if(opcodes[i] == Opcodes.INVOKEVIRTUAL) {
				            invokevirtual = (MethodInsnNode) insn;
		            	}
		            }
		            for (MethodNode m : node.methods) {
		                if (m.name.equals(invokevirtual.name) && m.desc.equals(invokevirtual.desc)) {
		                	//Rename called method
		                    m.name = invokevirtual.name = "compare";
		                    break;
		                }
		            }
		            //Add generics
		            String generics = invokevirtual.desc.substring(1, invokevirtual.desc.indexOf(';'));
		            node.signature = "Ljava/lang/Object;Ljava/util/Comparator<" + generics + ";>;";
		            //Remove bridge
                    node.methods.remove(method);
		            break;
		        }
        	} catch (IllegalStateException e) {
        		// Not a bridge
	            method.access &= ~Opcodes.ACC_BRIDGE;
	        	if(DEBUG) e.printStackTrace();
        	}
	    }
	}

}
