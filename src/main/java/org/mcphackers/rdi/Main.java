package org.mcphackers.rdi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.mcphackers.rdi.injector.Injector;
import org.mcphackers.rdi.injector.RDInjector;
import org.mcphackers.rdi.util.FileUtil;

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
        System.out.println("---- Available parameters ----");
        System.out.println("--jar [jarfile] ---- Sets the JAR file used as input for debugging information");
        System.out.println("--outputjar [jarfile] ---- Sets the JAR file used as output");
    }

    private static void execute(String[] args) {
        Instant startTime = Instant.now();
        Path inputJarFile = null;
        Path outputJarFile = null;
        for (int i = 0; i < args.length; i++) {
            String value = args[i];
            if (value.equals("--jar")) {
                inputJarFile = Paths.get(args[i + 1]);
            } else if (value.equals("--outputjar")) {
                outputJarFile = Paths.get(args[i + 1]);
            }
        }

        if (inputJarFile == null || outputJarFile == null) return;

        // Transform classes
        Injector injector = new RDInjector(inputJarFile)
        		.fixInnerClasses()
        		.fixParameterLVT();
        
        injector.transform();

        // Export classes
        try {
            FileUtil.write(injector, Files.newOutputStream(outputJarFile), inputJarFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Instant endTime = Instant.now();
        System.out.println("Adding debug information took: " + Duration.between(startTime, endTime).get(ChronoUnit.NANOS) / 1E+9 + "s");
    }
}
