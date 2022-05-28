package org.mcphackers.rdi.injector;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Access {

	public static enum Level {
		PRIVATE, DEFAULT, PROTECTED, PUBLIC;

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
			acc &= ~(ACC_PRIVATE | ACC_PROTECTED | ACC_PUBLIC);
			acc |= this == PRIVATE ? ACC_PRIVATE : 0;
			acc |= this == PROTECTED ? ACC_PROTECTED : 0;
			acc |= this == PUBLIC ? ACC_PUBLIC : 0;
			return acc;
		}
	}

	private Map<String, Level> changes = new HashMap<>();

	public Access(Path file) {
		load(file);
	}

	public boolean load(Path file) {
		this.changes.clear();
		try {
			Files.readAllLines(file).forEach(line -> {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#"))
					return;

				int idx = line.indexOf(' ');
				Level level = Level.valueOf(line.substring(0, idx));
				String key = line.substring(idx + 1);
				this.changes.put(key, level);
			});
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public Level getLevel(String className) {
		return this.changes.get(className);
	}

	public Level getLevel(String cls, String name) {
		return this.changes.get(cls + " " + name);
	}

	public Level getLevel(String cls, String name, String desc) {
		return this.changes.get(cls + " " + name + " " + desc);
	}

}
