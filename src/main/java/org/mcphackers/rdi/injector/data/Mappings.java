package org.mcphackers.rdi.injector.data;

import java.util.HashMap;
import java.util.Map;

import org.mcphackers.rdi.injector.remapper.FieldRenameMap;
import org.mcphackers.rdi.injector.remapper.MethodRenameMap;

public class Mappings {
	public final FieldRenameMap fields = new FieldRenameMap();
	public final MethodRenameMap methods = new MethodRenameMap();
	public final Map<String, String> classes = new HashMap<String, String>();
	
	public enum Provider {
		TINY1,
		TINY2;
	}
}
