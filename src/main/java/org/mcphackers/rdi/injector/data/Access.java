package org.mcphackers.rdi.injector.data;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.mcphackers.rdi.util.FieldReference;
import org.mcphackers.rdi.util.MethodReference;

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

	private Map<String, Level> classes = new HashMap<>();
	private Map<FieldReference, Level> fields = new HashMap<>();
	private Map<MethodReference, Level> methods = new HashMap<>();

	public Access(Path file) {
		load(file);
	}

	public boolean load(Path file) {
		this.classes.clear();
		try {
			Files.readAllLines(file).forEach(line -> {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#"))
					return;
				// PUBLIC net/minecraft/client/Minecraft
				// PRIVATE net/minecraft/client/Minecraft fullscreen
				// PROTECTED net/minecraft/client/Minecraft startGame ()V
				int idx = line.indexOf(' ');
				Level level = Level.valueOf(line.substring(0, idx));
				String[] keys = line.substring(idx + 1).trim().split(" ");
				switch (keys.length) {
				case 1:
					this.classes.put(keys[0], level);
					break;
				case 2:
					this.fields.put(new FieldReference(keys[0], keys[1], ""), level);
					break;
				case 3:
					this.methods.put(new MethodReference(keys[0], keys[1], keys[2]), level);
					break;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

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
