package org.mcphackers.rdi.injector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.mcphackers.rdi.injector.visitors.AccessFixer;
import org.mcphackers.rdi.injector.visitors.AddExceptions;
import org.mcphackers.rdi.injector.visitors.ClassVisitor;
import org.mcphackers.rdi.injector.visitors.FixParameterLVT;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

public class RDInjector implements Injector {
	
	public RDInjector(Path path) {
		indexJar(path, this);
	}

    private final Map<String, ClassNode> indexedNodes = new HashMap<>();
    
    private boolean fixInnerClasses = false;
    private ClassVisitor visitorStack;

    public Map<String, ClassNode> getIndexedNodes() {
        return indexedNodes;
    }

    public void setIndexedNodes(Map<String, ClassNode> indexedNodes) {
        this.indexedNodes.clear();
        this.indexedNodes.putAll(indexedNodes);
    }

    public void setClasses(List<ClassNode> nodes) {
    	Map<String, ClassNode> indexedNodes = new HashMap<>();
    	for(ClassNode node : nodes) {
    		indexedNodes.put(node.name, node);
    	}
    	setIndexedNodes(indexedNodes);
    }
    
    public ClassNode getClass(String className) {
    	return indexedNodes.get(className);
    }

    public List<ClassNode> getClasses() {
        List<ClassNode> classes = new ArrayList<>();
        for (Map.Entry<String, ClassNode> nodeEntry : indexedNodes.entrySet()) {
            classes.add(nodeEntry.getValue());
        }
        return classes;
    }

	public void transform() {
		if(fixInnerClasses) doFixInnerClasses();
		for(ClassNode node : getClasses()) {
			visitorStack.visit(node);
		}
	}
    
    public RDInjector fixInnerClasses() {
    	fixInnerClasses = true;
    	return this;
    }
    
    public RDInjector fixParameterLVT() {
    	visitorStack = new FixParameterLVT(visitorStack);
		return this;
    }
    
    public RDInjector fixExceptions(Path path) {
    	Exceptions exceptions = new Exceptions(path);
    	visitorStack = new AddExceptions(exceptions, visitorStack);
		return this;
    }
    
    public RDInjector fixAccess(Path path) {
    	Access access = new Access(path);
    	visitorStack = new AccessFixer(access, visitorStack);
		return this;
    }

    /**
     * Guesses the inner classes from class nodes
     */
    private void doFixInnerClasses() {
        Map<String, InnerClassNode> splitInner = new HashMap<>();
        Set<String> enums = new HashSet<>();
        Map<String, List<InnerClassNode>> parents = new HashMap<>();

        // Initial indexing sweep
        for (ClassNode node : this.getClasses()) {
            parents.put(node.name, new ArrayList<>());
            if (node.superName.equals("java/lang/Enum")) {
                enums.add(node.name); // Register enum
            }
        }
        // Second sweep
        for (ClassNode node : this.getClasses()) {
            // Sweep enum members
            if (enums.contains(node.superName)) {
                // Child of (abstract) enum
                boolean skip = false;
                for (InnerClassNode innerNode : node.innerClasses) {
                    if (node.name.equals(innerNode.name)) {
                        skip = true;
                        break;
                    }
                }
                if (!skip) {
                    // Apply fixup
                    // We are using 16400 for access, but are there times where this is not wanted?
                    // 16400 = ACC_FINAL | ACC_ENUM
                    InnerClassNode innerNode = new InnerClassNode(node.name, null, null, 16400);
                    parents.get(node.superName).add(innerNode);
                    node.outerClass = node.superName;
                    node.innerClasses.add(innerNode);
                }
            } else if (node.name.contains("$")) {
                // Partially unobfuscated inner class.

                // This operation cannot be performed during the first sweep
                boolean skip = false;
                for (InnerClassNode innernode : node.innerClasses) {
                    if (innernode.name.equals(node.name)) {
                        skip = true;
                        break;
                    }
                }
                if (!skip) {
                    int lastSeperator = node.name.lastIndexOf('$');
                    String outerNode = node.name.substring(0, lastSeperator++);
                    String innerMost = node.name.substring(lastSeperator);
                    InnerClassNode innerClassNode;
                    if (innerMost.matches("^\\d+$")) {
                        // Anonymous class
                        // We know that ACC_SUPER is invalid for inner classes, so we remove that flag
                        innerClassNode = new InnerClassNode(node.name, null, null, node.access & ~Opcodes.ACC_SUPER);
                        node.outerClass = outerNode;
                    } else {
                        // We need to check for static inner classes.
                        // We already know that anonymous classes can never be static classes by definition,
                        // So we can skip that step for anonymous classes
                        boolean staticInnerClass = false;
                        boolean implicitStatic = false;
                        // Interfaces, Enums and Records are implicitly static
                        if (!staticInnerClass) {
                            staticInnerClass = (node.access & Opcodes.ACC_INTERFACE) != 0
                                    || (node.access & Opcodes.ACC_RECORD) != 0
                                    || ((node.access & Opcodes.ACC_ENUM) != 0 && node.superName.equals("java/lang/Enum"));
                            implicitStatic = staticInnerClass;
                        }
                        // Member classes of interfaces are implicitly static
                        if (!staticInnerClass) {
                            ClassNode outerClassNode = indexedNodes.get(outerNode);
                            staticInnerClass = outerClassNode != null && (outerClassNode.access & Opcodes.ACC_INTERFACE) != 0;
                            implicitStatic = staticInnerClass;
                        }
                        // The constructor of non-static inner classes must take in an instance of the outer class an
                        // argument
                        if (!staticInnerClass && outerNode != null) {
                            boolean staticConstructor = false;
                            for (MethodNode method : node.methods) {
                                if (method.name.equals("<init>")) {
                                    int outernodeLen = outerNode.length();
                                    if (outernodeLen + 2 > method.desc.length()) {
                                        // The reference to the outer class cannot be passed in via a parameter as there
                                        // i no space for it in the descriptor, so the class has to be static
                                        staticConstructor = true;
                                        break;
                                    }
                                    String arg = method.desc.substring(2, outernodeLen + 2);
                                    if (!arg.equals(outerNode)) {
                                        // Has to be static. The other parameters are irrelevant as the outer class
                                        // reference is always at first place.
                                        staticConstructor = true;
                                        break;
                                    }
                                }
                            }
                            if (staticConstructor) {
                                staticInnerClass = true;
                                implicitStatic = false;
                            }
                        }
                        if (staticInnerClass && !implicitStatic) {
                            for (FieldNode field : node.fields) {
                                if ((field.access & Opcodes.ACC_FINAL) != 0 && field.name.startsWith("this$")) {
                                    System.err.println("Falsely identified " + node.name + " as static inner class.");
                                    staticInnerClass = false;
                                }
                            }
                        }

                        int innerClassAccess = node.access & ~Opcodes.ACC_SUPER; // Super is not allowed for inner class nodes

                        // Don't fall to the temptation of adding ACC_STATIC to the class node.
                        // According the the ASM verifier it is not legal to do so. However the JVM does not seem care
                        // Nonetheless, we are not adding it the access flags of the class, though we will add it in the inner
                        // class node
                        if (!staticInnerClass) {
                            // Beware of https://docs.oracle.com/javase/specs/jls/se16/html/jls-8.html#jls-8.1.3
                            node.outerClass = outerNode;
                        } else {
                            innerClassAccess |= Opcodes.ACC_STATIC;
                        }
                        innerClassNode = new InnerClassNode(node.name, outerNode, innerMost, innerClassAccess);
                    }
                    parents.get(outerNode).add(innerClassNode);
                    splitInner.put(node.name, innerClassNode);
                    node.innerClasses.add(innerClassNode);
                }
            }
        }
        for (ClassNode node : this.getClasses()) {
            // General sweep
            Collection<InnerClassNode> innerNodesToAdd = new ArrayList<>();
            for (FieldNode field : node.fields) {
                String descriptor = field.desc;
                if (descriptor.length() < 4) {
                    continue; // Most likely a primitive
                }
                if (descriptor.charAt(0) == '[') {
                    // Array
                    descriptor = descriptor.substring(2, descriptor.length() - 1);
                } else {
                    // Non-array
                    descriptor = descriptor.substring(1, descriptor.length() - 1);
                }
                InnerClassNode innerNode = splitInner.get(descriptor);
                if (innerNode != null) {
                    if (innerNode.innerName == null && !field.name.startsWith("this$")) {
                        // Not fatal, but worrying
                        System.err.printf("Unlikely field descriptor for field \"%s\" with descriptor %s in class %s%n", field.name, field.desc, node.name);
                    }
                    innerNodesToAdd.add(innerNode);
                }
            }
            // Apply inner nodes
            HashSet<String> entryNames = new HashSet<>();
            for (InnerClassNode inner : innerNodesToAdd) {
                if (entryNames.add(inner.name)) {
                    node.innerClasses.add(inner);
                }
            }
        }
        // Add inner classes to the parent of the anonymous classes
        for (Map.Entry<String, List<InnerClassNode>> entry : parents.entrySet()) {
            // Remove duplicates
            HashSet<String> entryNames = new HashSet<>();
            ArrayList<InnerClassNode> toRemove = new ArrayList<>();
            for (InnerClassNode inner : entry.getValue()) {
                if (!entryNames.add(inner.name)) {
                    toRemove.add(inner);
                }
            }
            toRemove.forEach(entry.getValue()::remove);
            ClassNode node = indexedNodes.get(entry.getKey());
            for (InnerClassNode innerEntry : entry.getValue()) {
                boolean skip = false;
                for (InnerClassNode inner : node.innerClasses) {
                    if (inner.name.equals(innerEntry.name)) {
                        skip = true;
                        break;
                    }
                }
                if (!skip) {
                    node.innerClasses.add(innerEntry);
                }
            }
        }
    }
	
    public static void indexJar(Path path, Injector injector) {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(path))) {
        	List<ClassNode> nodes = new ArrayList<>();
        	ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.getName().endsWith(".class")) {
                    ClassReader classReader = new ClassReader(zipInputStream);
                    ClassNode classNode = new ClassNode();
                    classReader.accept(classNode, 0);
                    nodes.add(classNode);
                }
            }
            injector.setClasses(nodes);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
