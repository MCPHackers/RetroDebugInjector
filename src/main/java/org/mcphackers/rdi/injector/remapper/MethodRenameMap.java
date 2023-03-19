package org.mcphackers.rdi.injector.remapper;

import java.util.HashMap;
import java.util.Map;

import org.mcphackers.rdi.util.MethodReference;

public class MethodRenameMap {

	private final Map<MethodReference, String> renames = new HashMap<MethodReference, String>();
	private final Map<MethodReference, String[]> lvt = new HashMap<MethodReference, String[]>();

	public MethodRenameMap() {
	}

	public void clear() {
		renames.clear();
	}

	public String get(String owner, String descriptor, String oldName) {
		return renames.get(new MethodReference(owner, oldName, descriptor));
	}

	public String getOrDefault(String owner, String descriptor, String oldName, String defaultValue) {
		MethodReference ref = new MethodReference(owner, oldName, descriptor);
		if(!renames.containsKey(ref)) {
			return defaultValue;
		}
		return renames.get(ref);
	}

	public String optGet(String owner, String descriptor, String oldName) {
		MethodReference ref = new MethodReference(owner, oldName, descriptor);
		if(!renames.containsKey(ref)) {
			return oldName;
		}
		return renames.get(ref);
	}

	public void put(String owner, String descriptor, String name, String newName) {
		renames.put(new MethodReference(owner, name, descriptor), newName);
	}

	/**
	 * Removes a method remapping entry from the method remapping list. This method practically undoes {@link MethodRenameMap#put(String, String, String, String)}.
	 * Like put remove only affects a SINGLE method in a SINGLE class and it's references.
	 * Note that implicitly declared/inherited methods must also be added to the remap list, sperately.
	 *
	 * @param owner The class of the method that should not be remapped
	 * @param desc The descriptor of the method to not remap
	 * @param name The name of the method that should not be remapped
	 */
	public void remove(String owner, String desc, String name) {
		renames.remove(new MethodReference(owner, name, desc));
	}

	public void putAll(MethodRenameMap other) {
		this.renames.putAll(other.renames);
		this.lvt.putAll(other.lvt);
	}

	public int size() {
		return renames.size();
	}

	public String[] getLVMappings(String owner, String descriptor, String oldName) {
		return lvt.get(new MethodReference(owner, oldName, descriptor));
	}

	public void setLVMappings(String owner, String descriptor, String oldName, String[] params) {
		lvt.put(new MethodReference(owner, oldName, descriptor), params);
	}
}
