package org.mcphackers.rdi.util;

import org.objectweb.asm.Opcodes;

/**
 * Opcode categorisation utility tool.
 */
public final class OPHelper {

    public static final boolean isArrayLoad(int opcode) {
        switch (opcode) {
        case Opcodes.AALOAD:
        case Opcodes.BALOAD: // for booleans and bytes
        case Opcodes.CALOAD:
        case Opcodes.DALOAD:
        case Opcodes.FALOAD:
        case Opcodes.IALOAD:
        case Opcodes.LALOAD:
        case Opcodes.SALOAD:
            return true;
        default:
            return false;
        }
    }

    public static final boolean isVarLoad(int opcode) {
        switch (opcode) {
        case Opcodes.ALOAD:
        //case Opcodes.BLOAD: // bytes & booleans are also regular integers
        //case Opcodes.CLOAD: // characters are regular integers (?) under the hood
        case Opcodes.DLOAD:
        case Opcodes.FLOAD:
        case Opcodes.ILOAD:
        case Opcodes.LLOAD:
        //case Opcodes.SLOAD: // and so are shorts
            return true;
        default:
            return false;
        }
    }

    public static final boolean isVarSimilarType(int opcode1, int opcode2) {
        switch (opcode1) {
        case Opcodes.AALOAD:
            return opcode2 == Opcodes.ALOAD;
        case Opcodes.BALOAD:
        case Opcodes.CALOAD:
        case Opcodes.IALOAD:
        case Opcodes.SALOAD:
            return opcode2 == Opcodes.ILOAD;
        case Opcodes.DALOAD:
            return opcode2 == Opcodes.DLOAD;
        case Opcodes.FALOAD:
            return opcode2 == Opcodes.FLOAD;
        case Opcodes.LALOAD:
            return opcode2 == Opcodes.LLOAD;
        case Opcodes.ALOAD:
            return opcode2 == Opcodes.AALOAD;
        case Opcodes.DLOAD:
            return opcode2 == Opcodes.DALOAD;
        case Opcodes.FLOAD:
            return opcode2 == Opcodes.FALOAD;
        case Opcodes.ILOAD:
            return opcode2 == Opcodes.IALOAD || opcode2 == Opcodes.SALOAD
                || opcode2 == Opcodes.BALOAD || opcode2 == Opcodes.CALOAD;
        case Opcodes.LLOAD:
            return opcode2 == Opcodes.LALOAD;
        // -- The same story for the store operation family --
        case Opcodes.AASTORE:
            return opcode2 == Opcodes.ASTORE;
        case Opcodes.BASTORE:
        case Opcodes.CASTORE:
        case Opcodes.IASTORE:
        case Opcodes.SASTORE:
            return opcode2 == Opcodes.ISTORE;
        case Opcodes.DASTORE:
            return opcode2 == Opcodes.DSTORE;
        case Opcodes.FASTORE:
            return opcode2 == Opcodes.FSTORE;
        case Opcodes.LASTORE:
            return opcode2 == Opcodes.LSTORE;
        case Opcodes.ASTORE:
            return opcode2 == Opcodes.ASTORE;
        case Opcodes.DSTORE:
            return opcode2 == Opcodes.DSTORE;
        case Opcodes.FSTORE:
            return opcode2 == Opcodes.FSTORE;
        case Opcodes.ISTORE:
            return opcode2 == Opcodes.IASTORE || opcode2 == Opcodes.SASTORE
                || opcode2 == Opcodes.BASTORE || opcode2 == Opcodes.CASTORE;
        case Opcodes.LSTORE:
            return opcode2 == Opcodes.LSTORE;
        default:
            throw new IllegalArgumentException("Opcode1 not valid.");
        }
    }

    public static final boolean isVarStore(int opcode) {
        switch (opcode) {
        case Opcodes.ASTORE:
        //case Opcodes.BLOAD: // bytes & booleans are also regular integers
        //case Opcodes.CLOAD: // characters are regular integers (?) under the hood
        case Opcodes.DSTORE:
        case Opcodes.FSTORE:
        case Opcodes.ISTORE:
        case Opcodes.LSTORE:
        //case Opcodes.SLOAD: // and so are shorts
            return true;
        default:
            return false;
        }
    }
}