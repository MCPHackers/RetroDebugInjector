package org.mcphackers.rdi.injector.data.constants;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public class FloatCastedToDouble extends Constant {

	@Override
	public boolean replace(InsnList sourceList, LdcInsnNode constant) {
		if(constant.cst instanceof Double) {
			double cstDouble = (Double)constant.cst;
			float cstFloat = (float)cstDouble;
			String d = Double.toString(cstDouble);
			String f = Float.toString(cstFloat);
			if(cstDouble == cstFloat && f.length() < d.length()) {
				InsnList list = new InsnList();
				list.add(new LdcInsnNode(cstFloat));
				list.add(new InsnNode(Opcodes.F2D));
				sourceList.insert(constant, list);
				sourceList.remove(constant);
				return true;
			}
		}
		return false;
	}

	@Override
	public FloatCastedToDouble copy() {
		return this;
	}

}
