package org.mcphackers.rdi.injector.data.constants;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;

public abstract class Constant {

	public boolean replace(InsnList sourceList, AbstractInsnNode constant) {
		if(constant.getType() == AbstractInsnNode.LDC_INSN) {
			return replace(sourceList, (LdcInsnNode)constant);
		}
		return false;
	}
	
	public abstract boolean replace(InsnList sourceList, LdcInsnNode constant);
	
	public abstract Constant copy();

}
