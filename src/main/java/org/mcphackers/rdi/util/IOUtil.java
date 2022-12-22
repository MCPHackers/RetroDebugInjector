package org.mcphackers.rdi.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public class IOUtil {

	public static List<ClassNode> read(Path path) {
		List<ClassNode> nodes = new ArrayList<>();
		try {
			if(Files.isDirectory(path)) {
				Files.walk(path).forEach(file -> {
					if(file.getFileName().toString().endsWith(".class")) {
						ClassReader classReader;
						try {
							classReader = new ClassReader(Files.readAllBytes(file));
						} catch (IOException e) {
							return;
						}
						ClassNode classNode = new ClassNode();
						classReader.accept(classNode, 0);
						nodes.add(classNode);
					}
				});
			}
			else {
				try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(path))) {
					ZipEntry zipEntry;
					while ((zipEntry = zipInputStream.getNextEntry()) != null) {
						if (zipEntry.getName().endsWith(".class")) {
							ClassReader classReader = new ClassReader(zipInputStream);
							ClassNode classNode = new ClassNode();
							classReader.accept(classNode, 0);
							nodes.add(classNode);
						}
					}
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return nodes;
	}
}
