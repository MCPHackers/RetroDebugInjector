package org.mcphackers.rdi.injector.remapper;

import java.util.HashMap;
import java.util.Map;

import org.mcphackers.rdi.util.MethodReference;

public class MethodRenameMap {

    private final Map<MethodReference, String> renames = new HashMap<>();

    public MethodRenameMap() {
    }

    public void clear() {
        renames.clear();
    }

    public String get(String owner, String descriptor, String oldName) {
        return renames.get(new MethodReference(owner, descriptor, oldName));
    }

    public String getOrDefault(String owner, String descriptor, String oldName, String defaultValue) {
        return renames.getOrDefault(new MethodReference(owner, descriptor, oldName), defaultValue);
    }

    public String optGet(String owner, String descriptor, String oldName) {
        return renames.getOrDefault(new MethodReference(owner, descriptor, oldName), oldName);
    }

    public void put(String owner, String descriptor, String name, String newName) {
        renames.put(new MethodReference(owner, descriptor, name), newName);
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
        renames.remove(new MethodReference(owner, desc, name));
    }

    public void putAllIfAbsent(MethodRenameMap other) {
        other.renames.forEach(this.renames::putIfAbsent);
    }

    public int size() {
        return renames.size();
    }
}
