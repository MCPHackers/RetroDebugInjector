package org.mcphackers.rdi.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public class IOUtil {

	public static List<ClassNode> readJar(File path) throws IOException {
		final List<ClassNode> nodes = new ArrayList<ClassNode>();
		ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(path));
		try {
			ZipEntry zipEntry;
			while ((zipEntry = zipInputStream.getNextEntry()) != null) {
				if (zipEntry.getName().endsWith(".class")) {
					ClassReader classReader = new ClassReader(zipInputStream);
					ClassNode classNode = new ClassNode();
					classReader.accept(classNode, 0);
					nodes.add(classNode);
				}
			}
		} catch (IOException e) {
			zipInputStream.close();
			throw e;
		}
		return nodes;
	}
	
	public static List<ClassNode> readDirectory(File path) throws IOException {
		final List<ClassNode> classes = new ArrayList<ClassNode>();
		for(String fileName : path.list()) {
			if(!fileName.endsWith(".class")) {
				continue;
			}
			File file = new File(path, fileName);
			if(!file.isFile()) {
				continue;
			}
			ClassReader reader = new ClassReader(new FileInputStream(file));
			ClassNode classNode = new ClassNode();
			reader.accept(classNode, 0);
			classes.add(classNode);
		}
		return classes;
	}
}
