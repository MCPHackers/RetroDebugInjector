package me.zero.rdi.wrapper;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.List;

public class RDIFieldWrapper {
    protected RDIClassWrapper classWrapper;

    public RDIFieldWrapper (ClassNode classNode) {
        this.classWrapper = new RDIClassWrapper(classNode);
    }

    public RDIFieldWrapper (RDIClassWrapper classWrapper) {
        this.classWrapper = classWrapper;
    }

    // Transformation methods
    public List<FieldNode> getFields () {
        return this.classWrapper.classNode.fields;
    }
}
