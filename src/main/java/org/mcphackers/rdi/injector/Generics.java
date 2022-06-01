package org.mcphackers.rdi.injector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Generics {

	private Map<String, String> fieldGenerics = new HashMap<>();
	private Map<String, String> methodGenerics = new HashMap<>();

	public Generics(Path file) {
		load(file);
	}

	public boolean load(Path file) {
		this.fieldGenerics.clear();
		this.methodGenerics.clear();
		try {
			Files.readAllLines(file).forEach(line -> {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#"))
					return;
				int idx = line.indexOf(' ');
				switch (line.substring(0, idx)) {
				case "m":
				case "method": {
					String key = line.substring(idx + 1, line.indexOf(' ', idx + 1));
					this.methodGenerics.put(key, line.substring(line.lastIndexOf(' ') + 1));
					break;
				}
				case "f":
				case "field": {
					String key = line.substring(idx + 1, line.indexOf(' ', idx + 1));
					this.fieldGenerics.put(key, line.substring(line.lastIndexOf(' ') + 1));
					break;
				}
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public String getMethodSignature(String cls, String name, String desc) {
		return methodGenerics.get(cls + "/" + name + desc);
	}
	
	public String getFieldSignature(String cls, String name) {
		return fieldGenerics.get(cls + "/" + name);
		
	}

}
