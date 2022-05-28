package org.mcphackers.rdi.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.mcphackers.rdi.injector.Injector;
import org.mcphackers.rdi.injector.RDInjector;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class FileUtil {
    public static void write(RDInjector wrapper, OutputStream out) throws IOException {
        JarOutputStream jarOut = new JarOutputStream(out);
        for (Map.Entry<String, ClassNode> classNodeEntry : wrapper.getIndexedNodes().entrySet()) {
            ClassWriter writer = new ClassWriter(0);
            classNodeEntry.getValue().accept(writer);
            jarOut.putNextEntry(new ZipEntry(classNodeEntry.getValue().name + ".class"));
            jarOut.write(writer.toByteArray());
            jarOut.closeEntry();
        }
        jarOut.close();
    }

    /**
     * Write all class nodes and copy over all resources from a given jar.
     * This method throws an IOException if there is no file at the path "resources"
     * or if it is not a zip (and by extension jar) file.
     * If no resources need to be copied over, {@link FileUtil#write(OutputStream)} should be used instead.
     *
     * @param out The stream to write the nodes and resources to as a jar
     * @param resources The path to obtain resources from
     * @throws IOException If something went wrong while writing to the stream or reading the resources jar.
     */
    public static void write(Injector injector, OutputStream out, Path resources) throws IOException {
        if (Files.notExists(resources)) {
            throw new IOException("The path (" + resources.toString() + ") specified by \"resources\" does not exist.");
        }
        JarOutputStream jarOut = new JarOutputStream(out);
        for (ClassNode classNode : injector.getClasses()) {
            ClassWriter writer = new ClassWriter(0);
            classNode.accept(writer);
            jarOut.putNextEntry(new ZipEntry(classNode.name + ".class"));
            jarOut.write(writer.toByteArray());
            jarOut.closeEntry();
        }
        try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(resources))) {
            for (ZipEntry entry = zipIn.getNextEntry(); entry != null; entry = zipIn.getNextEntry()) {
                if (entry.getName().endsWith(".class")) {
                    int ch1 = zipIn.read();
                    int ch2 = zipIn.read();
                    int ch3 = zipIn.read();
                    int ch4 = zipIn.read();
                    if ((ch1 | ch2 | ch3 | ch4) < 0) {
                        // This might lead to duplicate .class files, but the chance of this happening is relatively low
                        // so this behaviour will be ignored
                        jarOut.putNextEntry(entry);
                        if (ch1 == -1) {
                            continue;
                        }
                        jarOut.write(ch1);
                        if (ch2 == -1) {
                            continue;
                        }
                        jarOut.write(ch2);
                        if (ch3 == -1) {
                            continue;
                        }
                        jarOut.write(ch3);
                        if (ch4 == -1) {
                            continue;
                        } else {
                            throw new IOException(String.format("Unexpected header: [%d, %d, %d, %d]", ch1, ch2, ch3, ch4));
                        }
                    }
                    if (ch1 == 0xCA && ch2 == 0xFE && ch3 == 0xBA && ch4 == 0xBE) {
                        // every valid class file must begin with CAFEBABE
                        continue;
                    }
                }
                jarOut.putNextEntry(entry);
                copy(zipIn, jarOut);
            }
        }
        jarOut.close();
    }

    static void copy(InputStream source, OutputStream target) throws IOException {
        byte[] buf = new byte[8192];
        int length;
        while ((length = source.read(buf)) > 0) {
            target.write(buf, 0, length);
        }
    }
}
