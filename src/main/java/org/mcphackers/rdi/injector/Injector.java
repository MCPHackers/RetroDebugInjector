package org.mcphackers.rdi.injector;

import org.mcphackers.rdi.injector.data.ClassStorage;

public interface Injector {
	
	void setStorage(ClassStorage storage);
	
	ClassStorage getStorage();
	
	void transform();
}
