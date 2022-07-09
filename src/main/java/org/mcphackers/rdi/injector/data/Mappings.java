package org.mcphackers.rdi.injector.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.mcphackers.rdi.injector.remapper.FieldRenameMap;
import org.mcphackers.rdi.injector.remapper.MethodRenameMap;

import net.fabricmc.mappingio.format.Tiny1Reader;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.FieldMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodMapping;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public class Mappings {
    public final FieldRenameMap fields = new FieldRenameMap();
    public final MethodRenameMap methods = new MethodRenameMap();
    public final Map<String, String> classes = new HashMap<>();
	
	public Mappings() {
	}
	
	public String getPackageName(String pkg) {
		return pkg;
	}

	public static Mappings read(Path path, String srcNamespace, String targetNamespace) {
		Mappings mappings = new Mappings();
		try(BufferedReader br = Files.newBufferedReader(path)) {
			MemoryMappingTree mappingTree = new MemoryMappingTree();
			Tiny1Reader.read(br, mappingTree);
			for(ClassMapping classMapping : mappingTree.getClasses()) {
				String className = classMapping.getName(srcNamespace);
				if(className == null) {
					className = classMapping.getSrcName();
				}
				else {
					mappings.classes.put(classMapping.getName(srcNamespace), classMapping.getName(targetNamespace));
				}
				
				for(FieldMapping fieldMapping : classMapping.getFields()) {
					if(fieldMapping.getName(srcNamespace) == null) {
						continue;
					}
					mappings.fields.put(
							className,
							fieldMapping.getDesc(srcNamespace),
							fieldMapping.getName(srcNamespace),
							fieldMapping.getName(targetNamespace));
				}
				
				for(MethodMapping methodMapping : classMapping.getMethods()) {
					if(methodMapping.getName(srcNamespace) == null) {
						continue;
					}
					mappings.methods.put(
							className,
							methodMapping.getDesc(srcNamespace),
							methodMapping.getName(srcNamespace),
							methodMapping.getName(targetNamespace));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return mappings;
	}
}
