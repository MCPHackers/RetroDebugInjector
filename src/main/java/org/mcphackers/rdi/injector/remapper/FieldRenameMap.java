package org.mcphackers.rdi.injector.remapper;

import java.util.HashMap;
import java.util.Map;

import org.mcphackers.rdi.util.FieldReference;

public final class FieldRenameMap {

	private final Map<FieldReference, String> renames = new HashMap<FieldReference, String>();

	public FieldRenameMap() {
	}

	public void clear() {
		renames.clear();
	}

	public String get(String owner, String descriptor, String oldName) {
		return renames.get(new FieldReference(owner, oldName, descriptor));
	}

	public String getOrDefault(String owner, String descriptor, String oldName, String defaultValue) {
		FieldReference ref = new FieldReference(owner, oldName, descriptor);
		if(!renames.containsKey(ref)) {
			return defaultValue;
		}
		return renames.get(ref);
	}

	public String optGet(String owner, String descriptor, String oldName) {
		FieldReference ref = new FieldReference(owner, oldName, descriptor);
		if(!renames.containsKey(ref)) {
			return oldName;
		}
		return renames.get(ref);
	}

	public void put(String owner, String descriptor, String name, String newName) {
		renames.put(new FieldReference(owner, name, descriptor), newName);
	}

	/**
	 * Merges the entries of a {@link FieldRenameMap} into this map.
	 *
	 * @param other the rename map to merge
	 */
	public void putAll(FieldRenameMap other) {
		this.renames.putAll(other.renames);
	}

	public int size() {
		return renames.size();
	}
}
