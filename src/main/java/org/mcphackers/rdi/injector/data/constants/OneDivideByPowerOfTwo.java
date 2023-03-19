package org.mcphackers.rdi.injector.data.constants;

import org.mcphackers.rdi.util.InsnHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public class OneDivideByPowerOfTwo extends Constant {

	@Override
	public boolean replace(InsnList sourceList, LdcInsnNode constant) {
		if(constant.cst instanceof Double) {
			double divisor = 1D;
			double cst = (Double)constant.cst;
			if(cst > 1D / 16D) {
				return false;
			}
			String d = Double.toString(cst);
			if(!d.endsWith("5")) {
				return false;
			}
			while(cst < 1 && !Double.isInfinite(cst)) {
				divisor *= 2D;
				cst *= 2D;
			}
			if(cst == 1D) {
				InsnList list = new InsnList();
				list.add(new InsnNode(Opcodes.DCONST_1));
				list.add(InsnHelper.doubleInsn(divisor));
				list.add(new InsnNode(Opcodes.DDIV));
				sourceList.insert(constant, list);
				sourceList.remove(constant);
				return true;
			}
			return false;
		}
		else if(constant.cst instanceof Float) {
			float divisor = 1F;
			float cst = (Float)constant.cst;
			if(cst > 1F / 16F) {
				return false;
			}
			String f = Float.toString(cst);
			if(!f.endsWith("5")) {
				return false;
			}
			while(cst < 1 && Float.isInfinite(cst)) {
				divisor *= 2F;
				cst *= 2F;
			}
			if(cst == 1F) {
				InsnList list = new InsnList();
				list.add(new InsnNode(Opcodes.FCONST_1));
				list.add(InsnHelper.floatInsn(divisor));
				list.add(new InsnNode(Opcodes.FDIV));
				sourceList.insert(constant, list);
				sourceList.remove(constant);
				return true;
			}
			return false;
		}
		return false;
			
	}

	@Override
	public Constant copy() {
		return new OneDivideByPowerOfTwo();
	}

}
