package org.mcphackers.rdi.injector.visitors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.mcphackers.rdi.util.DescString;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

public class FixParameterLVT extends ClassVisitor {
	
	public FixParameterLVT(ClassVisitor cv) {
		super(cv);
	}

    /**
     * Method that tries to put the Local Variable Table (LVT) in a acceptable state
     * by synchronising parameter declarations with lvt declarations. Does not do
     * anything to the LVT is the LVT is declared but empty, which is a sign of the
     * usage of obfuscation tools.
     * It is intended to be used in combination with decompilers such as quiltflower
     * but might not be useful for less naive decompilers such as procyon, which do not decompile
     * into incoherent java code if the LVT is damaged.
     */
    public void visitMethod(MethodNode method) {
        List<LocalVariableNode> locals = method.localVariables;
        List<ParameterNode> params = method.parameters;
        if (method.desc.indexOf(')') == 1 && params == null) {
            // since the description starts with a '(' we don't need to check that one
            // a closing parenthesis after the opening one suggests that there are no input parameters.
            return;
        }
        if ((method.access & Opcodes.ACC_ABSTRACT) != 0) {
            // abstract methods do not have any local variables apparently.
            // It makes sense however given that abstract methods do not have a method body
            // where local variables could be declared
            return;
        }
        if (!Objects.requireNonNull(locals).isEmpty()) {
            // LVTs that have been left alone by the obfuscator will have at least one declared local
            return;
        }

        if (params == null) {
            method.parameters = new ArrayList<>();
            params = method.parameters;
            // Generate method parameter array
            DescString description = new DescString(method.desc);
            List<String> types = new ArrayList<>();
            while (description.hasNext()) {
                types.add(description.nextType());
            }
            Set<String> existingTypes = new HashSet<>();
            Set<String> duplicateTypes = new HashSet<>();
            duplicateTypes.add("Ljava/lang/Class;"); // class is a keyword
            boolean oneArray = false;
            boolean multipleArrays = false;
            for (String type : types) {
                if (type.charAt(0) == '[') {
                    if (oneArray) {
                        multipleArrays = true;
                    } else {
                        oneArray = true;
                    }
                } else {
                    if (!existingTypes.add(type)) {
                        duplicateTypes.add(type);
                    }
                }
            }
            for (int i = 0; i < types.size(); i++) {
                String type = types.get(i);
                String name = null;
                switch (type.charAt(0)) {
                    case 'L':
                        int cutOffIndex = Math.max(type.lastIndexOf('/'), type.lastIndexOf('$')) + 1;
                        name = (char) Character.toLowerCase(type.codePointAt(cutOffIndex)) + type.substring(cutOffIndex + 1, type.length() - 1);
                        if (name.length() < 3) {
                            name = "argument"; // This reduces the volatility of obfuscated code
                        }
                        if (duplicateTypes.contains(type)) {
                            name += i;
                        }
                        break;
                    case '[':
                        if (multipleArrays) {
                            name = "arr" + i;
                        } else {
                            name = "arr";
                        }
                        break;
                    case 'F': // float
                        name = "float" + i;
                        break;
                    case 'D': // double
                        name = "double" + i;
                        break;
                    case 'Z': // boolean
                        name = "boolean" + i;
                        break;
                    case 'B': // byte
                        name = "byte" + i;
                        break;
                    case 'C': // char
                        if (duplicateTypes.contains(type)) {
                            name = "character" + i;
                        } else {
                            name = "character";
                        }
                        break;
                    case 'S': // short
                        name = "short" + i;
                        break;
                    case 'I': // integer
                        if (duplicateTypes.contains(type)) {
                            name = "integer" + i;
                        } else {
                            name = "integer";
                        }
                        break;
                    case 'J': // long
                        name = "long" + i;
                        break;
                    default:
                        throw new IllegalStateException("Unknown type: " + type);
                }
                params.add(new ParameterNode(Objects.requireNonNull(name), 0));
            }
        }

        int localVariableIndex = 0;
        if ((method.access & Opcodes.ACC_STATIC) == 0) {
            localVariableIndex++;
        }
        DescString description = new DescString(method.desc);

        // since we can only guess when the parameters are used and when they are not
        // it only makes sense that we are cheating here and declaring empty label nodes.
        // Apparently both ASM and quiltflower accept this, so /shrug
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        for (int i = 0; i < params.size(); i++) {
            String type = description.nextType();
            LocalVariableNode a = new LocalVariableNode(params.get(i).name,
                    type,
                    null, // we can only guess about the signature, so it'll be null
                    start,
                    end,
                    localVariableIndex);
            char c = type.charAt(0);
            if (c == 'D' || c == 'J') {
                // doubles and longs take two frames on the stack. Makes sense, I know
                localVariableIndex += 2;
            } else {
                localVariableIndex++;
            }
            locals.add(a);
        }
        super.visitMethod(method);
    }

}
