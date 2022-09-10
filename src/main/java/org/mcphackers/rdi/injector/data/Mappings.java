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
import net.fabricmc.mappingio.format.Tiny2Reader;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.FieldMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodMapping;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public class Mappings {
	public final FieldRenameMap fields = new FieldRenameMap();
	public final MethodRenameMap methods = new MethodRenameMap();
	public final Map<String, String> classes = new HashMap<>();

	public static Mappings read(Path path, String srcNamespace, String targetNamespace) {
		Mappings mappings = new Mappings();
		Provider provider = getProvider(path);
		if(provider == null) {
			return mappings;
		}
		try(BufferedReader br = Files.newBufferedReader(path)) {
			MemoryMappingTree mappingTree = new MemoryMappingTree();
			switch (provider) {
			case TINY1:
				Tiny1Reader.read(br, mappingTree);
				break;
			case TINY2:
				Tiny2Reader.read(br, mappingTree);
				break;
			}
			for(ClassMapping classMapping : mappingTree.getClasses()) {
				String className = classMapping.getName(srcNamespace);
				if(className == null) {
					className = classMapping.getSrcName();
				}
				else if (classMapping.getName(targetNamespace) != null) {
					mappings.classes.put(classMapping.getName(srcNamespace), classMapping.getName(targetNamespace));
				}
				
				for(FieldMapping fieldMapping : classMapping.getFields()) {
					if(fieldMapping.getName(srcNamespace) == null || fieldMapping.getName(targetNamespace) == null) {
						continue;
					}
					mappings.fields.put(
							className,
							fieldMapping.getDesc(srcNamespace),
							fieldMapping.getName(srcNamespace),
							fieldMapping.getName(targetNamespace));
				}
				
				for(MethodMapping methodMapping : classMapping.getMethods()) {
					if(methodMapping.getName(srcNamespace) == null || methodMapping.getName(targetNamespace) == null) {
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
	
	public static Provider getProvider(Path mappings) {
		try(BufferedReader reader = Files.newBufferedReader(mappings)) {
			String header = reader.readLine();
			if(header != null) {
				if(header.startsWith("tiny\t2\t0\t")) {
					return Provider.TINY2;
				}
				else if(header.startsWith("v1\t")) {
					return Provider.TINY1;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public enum Provider {
		TINY1,
		TINY2;
	}
}
