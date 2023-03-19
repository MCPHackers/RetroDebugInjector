package org.mcphackers.rdi.util;

import java.util.Locale;

import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class MethodReference {

	private final String desc;
	private final String name;
	private final String owner;

	public MethodReference(MethodInsnNode instruction) {
		this(instruction.owner, instruction.name, instruction.desc);
	}

	public MethodReference(String owner, MethodNode node) {
		this(owner, node.name, node.desc);
	}

	public MethodReference(String owner, String name, String desc) {
		this.owner = owner;
		this.name = name;
		this.desc = desc;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof MethodReference)) {
			return false;
		}
		MethodReference other = (MethodReference) obj;
		return other.name.equals(this.name) && other.desc.equals(this.desc) && other.owner.equals(this.owner);
	}

	/**
	 * Obtains the method descriptor of the referenced method
	 *
	 * @return The method descriptor
	 */
	public String getDesc() {
		return desc;
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

	@Override
	public int hashCode() {
		return (owner.hashCode() & 0xFFFF0000 | name.hashCode() & 0x0000FFFF) ^ desc.hashCode();
	}

	@Override
	public String toString() {
		return String.format(Locale.ENGLISH, "MethodReference[owner=\"%s\",name=\"%s\",desc=\"%s\"]", this.owner, this.name, this.desc);
	}
}
