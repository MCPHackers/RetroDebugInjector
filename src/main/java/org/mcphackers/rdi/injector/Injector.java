package org.mcphackers.rdi.injector;

import java.util.List;

import org.objectweb.asm.tree.ClassNode;

public interface Injector {
	
	void setClasses(List<ClassNode> nodes);

	List<ClassNode> getClasses();
	
	ClassNode getClass(String className);
	
	void transform();
}
