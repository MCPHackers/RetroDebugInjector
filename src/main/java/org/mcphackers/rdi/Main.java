package org.mcphackers.rdi;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.mcphackers.rdi.injector.RDInjector;
import org.mcphackers.rdi.injector.transform.Transform;
import org.objectweb.asm.ClassWriter;

public class Main {
	public static final String VERSION = "v1.0";

	public static void main(String[] args) {
		if (args.length == 0) {
			showHelp();
		} else {
			execute(args);
		}
	}

	private static void showHelp() {
		System.out.println("RetroDebugInjector (RDI) " + VERSION);
		System.out.println("RDI is a tool developed by MCPHackers to re-add debugging information to a JAR based on class analysis.");
		System.out.println("------------------------------------ Available parameters ------------------------------------");
		System.out.println("--jar [jarfile]                          ---- Sets the JAR file used as input for debugging information");
		System.out.println("--outputjar [jarfile]                    ---- Sets the JAR file used as output");
		System.out.println("--remap [file] [namespace1] [namespace2] ---- Apply mappings in Tiny format");
		System.out.println("--exc [file]                             ---- Add exceptions in Exceptor format");
		System.out.println("--striplvt                               ---- Removes all local variable tables from methods");
		System.out.println("--fixinner                               ---- Re-attaches inner classes with their owner");
		System.out.println("--restoresource                          ---- Changes sourceFile property of all classes to their name");
		System.out.println("--guessgenerics                          ---- Guesses generics based on bridges (Experimental)");
	}

	private static void execute(String[] args) {
		Instant startTime = Instant.now();
		Path inputJarFile = null;
		Path outputJarFile = null;
		List<String> params = new ArrayList<>();
		Path mappings = null;
		String sourceNamespace = null;
		String targetNamespace = null;
		Path exceptions = null;
		for (int i = 0; i < args.length; i++) {
			String value = args[i];
			if(!value.startsWith("--")) {
				continue;
			}
			String arg = value.substring(2).toLowerCase();
			switch(arg) {
			case "jar":
				if(i + 1 < args.length)
					inputJarFile = Paths.get(args[i + 1]);
				break;
			case "outputjar":
				if(i + 1 < args.length)
					outputJarFile = Paths.get(args[i + 1]);
				break;
			case "remap":
				if(i + 3 < args.length) {
					mappings = Paths.get(args[i + 1]);
					sourceNamespace = args[i + 2];
					targetNamespace = args[i + 3];
				}
				break;
			case "exc":
				if(i + 1 < args.length)
					exceptions = Paths.get(args[i + 1]);
				break;
			default:
				params.add(arg);
				break;
			}
		}

		if (inputJarFile == null || outputJarFile == null) return;

		// Transform classes
		RDInjector injector = new RDInjector(inputJarFile);
		if(params.contains("striplvt")) {
			injector.stripLVT();
		}
		if(mappings != null) {
			injector.applyMappings(mappings, sourceNamespace, targetNamespace);
		}
		if(exceptions != null) {
			injector.fixExceptions(exceptions);
		}
		if(params.contains("fixinner")) {
			injector.fixInnerClasses();
		}
		if(params.contains("guessgenerics")) {
			injector.guessGenerics();
		}
		if(params.contains("restoresource")) {
			injector.restoreSourceFile();
		}
		injector.addTransform(storage -> Transform.decomposeVars(storage));
		injector.addTransform(storage -> Transform.test(storage));
		injector.transform();

		// Export classes
		try {
			injector.write(outputJarFile, ClassWriter.COMPUTE_MAXS);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		Instant endTime = Instant.now();
		System.out.println("Adding debug information took: " + Duration.between(startTime, endTime).get(ChronoUnit.NANOS) / 1E+9 + "s");
	}
}
