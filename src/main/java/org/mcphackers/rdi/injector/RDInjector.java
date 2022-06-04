package org.mcphackers.rdi.injector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.mcphackers.rdi.injector.transform.AddGenerics;
import org.mcphackers.rdi.injector.transform.FixBridges;
import org.mcphackers.rdi.injector.transform.Injection;
import org.mcphackers.rdi.injector.transform.Transform;
import org.mcphackers.rdi.injector.visitors.AccessFixer;
import org.mcphackers.rdi.injector.visitors.AddExceptions;
import org.mcphackers.rdi.injector.visitors.ClassInitAdder;
import org.mcphackers.rdi.injector.visitors.ClassVisitor;
import org.mcphackers.rdi.injector.visitors.FixParameterLVT;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public class RDInjector implements Injector {
    
	public RDInjector(Path path) {
		indexJar(path, this);
	}
	public RDInjector(List<ClassNode> nodes) {
		setClasses(nodes);
	}

    private final Map<String, ClassNode> indexedNodes = new HashMap<>();

    private List<Injection> globalTransform = new ArrayList<>();
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

    public void addClass(ClassNode node) {
		indexedNodes.put(node.name, node);
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
		for(Injection transform : globalTransform) {
			transform.transform();
		}
		visitorStack.visit(getClasses());
	}
    
    public RDInjector fixInnerClasses() {
    	globalTransform.add(() -> Transform.fixInnerClasses(this));
    	return this;
    }
    
    public RDInjector fixSwitchMaps() {
    	globalTransform.add(() -> Transform.fixSwitchMaps(this));
    	return this;
    }
    
    public RDInjector guessAnonymousInnerClasses() {
    	globalTransform.add(() -> Transform.guessAnonymousInnerClasses(this));
    	return this;
    }
    
    public RDInjector addGenerics(Path path) {
    	Generics generics = new Generics(path);
    	globalTransform.add(new AddGenerics(this, generics));
		return this;
    }
    
    public RDInjector fixBridges() {
    	globalTransform.add(new FixBridges(this));
		return this;
    }
    
    public RDInjector fixParameterLVT() {
    	visitorStack = new FixParameterLVT(visitorStack);
		return this;
    }
    
    public RDInjector fixImplicitConstructors() {
    	visitorStack = new ClassInitAdder(this, visitorStack);
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
