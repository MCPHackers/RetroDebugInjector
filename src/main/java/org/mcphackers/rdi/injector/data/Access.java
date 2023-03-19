package org.mcphackers.rdi.injector.data;

import static org.mcphackers.rdi.injector.transform.Transform.VISIBILITY_MODIFIERS;
import static org.objectweb.asm.Opcodes.*;

import java.util.HashMap;
import java.util.Map;

import org.mcphackers.rdi.util.FieldReference;
import org.mcphackers.rdi.util.MethodReference;

public class Access {

	public static enum Level {
		PRIVATE,
		DEFAULT,
		PROTECTED,
		PUBLIC;

		public static Level getFromBytecode(int acc) {
			if ((acc & ACC_PRIVATE) == ACC_PRIVATE)
				return PRIVATE;
			if ((acc & ACC_PROTECTED) == ACC_PROTECTED)
				return PROTECTED;
			if ((acc & ACC_PUBLIC) == ACC_PUBLIC)
				return PUBLIC;
			return DEFAULT;
		}

		public int setAccess(int acc) {
			acc &= ~(VISIBILITY_MODIFIERS);
			acc |= this == PRIVATE ? ACC_PRIVATE : 0;
			acc |= this == PROTECTED ? ACC_PROTECTED : 0;
			acc |= this == PUBLIC ? ACC_PUBLIC : 0;
			return acc;
		}
	}

	Map<String, Level> classes = new HashMap<String, Level>();
	Map<FieldReference, Level> fields = new HashMap<FieldReference, Level>();
	Map<MethodReference, Level> methods = new HashMap<MethodReference, Level>();

	public Level getLevel(String className) {
		return this.classes.get(className);
	}

	public Level getLevel(FieldReference field) {
		// Recreating reference without the descriptor
		return this.fields.get(new FieldReference(field.getOwner(), field.getName(), ""));
	}

	public Level getLevel(MethodReference method) {
		return this.methods.get(method);
	}

}
