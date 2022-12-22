package org.mcphackers.rdi.injector.data.constants;

import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.rdi.util.InsnHelper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public class InsnConstant<T> extends Constant {
	
	protected InsnList insnList;
	final T value;
	final Class<?> type;
	
	public InsnConstant(InsnList insns, T constValue) {
		insnList = insns;
		type = constValue.getClass();
		value = constValue;
	}

	@Override
	public boolean replace(InsnList sourceList, LdcInsnNode constant) {
		return replace(sourceList, (AbstractInsnNode)constant);
	}

	public boolean replace(InsnList sourceList, AbstractInsnNode constant) {
		Object constValue;
		if(constant.getOpcode() == LDC) {
			constValue = ((LdcInsnNode)constant).cst;
		} else if(constant.getOpcode() == SIPUSH || constant.getOpcode() == BIPUSH) {
			constValue = ((IntInsnNode)constant).operand;
		} else {
			return false;
		}
		if(type.isInstance(constValue)) {
			if(constValue.equals(value)) {
				replace(sourceList, constant, InsnHelper.clone(insnList));
				return true;
			}
			else if(constValue.equals(getNegative(value))) {
				InsnList insert = InsnHelper.clone(insnList);
				insert.add(getNegateInstruction());
				replace(sourceList, constant, insert);
				return true;
			}
		}
		return false;
	}

	public void replace(InsnList sourceList, AbstractInsnNode constant, InsnList replaceList) {
		if(replaceList.size() == 1) {
			sourceList.set(constant, replaceList.get(0));
			return;
		}
		sourceList.insert(constant, replaceList);
		sourceList.remove(constant);
	}
	
	protected Object getNegative(T value) {
		if(value instanceof Integer) {
			return -(Integer)value;
		}
		if(value instanceof Long) {
			return -(Long)value;
		}
		if(value instanceof Float) {
			return -(Float)value;
		}
		if(value instanceof Double) {
			return -(Double)value;
		}
		return null;
	}
	
	protected AbstractInsnNode getNegateInstruction() {
		if(value instanceof Integer) {
			return new InsnNode(INEG);
		}
		if(value instanceof Long) {
			return new InsnNode(LNEG);
		}
		if(value instanceof Float) {
			return new InsnNode(FNEG);
		}
		if(value instanceof Double) {
			return new InsnNode(DNEG);
		}
		return null;
	}

	@Override
	public Constant copy() {
		return new InsnConstant<T>(InsnHelper.clone(insnList), value);
	}

}
