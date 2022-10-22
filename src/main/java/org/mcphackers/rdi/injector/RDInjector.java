package org.mcphackers.rdi.injector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.mcphackers.rdi.injector.data.Access;
import org.mcphackers.rdi.injector.data.ClassStorage;
import org.mcphackers.rdi.injector.data.Exceptions;
import org.mcphackers.rdi.injector.data.Generics;
import org.mcphackers.rdi.injector.data.Mappings;
import org.mcphackers.rdi.injector.remapper.Remapper;
import org.mcphackers.rdi.injector.transform.AddGenerics;
import org.mcphackers.rdi.injector.transform.GuessGenerics;
import org.mcphackers.rdi.injector.transform.Injection;
import org.mcphackers.rdi.injector.transform.Transform;
import org.mcphackers.rdi.injector.visitors.AccessFixer;
import org.mcphackers.rdi.injector.visitors.AddExceptions;
import org.mcphackers.rdi.injector.visitors.ClassInitAdder;
import org.mcphackers.rdi.injector.visitors.ClassVisitor;
import org.mcphackers.rdi.injector.visitors.FixParameterLVT;
import org.mcphackers.rdi.util.IOUtil;
import org.objectweb.asm.tree.ClassNode;

public class RDInjector implements Injector {

	private List<Path> resourcesPath = new ArrayList<>();
	private List<Injection> globalTransform = new ArrayList<>();
	private ClassVisitor visitorStack;
	private ClassStorage storage;
	
	public RDInjector() {
	}
	
	public RDInjector(Path path) {
		setStorage(new ClassStorage(IOUtil.read(path)));
		addResources(path);
	}
	public RDInjector(List<ClassNode> nodes) {
		setStorage(new ClassStorage(nodes));
	}
	public RDInjector(ClassStorage storage) {
		setStorage(storage);
	}

	@Override
	public void setStorage(ClassStorage storage) {
		this.storage = storage;
	}
	
	public void addResources(Path path) {
		resourcesPath.add(path);
	}
	
	@Override
	public ClassStorage getStorage() {
		return storage;
	}
	
	public void write(Path path) throws IOException {
		IOUtil.write(getStorage(), Files.newOutputStream(path), resourcesPath);
	}

	public void transform() {
		for(Injection transform : globalTransform) {
			transform.transform();
		}
		if(visitorStack != null) {
			visitorStack.visit(storage.getClasses());
		}
		globalTransform.clear();
		visitorStack = null;
	}
	
	public void addVisitor(ClassVisitor visitor) {
		visitorStack = ClassVisitor.merge(visitorStack, visitor);
	}
	
	public RDInjector applyMappings(Mappings mappings) {
		globalTransform.add(() -> new Remapper().load(storage).load(mappings).process());
		return this;
	}
	
	public RDInjector applyMappings(Path path, String srcNamespace, String targetNamespace) {
		Mappings mappings = Mappings.read(path, srcNamespace, targetNamespace);
		return applyMappings(mappings);
	}
	
	public RDInjector applyMappings(Path path, int srcNamespace, int targetNamespace) {
		Mappings mappings = Mappings.read(path, srcNamespace, targetNamespace);
		return applyMappings(mappings);
	}
	
	public RDInjector fixInnerClasses() {
		globalTransform.add(() -> Transform.fixInnerClasses(storage));
		return this;
	}
	
	public RDInjector restoreSourceFile() {
		globalTransform.add(() -> Transform.restoreSourceFile(storage));
		return this;
	}
	
	public RDInjector fixAccess() {
		globalTransform.add(() -> Transform.fixAccess(storage));
		return this;
	}
	
	public RDInjector setMajorVersion(int version) {
		globalTransform.add(() -> Transform.setMajorVersion(storage, version));
		return this;
	}
	
	public RDInjector stripLVT() {
		globalTransform.add(() -> Transform.stripLVT(storage));
		return this;
	}
	
	@Deprecated
	public RDInjector fixSwitchMaps() {
		globalTransform.add(() -> Transform.fixSwitchMaps(storage));
		return this;
	}
	
	public RDInjector guessAnonymousInnerClasses() {
		globalTransform.add(() -> Transform.guessAnonymousInnerClasses(storage));
		return this;
	}
	
	public RDInjector mergeWith(Path path) {
		ClassStorage storage2 = new ClassStorage(IOUtil.read(path));
		globalTransform.add(() -> Transform.merge(storage, storage2));
		return this;
	}
	
	public RDInjector mergeWith(ClassStorage storage2) {
		globalTransform.add(() -> Transform.merge(storage, storage2));
		return this;
	}
	
	public RDInjector addGenerics(Path path) {
		Generics generics = new Generics(path);
		globalTransform.add(new AddGenerics(storage, generics));
		return this;
	}
	
	public RDInjector guessGenerics() {
		globalTransform.add(new GuessGenerics(storage));
		return this;
	}
	
	public RDInjector fixParameterLVT() {
		visitorStack = new FixParameterLVT(visitorStack);
		return this;
	}
	
	public RDInjector fixImplicitConstructors() {
		visitorStack = new ClassInitAdder(storage, visitorStack);
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
}
