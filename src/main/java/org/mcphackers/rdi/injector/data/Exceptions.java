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

import org.mcphackers.rdi.util.MethodReference;

public class Exceptions {
	
	public Exceptions(Path file) {
		load(file);
	}

	public Map<MethodReference, List<String>> exceptions = new HashMap<>();

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
				String s = oldFormat ? line.substring(0, idx).replace('.', '/').replace("(", " (") : line.substring(0, idx);
				String s2 = s.substring(0, s.lastIndexOf(' '));
				String desc = s.substring(s.lastIndexOf(' ') + 1);
				String name = s2.substring(s2.lastIndexOf('/') + 1, s2.length());
				String owner = s2.substring(0, s2.lastIndexOf('/'));
				List<String> excs = Arrays.asList(line.substring(idx + 1).split(oldFormat ? "," : " "));
				setExceptions(new MethodReference(owner, name, desc), excs);
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
					.sorted((e1, e2) -> {
						MethodReference ref1 = e1.getKey();
						MethodReference ref2 = e2.getKey();
						return (ref1.getOwner() + "/" + ref1.getName() + ref1.getDesc()).compareTo(ref2.getOwner() + "/" + ref2.getName() + ref2.getDesc());
					})
					.map(e -> {
						MethodReference ref = e.getKey();
						return ref.getOwner() + "/" + ref.getName() + ref.getDesc() + " " + String.join(" ", e.getValue());
					})
					.collect(Collectors.toList());
			Files.write(file, String.join("\n", ret).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public List<String> getExceptions(MethodReference ref) {
		List<String> ret = this.exceptions.get(ref);
		return ret == null ? new ArrayList<>() : ret;
	}

	public void setExceptions(MethodReference ref, List<String> excs) {
		this.exceptions.put(ref, excs);
	}
}
