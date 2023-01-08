package org.mcphackers.rdi.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public class IOUtil {

	public static List<ClassNode> readJar(Path path) throws IOException {
		final List<ClassNode> nodes = new ArrayList<>();
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
		return nodes;
	}
	
	public static List<ClassNode> readDirectory(Path path) throws IOException {
		final List<ClassNode> classes = new ArrayList<>();
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
				if(!file.getFileName().toString().endsWith(".class")) {
					return FileVisitResult.CONTINUE;
				}
				ClassReader reader = new ClassReader(Files.readAllBytes(file));
				ClassNode classNode = new ClassNode();
				reader.accept(classNode, 0);
				classes.add(classNode);
				return FileVisitResult.CONTINUE;
			}
		});
		return classes;
	}
}
