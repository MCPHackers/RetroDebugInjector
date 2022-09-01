package org.mcphackers.rdi.injector.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Exceptions {
	
	public Exceptions(Path file) {
		load(file);
	}

	public Map<String, List<String>> exceptions = new HashMap<>();

	public boolean load(Path file) {
		this.exceptions.clear();
		try {
			Files.readAllLines(file).forEach(line -> {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#"))
					return;
				// New: net/minecraft/client/Minecraft/startGame ()V org/lwjgl/LWJGLException java/io/IOException
				// Old: net/minecraft/client/Minecraft.startGame()V=org/lwjgl/LWJGLException,java/io/IOException
				boolean oldFormat = line.contains(".");
				int idx = oldFormat ? line.lastIndexOf('=') : line.indexOf(' ', line.indexOf(' ') + 1);
				if (idx == -1) return;
				String key = oldFormat ? line.substring(0, idx).replace('.', '/').replace("(", " (") : line.substring(0, idx);
				List<String> excs = Arrays.asList(line.substring(idx + 1).split(oldFormat ? "," : " "));
				this.exceptions.put(key, excs);
			});
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean dump(Path file) {
		try {
			List<String> ret = this.exceptions.entrySet().stream()
					.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
					.map(e -> e.getKey() + " " + String.join(" ", e.getValue())).collect(Collectors.toList());
			Files.write(file, String.join("\n", ret).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public List<String> getExceptions(String cls, String name, String desc) {
		List<String> ret = this.exceptions.get(cls + "/" + name + " " + desc);
		return ret == null ? new ArrayList<>() : ret;
	}

	public void setExceptions(String cls, String name, String desc, List<String> excs) {
		this.exceptions.put(cls + "/" + name + " " + desc, excs);
	}
}
