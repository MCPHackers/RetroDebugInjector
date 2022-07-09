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
        return renames.get(new FieldReference(owner, descriptor, oldName));
    }

    public String getOrDefault(String owner, String descriptor, String oldName, String defaultValue) {
        return renames.getOrDefault(new FieldReference(owner, descriptor, oldName), defaultValue);
    }

    public String optGet(String owner, String descriptor, String oldName) {
        return renames.getOrDefault(new FieldReference(owner, descriptor, oldName), oldName);
    }

    public void put(String owner, String descriptor, String name, String newName) {
        renames.put(new FieldReference(owner, descriptor, name), newName);
    }

    /**
     * Merges the entries of a {@link FieldRenameMap} into this map. Does not override already existing entries.
     *
     * @param other the rename map to merge
     */
    public void putAllIfAbsent(FieldRenameMap other) {
        other.renames.forEach(this.renames::putIfAbsent);
    }

    public int size() {
        return renames.size();
    }
}
