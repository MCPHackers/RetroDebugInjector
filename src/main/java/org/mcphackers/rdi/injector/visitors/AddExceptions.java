package org.mcphackers.rdi.injector.visitors;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mcphackers.rdi.injector.data.Exceptions;
import org.mcphackers.rdi.util.MethodReference;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class AddExceptions extends ClassVisitor {
	private final Exceptions context;
	private ClassNode classNode;

	public AddExceptions(Exceptions context, ClassVisitor cn) {
		super(cn);
		this.context = context;
	}

	@Override
	public void visitClass(ClassNode node) {
		this.classNode = node;
	}

	@Override
	public void visitMethod(MethodNode node) {
		if (!node.name.equals("<clinit>")) {
			node.exceptions = processExceptions(new MethodReference(classNode.name, node), node.exceptions);
		}
	}

	private List<String> processExceptions(MethodReference ref, List<String> exceptions) {
		Set<String> set = new HashSet<>(context.getExceptions(ref));
		if (exceptions != null) {
			for (String s : exceptions)
				set.add(s);
		}

		if (set.size() > (exceptions == null ? 0 : exceptions.size())) {
			exceptions = set.stream().sorted().collect(Collectors.toList());
			context.setExceptions(ref, exceptions);
		}

		return exceptions;
	}
}
