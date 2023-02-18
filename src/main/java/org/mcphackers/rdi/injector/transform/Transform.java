package org.mcphackers.rdi.injector.transform;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mcphackers.rdi.injector.data.Access.Level;
import org.mcphackers.rdi.injector.data.ClassStorage;
import org.mcphackers.rdi.injector.data.constants.Constant;
import org.mcphackers.rdi.injector.data.constants.ConstantPool;
import org.mcphackers.rdi.util.DescString;
import org.mcphackers.rdi.util.FieldReference;
import org.mcphackers.rdi.util.InsnHelper;
import org.mcphackers.rdi.util.MethodReference;
import org.mcphackers.rdi.util.OPHelper;
import org.mcphackers.rdi.util.Pair;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Class with global transformations to the list of classes
 */
public final class Transform {

	public static final int VISIBILITY_MODIFIERS = Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED | Opcodes.ACC_PUBLIC;
	
	/**
	 * Guesses the inner classes from class nodes
	 */
	public static void fixInnerClasses(ClassStorage storage) {
		Map<String, InnerClassNode> splitInner = new HashMap<>();
		Set<String> enums = new HashSet<>();
		Map<String, List<InnerClassNode>> parents = new HashMap<>();

		// Initial indexing sweep
		for (ClassNode node : storage) {
			parents.put(node.name, new ArrayList<>());
			if (node.superName.equals("java/lang/Enum")) {
				enums.add(node.name); // Register enum
			}
		}
		// Second sweep
		for (ClassNode node : storage) {
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
							ClassNode outerClassNode = storage.getClass(outerNode);
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
					List<InnerClassNode> innerNodes = parents.get(outerNode);
					if(innerNodes != null) {
						innerNodes.add(innerClassNode);
					}
					splitInner.put(node.name, innerClassNode);
					node.innerClasses.add(innerClassNode);
				}
			}
		}
		for (ClassNode node : storage) {
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
			ClassNode node = storage.getClass(entry.getKey());
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
	
	
	/**
	 * Method that tries to restore the SwitchMaps to how they should be.
	 * This includes marking the switchmap classes as anonymous classes, so they may not be referenceable
	 * afterwards.
	 *
	 * @return The amount of classes who were identified as switch maps.
	 */
	public static int fixSwitchMaps(ClassStorage storage) {
		Map<FieldReference, String> deobfNames = new HashMap<FieldReference, String>(); // The deobf name will be something like $SwitchMap$org$bukkit$Material

		// index switch map classes - or at least their candidates
		for (ClassNode node : storage) {
			if (node.superName != null && node.superName.equals("java/lang/Object") && node.interfaces.isEmpty()) {
				if (node.fields.size() == 1 && node.methods.size() == 1) {
					MethodNode method = node.methods.get(0);
					FieldNode field = node.fields.get(0);
					if (method.name.equals("<clinit>") && method.desc.equals("()V")
							&& field.desc.equals("[I")
							&& (field.access & Opcodes.ACC_STATIC) != 0) {
						FieldReference fieldRef = new FieldReference(node.name, field);
						String enumName = null;
						AbstractInsnNode instruction = method.instructions.getFirst();
						while (instruction != null) {
							if (instruction instanceof FieldInsnNode && instruction.getOpcode() == Opcodes.GETSTATIC) {
								FieldInsnNode fieldInstruction = (FieldInsnNode) instruction;
								if (fieldRef.equals(new FieldReference(fieldInstruction))) {
									AbstractInsnNode next = instruction.getNext();
									while (next instanceof FrameNode || next instanceof LabelNode) {
										// ASM is sometimes not so nice
										next = next.getNext();
									}
									if (next instanceof FieldInsnNode && next.getOpcode() == Opcodes.GETSTATIC) {
										if (enumName == null) {
											enumName = ((FieldInsnNode) next).owner;
										} else if (!enumName.equals(((FieldInsnNode) next).owner)) {
											enumName = null;
											break; // It may not be a switchmap field
										}
									}
								}
							}
							instruction = instruction.getNext();
						}
						if (enumName != null) {
							if (fieldRef.getName().indexOf('$') == -1) {
								// The deobf name will be something like $SwitchMap$org$bukkit$Material
								String newName = "$SwitchMap$" + enumName.replace('/', '$');
								deobfNames.put(fieldRef, newName);
								instruction = method.instructions.getFirst();
								// Remap references within this class
								while (instruction != null) {
									if (instruction instanceof FieldInsnNode) {
										FieldInsnNode fieldInsn = (FieldInsnNode) instruction;
										if ((fieldInsn.getOpcode() == Opcodes.GETSTATIC || fieldInsn.getOpcode() == Opcodes.PUTSTATIC)
												&& fieldInsn.owner.equals(node.name)
												&& fieldRef.equals(new FieldReference(fieldInsn))) {
											fieldInsn.name = newName;
										}
									}
									instruction = instruction.getNext();
								}
								// Remap the actual field declaration
								// Switch maps can only contain a single field and we have already obtained said field, so it isn't much of a deal here
								field.name = newName;
							}
						}
					}
				}
			}
		}

		// Rename references to the field
		for (ClassNode node : storage) {
			Set<String> addedInnerClassNodes = new HashSet<String>();
			for (MethodNode method : node.methods) {
				AbstractInsnNode instruction = method.instructions.getFirst();
				while (instruction != null) {
					if (instruction instanceof FieldInsnNode && instruction.getOpcode() == Opcodes.GETSTATIC) {
						FieldInsnNode fieldInstruction = (FieldInsnNode) instruction;
						if (fieldInstruction.owner.equals(node.name)) { // I have no real idea what I was doing here
							instruction = instruction.getNext();
							continue;
						}
						FieldReference fRef = new FieldReference(fieldInstruction);
						String newName = deobfNames.get(fRef);
						if (newName != null) {
							fieldInstruction.name = newName;
							if (!addedInnerClassNodes.contains(fRef.getOwner())) {
								InnerClassNode innerClassNode = new InnerClassNode(fRef.getOwner(), node.name, null, Opcodes.ACC_STATIC ^ Opcodes.ACC_SYNTHETIC ^ Opcodes.ACC_FINAL);
								ClassNode outerNode = storage.getClass(fRef.getOwner());
								if (outerNode != null) {
									outerNode.innerClasses.add(innerClassNode);
								}
								ClassNode outermostClassnode = null;
								if (node.outerClass != null) {
									outermostClassnode = storage.getClass(node.outerClass);
								}
								if (outermostClassnode == null) {
									for (InnerClassNode inner : node.innerClasses) {
										if (inner.name.equals(node.name) && inner.outerName != null) {
											outermostClassnode = storage.getClass(inner.outerName);
											break;
										}
									}
								}
								if (outermostClassnode != null) {
									outermostClassnode.innerClasses.add(innerClassNode);
								}
								node.innerClasses.add(innerClassNode);
							}
						}
					}
					instruction = instruction.getNext();
				}
			}
		}

		return deobfNames.size();
	}

	/**
	 * Guesses anonymous inner classes by checking whether they have a synthetic field and if they
	 * do whether they are referenced only by a single "parent" class.
	 * Note: this method is VERY aggressive when it comes to adding inner classes, sometimes it adds
	 * inner classes on stuff where it wouldn't belong. This  means that usage of this method should
	 * be done wisely. This method will do some damage even if it does no good.
	 *
	 * @return The amount of guessed anonymous inner classes
	 */
	public static int guessAnonymousInnerClasses(ClassStorage storage) {

		// Class name -> referenced class, method
		// I am well aware that we are using method node, but given that there can be multiple methods with the same
		// name it is better to use MethodNode instead of String to reduce object allocation overhead.
		// Should we use triple instead? Perhaps.
		HashMap<String, Map.Entry<String, MethodNode>> candidates = new LinkedHashMap<String, Map.Entry<String, MethodNode>>();
		for (ClassNode node : storage) {
			if ((node.access & VISIBILITY_MODIFIERS) != 0) {
				continue; // Anonymous inner classes are always package-private
			}
			boolean skipClass = false;
			FieldNode outerClassReference = null;
			for (FieldNode field : node.fields) {
				final int requiredFlags = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_FINAL;
				if ((field.access & requiredFlags) == requiredFlags
						&& (field.access & VISIBILITY_MODIFIERS) == 0) {
					if (outerClassReference != null) {
						skipClass = true;
						break; // short-circuit
					}
					outerClassReference = field;
				}
			}
			if (skipClass || outerClassReference == null) {
				continue;
			}
			// anonymous classes can only have a single constructor since they are only created at a single spot
			// However they also have to have a constructor so they can pass the outer class reference
			MethodNode constructor = null;
			for (MethodNode method : node.methods) {
				if (method.name.equals("<init>")) {
					if (constructor != null) {
						// cannot have multiple constructors
						skipClass = true;
						break; // short-circuit
					}
					if ((method.access & VISIBILITY_MODIFIERS) != 0) {
						// The constructor should be package - protected
						skipClass = true;
						break;
					}
					constructor = method;
				}
			}
			if (skipClass || constructor == null) { // require a single constructor, not more, not less
				continue;
			}
			// since we have the potential reference to the outer class and we know that it has to be set
			// via the constructor's parameter, we can check whether this is the case here
			DescString desc = new DescString(constructor.desc);
			skipClass = true;
			while (desc.hasNext()) {
				String type = desc.nextType();
				if (type.equals(outerClassReference.desc)) {
					skipClass = false;
					break;
				}
			}
			if (skipClass) {
				continue;
			}
			int dollarIndex = node.name.indexOf('$');
			if (dollarIndex != -1 && !Character.isDigit(node.name.codePointAt(dollarIndex + 1))) {
				// Unobfuscated class that is 100% not anonymous
				continue;
			}
			candidates.put(node.name, null);
		}

		// Make sure that the constructor is only invoked in a single class, which should be the outer class
		for (ClassNode node : storage) {
			for (MethodNode method : node.methods) {
				AbstractInsnNode instruction = method.instructions.getFirst();
				while (instruction != null) {
					if (instruction instanceof MethodInsnNode && ((MethodInsnNode)instruction).name.equals("<init>")) {
						MethodInsnNode methodInvocation = (MethodInsnNode) instruction;
						String owner = methodInvocation.owner;
						if (candidates.containsKey(owner)) {
							if (owner.equals(node.name)) {
								// this is no really valid anonymous class
								candidates.remove(owner);
							} else {
								Map.Entry<String, MethodNode> invoker = candidates.get(owner);
								if (invoker == null) {
									candidates.put(owner, new AbstractMap.SimpleEntry<String, MethodNode>(node.name, method));
								} else if (!invoker.getKey().equals(node.name)
										|| !invoker.getValue().name.equals(method.name)
										|| !invoker.getValue().desc.equals(method.desc)) {
									// constructor referenced by multiple classes, cannot be valid
									// However apparently these classes could be extended? I am not entirely sure how that is possible, but it is.
									// That being said, we are going to ignore that this is possible and just consider them invalid
									// as everytime this happens the decompiler is able to decompile the class without any issues.
									candidates.remove(owner);
								}
							}
						}
					}
					instruction = instruction.getNext();
				}
			}
		}

		// If another class has a field reference to the potential anonymous class, and that field is not
		// synthetic, then the class is likely not anonymous.
		// In the future I could settle with not checking for the anonymous access flag, but this would
		// be quite the effort to get around nonetheless since previous steps of this method utilise
		// this access flag
		for (ClassNode node : storage) {
			for (FieldNode field : node.fields) {
				if (field.desc.length() == 1 || (field.access & Opcodes.ACC_SYNTHETIC) != 0) {
					continue;
				}
				if (field.desc.codePointAt(field.desc.lastIndexOf('[') + 1) != 'L') {
					continue;
				}
				// Now technically, they are still inner classes. Just regular ones and they are not static ones
				// however not adding them as a inner class has no effect in recomplieabillity so we will not really care about it just yet.
				// TODO that being said, we should totally do it
				String className = field.desc.substring(field.desc.lastIndexOf('[') + 2, field.desc.length() - 1);
				candidates.remove(className);
			}
		}

		int addedInners = 0;
		for (Map.Entry<String, Map.Entry<String, MethodNode>> candidate : candidates.entrySet()) {
			String inner = candidate.getKey();
			Map.Entry<String, MethodNode> outer = candidate.getValue();
			if (outer == null) {
				continue;
			}
			ClassNode innerNode = storage.getClass(inner);
			ClassNode outernode = storage.getClass(outer.getKey());

			MethodNode outerMethod = outer.getValue();
			if (outernode == null) {
				continue;
			}
			boolean hasInnerClassInfoInner = false;
			for (InnerClassNode icn : innerNode.innerClasses) {
				if (icn.name.equals(inner)) {
					hasInnerClassInfoInner = true;
					break;
				}
			}
			boolean hasInnerClassInfoOuter = false;
			for (InnerClassNode icn : outernode.innerClasses) {
				if (icn.name.equals(inner)) {
					hasInnerClassInfoOuter = true;
					break;
				}
			}
			if (hasInnerClassInfoInner && hasInnerClassInfoOuter) {
				continue;
			}
			InnerClassNode newInnerClassNode = new InnerClassNode(inner, null, null, 16400);
			if (!hasInnerClassInfoInner) {
				innerNode.outerMethod = outerMethod.name;
				innerNode.outerMethodDesc = outerMethod.desc;
				innerNode.outerClass = outernode.name;
				innerNode.innerClasses.add(newInnerClassNode);
			}
			if (!hasInnerClassInfoOuter) {
				outernode.innerClasses.add(newInnerClassNode);
			}
			addedInners++;
		}

		return addedInners;
	}
	
	public static void fixAccess(ClassStorage storage) {
		Map<String, Level> classAccesses = new HashMap<String, Level>();
		Map<FieldReference, Pair<FieldNode, Level>> fieldAccesses = new HashMap<FieldReference, Pair<FieldNode, Level>>();
		Map<MethodReference, Pair<MethodNode, Level>> methodAccesses = new HashMap<MethodReference, Pair<MethodNode, Level>>();
		
		// Find all invalid access points
		
		for(ClassNode node : storage) {
			for(FieldNode field : node.fields) {
				// Checking field type
				Type type = Type.getType(field.desc);
				if(type.getSort() != Type.OBJECT) {
					continue;
				}
				ClassNode classNode = storage.getClass(type.getInternalName());
				if(classNode == null) {
					continue;
				}
				Level access = Level.getFromBytecode(classNode.access);
				if(access == Level.DEFAULT) {
					if(!ClassStorage.inOnePackage(node.name, classNode.name)) {
						classAccesses.put(classNode.name, Level.PUBLIC);
					}
				}
			}
			for(MethodNode method : node.methods) {
				// Checking method argument and return types
				List<Type> types = new ArrayList<Type>();
				types.addAll(Arrays.asList(Type.getArgumentTypes(method.desc)));
				types.add(Type.getReturnType(method.desc));
				for(Type type : types) {
					if(type.getSort() != Type.OBJECT) {
						continue;
					}
					if(classAccesses.containsKey(type.getInternalName())) {
						continue;
					}
					ClassNode classNode = storage.getClass(type.getInternalName());
					if(classNode == null) {
						continue;
					}
					Level access = Level.getFromBytecode(classNode.access);
					if(access == Level.DEFAULT) {
						if(!ClassStorage.inOnePackage(node.name, type.getInternalName())) {
							classAccesses.put(classNode.name, Level.PUBLIC);
						}
					}
				}
				for(AbstractInsnNode insn : method.instructions) {
					if(insn instanceof MethodInsnNode) {
						MethodInsnNode invoke = (MethodInsnNode)insn;
						MethodReference ref = new MethodReference(invoke);
						MethodNode methodNode = storage.getMethod(ref);
						if(methodNode == null) {
							continue;
						}
						if(!classAccesses.containsKey(invoke.owner)) {
							ClassNode classNode = storage.getClass(invoke.owner);
							if(classNode != null) {
								Level access = Level.getFromBytecode(classNode.access);
								if(access == Level.DEFAULT) {
									if(!ClassStorage.inOnePackage(node.name, invoke.owner)) {
										classAccesses.put(classNode.name, Level.PUBLIC);
									}
								}
							}
						}
						if(methodAccesses.containsKey(ref)) {
							continue;
						}
						Level accessLevel = Level.getFromBytecode(methodNode.access);
						if (accessLevel == Level.PUBLIC) {
							continue;
						}
						if(accessLevel == Level.PRIVATE) {
							if(!node.name.equals(invoke.owner)) {
								methodAccesses.put(ref, Pair.of(methodNode, Level.PUBLIC));
							}
							continue;
						}
						if(accessLevel == Level.DEFAULT || accessLevel == Level.PROTECTED) {
							if(accessLevel == Level.PROTECTED) {
								boolean isSuper = false;
								for(ClassNode clNode = node; clNode != null; clNode = storage.getClass(clNode.superName)) {
									if(clNode.name.equals(invoke.owner)) {
										isSuper = true;
									}
								}
								if(isSuper) continue;
							}
							if(!ClassStorage.inOnePackage(node.name, invoke.owner)) {
								methodAccesses.put(ref, Pair.of(methodNode, Level.PUBLIC));
							}
							continue;
						}
					}

					if(insn instanceof FieldInsnNode) {
						FieldInsnNode field = (FieldInsnNode)insn;
						FieldReference ref = new FieldReference(field);
						FieldNode fieldNode = storage.getField(ref);
						if(fieldNode == null) {
							continue;
						}
						if(!classAccesses.containsKey(field.owner)) {
							ClassNode classNode = storage.getClass(field.owner);
							if(classNode != null) {
								Level access = Level.getFromBytecode(classNode.access);
								if(access == Level.DEFAULT) {
									if(!ClassStorage.inOnePackage(node.name, field.owner)) {
										classAccesses.put(classNode.name, Level.PUBLIC);
									}
								}
							}
						}
						if(fieldAccesses.containsKey(ref)) {
							continue;
						}
						Level accessLevel = Level.getFromBytecode(fieldNode.access);
						if (accessLevel == Level.PUBLIC) {
							continue;
						}
						if(accessLevel == Level.PRIVATE) {
							if(!node.name.equals(field.owner)) {
								fieldAccesses.put(ref, Pair.of(fieldNode, Level.PUBLIC));
							}
							continue;
						}
						if(accessLevel == Level.DEFAULT || accessLevel == Level.PROTECTED) {
							if(accessLevel == Level.PROTECTED) {
								boolean isSuper = false;
								for(ClassNode clNode = node; clNode != null; clNode = storage.getClass(clNode.superName)) {
									if(clNode.name.equals(field.owner)) {
										isSuper = true;
									}
								}
								if(isSuper) continue;
							}
							if(!ClassStorage.inOnePackage(node.name, field.owner)) {
								fieldAccesses.put(ref, Pair.of(fieldNode, Level.PUBLIC));
							}
							continue;
						}
					}
				}
			}
		}
		
		// Apply all access modifiers
		
		for(Entry<String, Level> entry : classAccesses.entrySet()) {
			ClassNode node = storage.getClass(entry.getKey());

			if(node == null) {
				continue;
			}
			Level level = entry.getValue();
			
			node.access = level.setAccess(node.access);
		}

		for(Entry<FieldReference, Pair<FieldNode, Level>> entry : fieldAccesses.entrySet()) {
			Pair<FieldNode, Level> pair = entry.getValue();
			if(pair == null) {
				continue;
			}
			FieldNode node = pair.getLeft();
			Level level = pair.getRight();

			node.access = level.setAccess(node.access);
		}
		
		for(Entry<MethodReference, Pair<MethodNode, Level>> entry : methodAccesses.entrySet()) {
			Pair<MethodNode, Level> pair = entry.getValue();
			if(pair == null) {
				continue;
			}
			MethodNode node = pair.getLeft();
			Level level = pair.getRight();
			
			node.access = level.setAccess(node.access);
		}
	}

	public static void decomposeVars(ClassStorage storage) {
		for(ClassNode node : storage) {
			for(MethodNode method : node.methods) {
				AbstractInsnNode insn = method.instructions.getFirst();
				while(insn != null) {
					int opcode = insn.getOpcode();
					if(opcode == Opcodes.DUP || opcode == Opcodes.DUP2) {
						AbstractInsnNode insn2 = insn.getNext();
						if(insn2 != null && OPHelper.isVarStore(insn2.getOpcode())
								&& ((insn2.getOpcode() == Opcodes.DSTORE || insn2.getOpcode() == Opcodes.LSTORE) && opcode == Opcodes.DUP2
								|| opcode == Opcodes.DUP)) {
							VarInsnNode astore = (VarInsnNode)insn2;
							method.instructions.insert(insn2, new VarInsnNode(OPHelper.reverseVarOpcode(astore.getOpcode()), astore.var));
							method.instructions.remove(insn);
							insn = insn2;
						}
					}
					insn = insn.getNext();
				}
			}
		}
	}
	
	public static void removeVarSelfAssignment(ClassStorage storage) {
		for(ClassNode node : storage) {
			for(MethodNode method : node.methods) {
				AbstractInsnNode insn = method.instructions.getFirst();
				while(insn != null) {
					if(OPHelper.isVarLoad(insn.getOpcode())) {
						AbstractInsnNode insn2 = insn.getNext();
						if(insn2 != null && OPHelper.isVarStore(insn2.getOpcode())) {
							VarInsnNode aload = (VarInsnNode)insn;
							VarInsnNode astore = (VarInsnNode)insn2;
							AbstractInsnNode next = insn2.getNext();
							if(aload.var == astore.var) {
								method.instructions.remove(insn);
								method.instructions.remove(insn2);
								insn = next;
								continue;
							}
						}
					}
					insn = insn.getNext();
				}
			}
		}
	}
	
//	public static void test(ClassStorage storage) {
//		for(ClassNode node : storage) {
//			if(node.name.endsWith("Floor1") || node.name.endsWith("Floor0") || node.name.endsWith("Lookup") || node.name.endsWith("Drft") || node.name.endsWith("Lsp")) {
//				continue;
//			}
//			for(MethodNode method : node.methods) {
//				AbstractInsnNode insn = method.instructions.getFirst();
//				while(insn != null) {
//					if(insn.getOpcode() == Opcodes.LDC) {
//						LdcInsnNode ldc = (LdcInsnNode)insn;
//						if(ldc.cst instanceof Double) {
//							double cstDouble = (Double)ldc.cst;
//							String d = Double.toString(cstDouble);
//							if(d.length() - d.indexOf('.') > 5) {
//								System.out.println(node.name + " " + method.name + method.desc);
//								System.out.println(d +"D");
//							}
//						}
//						else if(ldc.cst instanceof Float) {
//							float cstFloat = (Float)ldc.cst;
//							String f = Float.toString(cstFloat);
//							if(f.length() - f.indexOf('.') > 5) {
//								System.out.println(node.name + " " + method.name + method.desc);
//								System.out.println(f+"F");
//							}
//						}
//					}
//					insn = insn.getNext();
//				}
//			}
//		}
//	}
	
	public static void replaceCommonConstants(ClassStorage storage) {
		for(ClassNode node : storage) {
			for(MethodNode method : node.methods) {
				AbstractInsnNode insn = method.instructions.getFirst();
				nextInsn:
				while(insn != null) {
					if(insn.getOpcode() == Opcodes.LDC || insn.getOpcode() == Opcodes.SIPUSH) {
						AbstractInsnNode next = insn.getNext();
						for(Constant constantReplacer : ConstantPool.CONSTANTS) {
							if(constantReplacer.replace(method.instructions, insn)) {
								insn = next;
								continue nextInsn;
							}
						}
					}
					insn = insn.getNext();
				}
			}
		}
	}
	
	/**
	 * Sets class version for every class in <code>storage</code>
	 * @param storage
	 * @param version
	 */
	public static void setMajorVersion(ClassStorage storage, int version) {
		for(ClassNode node : storage) {
			node.version = version;
		}
	}
	
	/**
	 * Changes sourceFile property of all classes to their name
	 * @param storage
	 */
	public static void restoreSourceFile(ClassStorage storage) {
		for(ClassNode node : storage) {
			int slashIndex = node.name.lastIndexOf('/');
			node.sourceFile = (slashIndex == -1 ? node.name : node.name.substring(slashIndex + 1)) + ".java";
		}
	}
	
	/**
	 * Clears local variable table for every method in each class
	 * @param storage
	 */
	public static void stripLVT(ClassStorage storage) {
		for(ClassNode node : storage) {
			for(MethodNode method : node.methods) {
				method.localVariables = null;
			}
		}
	}

	public static final void fixTryCatchRange(ClassStorage storage) {
		for(ClassNode node : storage) {
			for(MethodNode m : node.methods) {
				List<LabelNode> tryCatchStarts = new ArrayList<LabelNode>();
				for(TryCatchBlockNode tryCatch : m.tryCatchBlocks) {
					tryCatchStarts.add(tryCatch.start);
				}
				List<Pair<Integer, TryCatchBlockNode>> tryBlocks = new ArrayList<Pair<Integer, TryCatchBlockNode>>();
				for(TryCatchBlockNode tryCatch : m.tryCatchBlocks) {
					nextInsn:
					for(AbstractInsnNode insn = tryCatch.start; insn != null && insn != tryCatch.end; insn = insn.getNext()) {
						if(insn.getType() != AbstractInsnNode.JUMP_INSN) {
							continue;
						}
						JumpInsnNode jmp = (JumpInsnNode)insn;
						if(jmp.label == tryCatch.end) {
							continue;
						}
						if(tryCatchStarts.contains(jmp.label)) {
							continue;
						}
						if(OPHelper.isReturn(InsnHelper.nextInsn(jmp.label).getOpcode())) {
							continue;
						}
						for(AbstractInsnNode insn2 = tryCatch.start; insn2 != tryCatch.end; insn2 = insn2.getNext()) {
							if(jmp.label == insn2) {
								continue nextInsn;
							}
						}
						System.out.println("Found a bad try catch in " + node.name + "." + m.name + m.desc + " at index " + m.tryCatchBlocks.indexOf(tryCatch));
						for(AbstractInsnNode insn2 = jmp.label; insn2 != null; insn2 = insn2.getNext()) {
							if(OPHelper.isReturn(insn2.getOpcode()) || insn2.getOpcode() == Opcodes.GOTO) {
								LabelNode end = new LabelNode();
								m.instructions.insert(insn2, end);
								tryBlocks.add(Pair.of(m.tryCatchBlocks.indexOf(tryCatch) + 1, new TryCatchBlockNode(jmp.label, end, tryCatch.handler, tryCatch.type)));
								continue nextInsn;
							}
						}
					}
				}
				for(Pair<Integer, TryCatchBlockNode> pair : tryBlocks) {
					m.tryCatchBlocks.add(pair.left, pair.right);
				}
			}
		}
	}
	
	public static void stripSignatures(ClassStorage storage) {
		for(ClassNode node : storage) {
			node.signature = null;
			for(MethodNode method : node.methods) {
				method.signature = null;
			}
			for(FieldNode field : node.fields) {
				field.signature = null;
			}
		}
	}

	/**
	 * Adds all missing classes, methods and fields from storage2 to storage
	 * @param storage
	 * @param storage2
	 */
	public static void merge(ClassStorage storage, ClassStorage storage2) {
		for(ClassNode node2 : storage2) {
			ClassNode node1 = storage.getClass(node2.name);
			if(node1 == null) {
				storage.addClass(node2);
				continue;
			}
			for(int i = 0; i < node2.interfaces.size(); i++) {
				String itf = node2.interfaces.get(i);
				if(!node1.interfaces.contains(itf)) {
					int index = -1;
					int i2 = i - 1;
					while(i2 >= 0 && index == -1) {
						index = node1.interfaces.indexOf(node2.interfaces.get(i2));
						i2--;
					}
					if(index == -1) {
						node1.interfaces.add(itf);
					} else {
						node1.interfaces.add(index + 1, itf);
					}
				}
			}
			List<FieldReference> fields = new ArrayList<FieldReference>();
			for(FieldNode field : node1.fields) {
				fields.add(new FieldReference(node1.name, field));
			}
			for(int i = 0; i < node2.fields.size(); i++) {
				FieldNode field = node2.fields.get(i);
				FieldReference fieldRef = new FieldReference(node2.name, field);
				if(!fields.contains(fieldRef)) {
					int i2 = i - 1;
					int index = -1;
					FieldReference prevField = null;
					while(i2 >= 0 && index == -1) {
						index = fields.indexOf(prevField);
						prevField = new FieldReference(node2.name, node2.fields.get(i2));
						i2--;
					}
					if(index == -1) {
						node1.fields.add(field);
						fields.add(fieldRef);
					} else {
						node1.fields.add(index + 1, field);
						fields.add(index + 1, fieldRef);
					}
				}
			}
			List<MethodReference> methods = new ArrayList<MethodReference>();
			for(MethodNode method : node1.methods) {
				methods.add(new MethodReference(node1.name, method));
			}
			for(int i = 0; i < node2.methods.size(); i++) {
				MethodNode method = node2.methods.get(i);
				MethodReference methodRef = new MethodReference(node2.name, method);
				if(!methods.contains(methodRef)) {
					int i2 = i - 1;
					int index = -1;
					MethodReference prevMethod = null;
					while(i2 >= 0 && index == -1) {
						index = methods.indexOf(prevMethod);
						prevMethod = new MethodReference(node2.name, node2.methods.get(i2));
						i2--;
					}
					if(index == -1) {
						node1.methods.add(method);
						methods.add(methodRef);
					} else {
						node1.methods.add(index + 1, method);
						methods.add(index + 1, methodRef);
					}
				}
			}
		}
	}
	
}
