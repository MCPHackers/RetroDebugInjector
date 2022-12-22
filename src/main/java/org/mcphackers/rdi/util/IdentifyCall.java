package org.mcphackers.rdi.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public class IdentifyCall {
	protected MethodInsnNode invokeInsn;
	protected List<AbstractInsnNode[]> arguments;
	
	public IdentifyCall(MethodInsnNode invoke) {
		invokeInsn = invoke;
		arguments = initArguments();
	}
	
	private List<AbstractInsnNode[]> initArguments() {
		Type[] argTypes = Type.getArgumentTypes(invokeInsn.desc);
		List<AbstractInsnNode[]> args = new ArrayList<AbstractInsnNode[]>(argTypes.length);
		if(argTypes.length == 0) {
			return Collections.emptyList();
		}
		AbstractInsnNode insn = invokeInsn.getPrevious();
		AbstractInsnNode start = null;
		AbstractInsnNode end = insn;
		for(int currentArg = argTypes.length - 1; currentArg >= 0; currentArg--) {
			int stack = argTypes[currentArg].getSize();
			end = insn;
			while(stack > 0) {
				stack -= OPHelper.getStackSizeDelta(insn);
				if(stack == 0) {
					start = insn;
				}
				insn = insn.getPrevious();
			}
			if(start != null) {
				while(args.size() <= currentArg) {
					args.add(null);
				}
				args.set(currentArg, range(start, end));
			}
		}
		return args;
	}
	
	public List<AbstractInsnNode[]> getArguments() {
		return Collections.unmodifiableList(arguments);
	}
	
	public AbstractInsnNode[] getArgument(int index) {
		return arguments.get(index);
	}
	
	public static AbstractInsnNode[] range(AbstractInsnNode start, AbstractInsnNode end) {
		List<AbstractInsnNode> list = new LinkedList<>();
		AbstractInsnNode insn = start;
		while(insn != end) {
			list.add(insn);
			insn = insn.getNext();
		}
		list.add(end);
		AbstractInsnNode[] arr = new AbstractInsnNode[list.size()];
		return list.toArray(arr);
	}
}