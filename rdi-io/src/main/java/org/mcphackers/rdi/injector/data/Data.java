package org.mcphackers.rdi.injector.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.mcphackers.rdi.injector.data.Access.Level;
import org.mcphackers.rdi.util.FieldReference;
import org.mcphackers.rdi.util.MethodReference;

public class Data {

	public static Exceptions loadExceptions(File file) {
		Exceptions exceptions = new Exceptions();
		exceptions.exceptions.clear();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			while((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#"))
					continue;
				// New: net/minecraft/client/Minecraft/startGame ()V org/lwjgl/LWJGLException java/io/IOException
				// Old: net/minecraft/client/Minecraft.startGame()V=org/lwjgl/LWJGLException,java/io/IOException
				boolean oldFormat = line.contains(".");
				int idx = oldFormat ? line.lastIndexOf('=') : line.indexOf(' ', line.indexOf(' ') + 1);
				if (idx == -1) continue;
				String s = oldFormat ? line.substring(0, idx).replace('.', '/').replace("(", " (") : line.substring(0, idx);
				String s2 = s.substring(0, s.lastIndexOf(' '));
				String desc = s.substring(s.lastIndexOf(' ') + 1);
				String name = s2.substring(s2.lastIndexOf('/') + 1, s2.length());
				String owner = s2.substring(0, s2.lastIndexOf('/'));
				List<String> excs = Arrays.asList(line.substring(idx + 1).split(oldFormat ? "," : " "));
				exceptions.setExceptions(new MethodReference(owner, name, desc), excs);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return exceptions;
	}
	
	public static Access loadAccess(File file) {
		Access access = new Access();
		access.classes.clear();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			while((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#"))
					continue;
				// PUBLIC net/minecraft/client/Minecraft
				// PRIVATE net/minecraft/client/Minecraft fullscreen
				// PROTECTED net/minecraft/client/Minecraft startGame ()V
				int idx = line.indexOf(' ');
				Level level = Level.valueOf(line.substring(0, idx));
				String[] keys = line.substring(idx + 1).trim().split(" ");
				switch (keys.length) {
				case 1:
					access.classes.put(keys[0], level);
					break;
				case 2:
					access.fields.put(new FieldReference(keys[0], keys[1], ""), level);
					break;
				case 3:
					access.methods.put(new MethodReference(keys[0], keys[1], keys[2]), level);
					break;
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return access;
	}
}
