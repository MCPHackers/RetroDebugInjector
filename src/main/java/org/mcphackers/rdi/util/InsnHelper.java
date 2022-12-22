package org.mcphackers.rdi.util;

import static org.objectweb.asm.Opcodes.*;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;

public class InsnHelper {

	public static AbstractInsnNode[] fill(AbstractInsnNode insn, int size) {
		AbstractInsnNode[] arr = new AbstractInsnNode[size];
		int i = 0;
		while(insn != null && i < size) {
			arr[i] = insn;
			insn = insn.getNext();
			i++;
		}
		return arr;
	}

	public static InsnList clone(InsnList insnList) {
		Map<LabelNode, LabelNode> labels = new HashMap<>();
		for (AbstractInsnNode insn : insnList) {
		    if (insn.getType() == AbstractInsnNode.LABEL) {
		        LabelNode label = (LabelNode) insn;
		        labels.put(label, new LabelNode());
		    }
		}

		InsnList destList = new InsnList();
		for (AbstractInsnNode insn : insnList) {
		    AbstractInsnNode insnCopy = insn.clone(labels);
		    destList.add(insnCopy);
		}
		return destList;
	}

	public static AbstractInsnNode intInsn(int value) {
		switch(value) {
		case -1:
			return new InsnNode(ICONST_M1);
		case 0:
			return new InsnNode(ICONST_0);
		case 1:
			return new InsnNode(ICONST_1);
		case 2:
			return new InsnNode(ICONST_2);
		case 3:
			return new InsnNode(ICONST_3);
		case 4:
			return new InsnNode(ICONST_4);
		case 5:
			return new InsnNode(ICONST_5);
		default:
			if(value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
				return new IntInsnNode(BIPUSH, value);
			}
			else if(value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
				return new IntInsnNode(SIPUSH, value);
			}
			else {
				return new LdcInsnNode(value);
			}
		}
	}
	
	public static AbstractInsnNode longInsn(long value) {
		if(value == 0L) {
			return new InsnNode(LCONST_0);
		}
		else if(value == 1L) {
			return new InsnNode(LCONST_1);
		}
		else {
			return new LdcInsnNode(value);
		}
	}

	public static AbstractInsnNode booleanInsn(boolean value) {
		return new InsnNode(value ? ICONST_1 : ICONST_0);
	}

	public static AbstractInsnNode floatInsn(float value) {
		if(value == 0F) {
			return new InsnNode(FCONST_0);
		}
		else if(value == 1F) {
			return new InsnNode(FCONST_1);
		}
		else if(value == 2F) {
			return new InsnNode(FCONST_2);
		}
		else {
			return new LdcInsnNode(value);
		}
	}

	public static AbstractInsnNode doubleInsn(double value) {
		if(value == 0D) {
			return new InsnNode(DCONST_0);
		}
		else if(value == 1D) {
			return new InsnNode(DCONST_1);
		}
		else {
			return new LdcInsnNode(value);
		}
	}

}
