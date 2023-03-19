package org.mcphackers.rdi.nio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.mcphackers.rdi.injector.data.ClassStorage;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public class ClassStorageWriter {
	private Map<String, ClassNode> cache = new HashMap<>();
	protected int flags;
	protected ClassStorage storage;
	protected ClassLoader loader = ClassLoader.getSystemClassLoader();
	
	public ClassStorageWriter(ClassStorage storage, int flags) {
		this.storage = storage;
		this.flags = flags;
	}
	
	public void write(OutputStream out) throws IOException {
		ZipOutputStream jarOut = new ZipOutputStream(out);
		for (ClassNode classNode : storage) {
			ClassWriter writer = new ClassWriter(flags);
			classNode.accept(writer);
			jarOut.putNextEntry(new ZipEntry(classNode.name + ".class"));
			jarOut.write(writer.toByteArray());
			jarOut.closeEntry();
		}
		jarOut.close();
	}

	/**
	 * Write all class nodes and copy over all resources from a given jar.
	 * If no resources need to be copied over, {@link IOUtil#write(ClassStorage, OutputStream)} should be used instead.
	 *
	 * @param out The stream to write the nodes and resources to as a jar
	 * @param resources The list of paths to obtain resources from
	 * @throws IOException If something went wrong while writing to the stream or reading the resources jar.
	 */
	public void write(OutputStream out, List<Path> resources) throws IOException {
		try(ZipOutputStream jarOut = new ZipOutputStream(out)) {
			for (ClassNode classNode : storage) {
				ClassWriter writer = new ClassWriter(flags);
				classNode.accept(writer);
				jarOut.putNextEntry(new ZipEntry(classNode.name + ".class"));
				jarOut.write(writer.toByteArray());
				jarOut.closeEntry();
			}
			List<String> addedResources = new ArrayList<>();
			for(Path path : resources)
			try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(path))) {
				for (ZipEntry entry = zipIn.getNextEntry(); entry != null; entry = zipIn.getNextEntry()) {
					if (entry.getName().startsWith("META-INF/") && !entry.getName().equals("META-INF/MANIFEST.MF") && !entry.isDirectory()) {
						continue;
					}
					if (entry.getName().endsWith(".class")) {
						continue;
					}
					String name = entry.getName();
					if(!addedResources.contains(name)) {
						addedResources.add(name);
						jarOut.putNextEntry(new ZipEntry(name));
						copy(zipIn, jarOut);
						jarOut.closeEntry();
					}
				}
			}
		}
	}

	static void copy(InputStream source, OutputStream target) throws IOException {
		byte[] buf = new byte[8192];
		int length;
		while ((length = source.read(buf)) > 0) {
			target.write(buf, 0, length);
		}
	}

    public boolean canAssign(ClassNode superType, ClassNode subType) {
        final String name = superType.name;
        if ((superType.access & Opcodes.ACC_INTERFACE) != 0) {
            return isImplementingInterface(subType, name);
        } else {
            while (subType != null) {
                if (name.equals(subType.name) || name.equals(subType.superName)) {
                    return true;
                }
                if (subType.name.equals("java/lang/Object")) {
                    return false;
                }
                subType = getClass(subType.superName);
            }
        }
        return false;
    }

    public boolean isImplementingInterface(ClassNode clazz, String itf) {
        if (clazz.name.equals("java/lang/Object")) {
            return false;
        }
        ClassNode superClass = getClass(clazz.superName);
        for (String interfaceName : superClass.interfaces) {
            if (interfaceName.equals(itf)) {
                return true;
            } else {
                if (isImplementingInterface(getClass(interfaceName), itf)) {
                    return true;
                }
            }
        }
        if ((clazz.access & Opcodes.ACC_INTERFACE) != 0) {
            return false;
        }
        return isImplementingInterface(getClass(clazz.superName), itf);
    }

    public ClassNode getCommonSuperClass(ClassNode class1, ClassNode class2) {
        if (class1.name.equals("java/lang/Object")) {
            return class1;
        }
        if (class2.name.equals("java/lang/Object")) {
            return class2;
        }
        if (canAssign(class1, class2)) {
            return class1;
        }
        if (canAssign(class2, class1)) {
            return class2;
        }
        if ((class1.access & Opcodes.ACC_INTERFACE) != 0 || (class2.access & Opcodes.ACC_INTERFACE) != 0) {
            return getClass("java/lang/Object");
        }
        return getCommonSuperClass(class1, getClass(class2.superName));
    }
	
	protected ClassNode getClass(String className) {
		ClassNode storageNode = storage.getClass(className);
		if(storageNode != null) {
			return storageNode;
		}
		storageNode = cache.get(className);
		if(storageNode != null) {
			return storageNode;
		}
        String resource = className + ".class";
        InputStream is = loader.getResourceAsStream(resource);
        if (is == null) {
        	return null;
        }
        try {
	        try {
	        	ClassReader reader = new ClassReader(is);
	        	ClassNode node = new ClassNode();
	        	reader.accept(node, 0);
	        	cache.put(className, node);
	        	return node;
	        	
	        } finally {
				is.close();
	        }
        }
        catch(IOException e) {
        	return null;
        }
	}
	
	private class ClassWriter extends org.objectweb.asm.ClassWriter {
		public ClassWriter(int flags) {
			super(flags);
		}
		
		protected String getCommonSuperClass(final String type1, final String type2) {
			ClassNode class1 = ClassStorageWriter.this.getClass(type1);
			ClassNode class2 = ClassStorageWriter.this.getClass(type2);
			if(class1 == null || class2 == null) {
				return "java/lang/Object";
			}
			return ClassStorageWriter.this.getCommonSuperClass(class1, class2).name;
		}
	}
}
