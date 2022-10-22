package org.mcphackers.rdi.util;

import java.util.Locale;
import java.util.Objects;

import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * A reference to a field within a class.
 * This class is similar to {@link FieldNode} / {@link FieldInsnNode} though with a few
 * "useless" features removed so it can be used for one thing only.
 */
public final class FieldReference {

	private final String desc;
	private final String name;
	private final String owner;

	public FieldReference(FieldInsnNode instruction) {
		this(instruction.owner, instruction.name, instruction.desc);
	}

	public FieldReference(String owner, FieldNode node) {
		this(owner, node.name, node.desc);
	}

	public FieldReference(String owner, String name, String desc) {
		this.owner = owner;
		this.name = name;
		this.desc = desc;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof FieldReference)) {
			return false;
		}
		FieldReference other = (FieldReference) obj;
		return other.name.equals(this.name) && other.desc.equals(this.desc) && other.owner.equals(this.owner);
	}

	public String getName() {
		return name;
	}

	/**
	 * Obtains the simple internal name of the owner.
	 * This means it will be like this: "org/example/ClassName"
	 *
	 * @return The simple internal name of the owner
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * Obtains the field descriptor of the referred field
	 *
	 * @return The field descriptor
	 */
	public String getDesc() {
		return desc;
	}

	@Override
	public int hashCode() {
//		return (owner.hashCode() & 0xFFFF0000 | name.hashCode() & 0x0000FFFF) ^ desc.hashCode();
		return Objects.hash(owner, name, desc);
	}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "FieldReference[owner=\"%s\",name=\"%s\",desc=\"%s\"]", this.owner, this.name, this.desc);
	}
}
