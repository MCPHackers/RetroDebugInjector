package org.mcphackers.rdi.injector.remapper;

import java.util.HashMap;
import java.util.Map;

import org.mcphackers.rdi.util.FieldReference;

public final class FieldRenameMap {

	private final Map<FieldReference, String> renames = new HashMap<>();

	public FieldRenameMap() {
	}

	public void clear() {
		renames.clear();
	}

	public String get(String owner, String descriptor, String oldName) {
		return renames.get(new FieldReference(owner, oldName, descriptor));
	}

	public String getOrDefault(String owner, String descriptor, String oldName, String defaultValue) {
		return renames.getOrDefault(new FieldReference(owner, oldName, descriptor), defaultValue);
	}

	public String optGet(String owner, String descriptor, String oldName) {
		return renames.getOrDefault(new FieldReference(owner, oldName, descriptor), oldName);
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
		other.renames.forEach(this.renames::put);
	}

	public int size() {
		return renames.size();
	}
}
