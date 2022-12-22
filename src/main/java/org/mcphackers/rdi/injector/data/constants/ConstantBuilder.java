package org.mcphackers.rdi.injector.data.constants;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;

import static org.mcphackers.rdi.util.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.rdi.util.InsnHelper;

public class ConstantBuilder {
	
	private static ConstantBuilder INSTANCE = new ConstantBuilder();
	public static final int INT = 0;
	public static final int LONG = 1;
	public static final int FLOAT = 2;
	public static final int DOUBLE = 3;

	private InsnList insns = new InsnList();
	int type = -1;
	int intValue;
	long longValue;
	float floatValue;
	double doubleValue;
	
	public static ConstantBuilder newInstance() {
		return INSTANCE.clear();
	}

	// Initial value
	public ConstantBuilder set(int value) {
		type = INT;
		intValue = value;
		insns.add(intInsn(value));
		return this;
	}
	public ConstantBuilder set(long value) {
		type = LONG;
		longValue = value;
		insns.add(longInsn(value));
		return this;
	}
	public ConstantBuilder set(float value) {
		type = FLOAT;
		floatValue = value;
		insns.add(floatInsn(value));
		return this;
	}
	public ConstantBuilder set(double value) {
		type = DOUBLE;
		doubleValue = value;
		insns.add(doubleInsn(value));
		return this;
	}
	// Initial field value
	public ConstantBuilder set(String owner, String name, byte value) {
		type = INT;
		intValue = value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "B"));
		return this;
	}
	public ConstantBuilder set(String owner, String name, short value) {
		type = INT;
		intValue = value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "S"));
		return this;
	}
	public ConstantBuilder set(String owner, String name, int value) {
		type = INT;
		intValue = value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "I"));
		return this;
	}
	public ConstantBuilder set(String owner, String name, long value) {
		type = LONG;
		longValue = value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "J"));
		return this;
	}
	public ConstantBuilder set(String owner, String name, float value) {
		type = FLOAT;
		floatValue = value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "F"));
		return this;
	}
	public ConstantBuilder set(String owner, String name, double value) {
		type = DOUBLE;
		doubleValue = value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "D"));
		return this;
	}
	// Multiply by a constant
	public ConstantBuilder multiply(int value) {
		checkType(INT);
		intValue = intValue * value;
		insns.add(intInsn(value));
		insns.add(new InsnNode(IMUL));
		return this;
	}
	public ConstantBuilder multiply(long value) {
		checkType(LONG);
		longValue = longValue * value;
		insns.add(longInsn(value));
		insns.add(new InsnNode(LMUL));
		return this;
	}
	public ConstantBuilder multiply(float value) {
		checkType(FLOAT);
		floatValue = floatValue * value;
		insns.add(floatInsn(value));
		insns.add(new InsnNode(FMUL));
		return this;
	}
	public ConstantBuilder multiply(double value) {
		checkType(DOUBLE);
		doubleValue = doubleValue * value;
		insns.add(doubleInsn(value));
		insns.add(new InsnNode(DMUL));
		return this;
	}
	// Multiply by a constant field
	public ConstantBuilder multiply(String owner, String name, int value) {
		checkType(INT);
		intValue = intValue * value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "I"));
		insns.add(new InsnNode(IMUL));
		return this;
	}
	public ConstantBuilder multiply(String owner, String name, long value) {
		checkType(LONG);
		longValue = longValue * value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "J"));
		insns.add(new InsnNode(LMUL));
		return this;
	}
	public ConstantBuilder multiply(String owner, String name, float value) {
		checkType(FLOAT);
		floatValue = floatValue * value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "F"));
		insns.add(new InsnNode(FMUL));
		return this;
	}
	public ConstantBuilder multiply(String owner, String name, double value) {
		checkType(DOUBLE);
		doubleValue = doubleValue * value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "D"));
		insns.add(new InsnNode(DMUL));
		return this;
	}
	// Multiply by a value of another builder
	public ConstantBuilder multiply(ConstantBuilder builder) {
		checkType(builder.type);
		switch(type) {
		case INT:
			intValue = intValue * builder.intValue;
			insns.add(InsnHelper.clone(builder.insns));
			insns.add(new InsnNode(IMUL));
			break;
		case LONG:
			longValue = longValue * builder.longValue;
			insns.add(InsnHelper.clone(builder.insns));
			insns.add(new InsnNode(LMUL));
			break;
		case FLOAT:
			floatValue = floatValue * builder.floatValue;
			insns.add(InsnHelper.clone(builder.insns));
			insns.add(new InsnNode(FMUL));
			break;
		case DOUBLE:
			doubleValue = doubleValue * builder.doubleValue;
			insns.add(InsnHelper.clone(builder.insns));
			insns.add(new InsnNode(DMUL));
			break;
		}
		return this;
	}
	// Divide by a constant
	public ConstantBuilder divide(int value) {
		checkType(INT);
		intValue = intValue / value;
		insns.add(intInsn(value));
		insns.add(new InsnNode(IDIV));
		return this;
	}
	public ConstantBuilder divide(long value) {
		checkType(LONG);
		longValue = longValue / value;
		insns.add(longInsn(value));
		insns.add(new InsnNode(LDIV));
		return this;
	}
	public ConstantBuilder divide(float value) {
		checkType(FLOAT);
		floatValue = floatValue / value;
		insns.add(floatInsn(value));
		insns.add(new InsnNode(FDIV));
		return this;
	}
	public ConstantBuilder divide(double value) {
		checkType(DOUBLE);
		doubleValue = doubleValue / value;
		insns.add(doubleInsn(value));
		insns.add(new InsnNode(DDIV));
		return this;
	}
	// Divide by a constant field
	public ConstantBuilder divide(String owner, String name, int value) {
		checkType(INT);
		intValue = intValue / value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "I"));
		insns.add(new InsnNode(IDIV));
		return this;
	}
	public ConstantBuilder divide(String owner, String name, long value) {
		checkType(LONG);
		longValue = longValue / value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "J"));
		insns.add(new InsnNode(LDIV));
		return this;
	}
	public ConstantBuilder divide(String owner, String name, float value) {
		checkType(FLOAT);
		floatValue = floatValue / value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "F"));
		insns.add(new InsnNode(FDIV));
		return this;
	}
	public ConstantBuilder divide(String owner, String name, double value) {
		checkType(DOUBLE);
		doubleValue = doubleValue / value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "D"));
		insns.add(new InsnNode(DDIV));
		return this;
	}
	// Divide by a value of another builder
	public ConstantBuilder divide(ConstantBuilder builder) {
		checkType(builder.type);
		switch(type) {
		case INT:
			intValue = intValue / builder.intValue;
			insns.add(InsnHelper.clone(builder.insns));
			insns.add(new InsnNode(IDIV));
			break;
		case LONG:
			longValue = longValue / builder.longValue;
			insns.add(InsnHelper.clone(builder.insns));
			insns.add(new InsnNode(LDIV));
			break;
		case FLOAT:
			floatValue = floatValue / builder.floatValue;
			insns.add(InsnHelper.clone(builder.insns));
			insns.add(new InsnNode(FDIV));
			break;
		case DOUBLE:
			doubleValue = doubleValue / builder.doubleValue;
			insns.add(InsnHelper.clone(builder.insns));
			insns.add(new InsnNode(DDIV));
			break;
		}
		return this;
	}
	// Add a constant
	public ConstantBuilder add(int value) {
		checkType(INT);
		intValue = intValue + value;
		insns.add(intInsn(value));
		insns.add(new InsnNode(IADD));
		return this;
	}
	public ConstantBuilder add(long value) {
		checkType(LONG);
		longValue = longValue + value;
		insns.add(longInsn(value));
		insns.add(new InsnNode(LADD));
		return this;
	}
	public ConstantBuilder add(float value) {
		checkType(FLOAT);
		floatValue = floatValue + value;
		insns.add(floatInsn(value));
		insns.add(new InsnNode(FADD));
		return this;
	}
	public ConstantBuilder add(double value) {
		checkType(DOUBLE);
		doubleValue = doubleValue + value;
		insns.add(doubleInsn(value));
		insns.add(new InsnNode(DADD));
		return this;
	}
	// Add a constant field
	public ConstantBuilder add(String owner, String name, int value) {
		checkType(INT);
		intValue = intValue + value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "I"));
		insns.add(new InsnNode(IADD));
		return this;
	}
	public ConstantBuilder add(String owner, String name, long value) {
		checkType(LONG);
		longValue = longValue + value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "J"));
		insns.add(new InsnNode(LADD));
		return this;
	}
	public ConstantBuilder add(String owner, String name, float value) {
		checkType(FLOAT);
		floatValue = floatValue + value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "F"));
		insns.add(new InsnNode(FADD));
		return this;
	}
	public ConstantBuilder add(String owner, String name, double value) {
		checkType(DOUBLE);
		doubleValue = doubleValue + value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "D"));
		insns.add(new InsnNode(DADD));
		return this;
	}
	// Add value of another builder
	public ConstantBuilder add(ConstantBuilder builder) {
		checkType(builder.type);
		switch(type) {
		case INT:
			intValue = intValue + builder.intValue;
			insns.add(InsnHelper.clone(builder.insns));
			insns.add(new InsnNode(IADD));
			break;
		case LONG:
			longValue = longValue + builder.longValue;
			insns.add(InsnHelper.clone(builder.insns));
			insns.add(new InsnNode(LADD));
			break;
		case FLOAT:
			floatValue = floatValue + builder.floatValue;
			insns.add(InsnHelper.clone(builder.insns));
			insns.add(new InsnNode(FADD));
			break;
		case DOUBLE:
			doubleValue = doubleValue + builder.doubleValue;
			insns.add(InsnHelper.clone(builder.insns));
			insns.add(new InsnNode(DADD));
			break;
		}
		return this;
	}
	// Subtract a constant
	public ConstantBuilder subtract(int value) {
		checkType(INT);
		intValue = intValue - value;
		insns.add(intInsn(value));
		insns.add(new InsnNode(ISUB));
		return this;
	}
	public ConstantBuilder subtract(long value) {
		checkType(LONG);
		longValue = longValue - value;
		insns.add(longInsn(value));
		insns.add(new InsnNode(LSUB));
		return this;
	}
	public ConstantBuilder subtract(float value) {
		checkType(FLOAT);
		floatValue = floatValue - value;
		insns.add(floatInsn(value));
		insns.add(new InsnNode(FSUB));
		return this;
	}
	public ConstantBuilder subtract(double value) {
		checkType(DOUBLE);
		doubleValue = doubleValue - value;
		insns.add(doubleInsn(value));
		insns.add(new InsnNode(DSUB));
		return this;
	}
	// Subtract a constant field
	public ConstantBuilder subtract(String owner, String name, int value) {
		checkType(INT);
		intValue = intValue - value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "I"));
		insns.add(new InsnNode(ISUB));
		return this;
	}
	public ConstantBuilder subtract(String owner, String name, long value) {
		checkType(LONG);
		longValue = longValue - value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "J"));
		insns.add(new InsnNode(LSUB));
		return this;
	}
	public ConstantBuilder subtract(String owner, String name, float value) {
		checkType(FLOAT);
		floatValue = floatValue - value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "F"));
		insns.add(new InsnNode(FSUB));
		return this;
	}
	public ConstantBuilder subtract(String owner, String name, double value) {
		checkType(DOUBLE);
		doubleValue = doubleValue - value;
		insns.add(new FieldInsnNode(GETSTATIC, owner, name, "D"));
		insns.add(new InsnNode(DSUB));
		return this;
	}
	// Multiply by a value of another builder
	public ConstantBuilder subtract(ConstantBuilder builder) {
		checkType(builder.type);
		switch(type) {
		case INT:
			intValue = intValue - builder.intValue;
			insns.add(InsnHelper.clone(builder.insns));
			insns.add(new InsnNode(ISUB));
			break;
		case LONG:
			longValue = longValue - builder.longValue;
			insns.add(InsnHelper.clone(builder.insns));
			insns.add(new InsnNode(LSUB));
			break;
		case FLOAT:
			floatValue = floatValue - builder.floatValue;
			insns.add(InsnHelper.clone(builder.insns));
			insns.add(new InsnNode(FSUB));
			break;
		case DOUBLE:
			doubleValue = doubleValue - builder.doubleValue;
			insns.add(InsnHelper.clone(builder.insns));
			insns.add(new InsnNode(DSUB));
			break;
		}
		return this;
	}
	// Casts
	public ConstantBuilder toInt() {
		insns.add(cast(INT));
		type = INT;
		return this;
	}
	public ConstantBuilder toLong() {
		insns.add(cast(LONG));
		type = LONG;
		return this;
	}
	public ConstantBuilder toFloat() {
		insns.add(cast(FLOAT));
		type = FLOAT;
		return this;
	}
	public ConstantBuilder toDouble() {
		insns.add(cast(DOUBLE));
		type = DOUBLE;
		return this;
	}
	
	public Constant build() {
		switch(type) {
		case INT:
			return new InsnConstant<Integer>(insns, intValue);
		case LONG:
			return new InsnConstant<Long>(insns, longValue);
		case FLOAT:
			return new InsnConstant<Float>(insns, floatValue);
		case DOUBLE:
			return new InsnConstant<Double>(insns, doubleValue);
		default:
			return null;
		}
	}
	
	public ConstantBuilder copy() {
		ConstantBuilder builder = new ConstantBuilder();
		builder.type = type;
		builder.intValue = intValue;
		builder.longValue = longValue;
		builder.floatValue = floatValue;
		builder.doubleValue = doubleValue;
		builder.insns = InsnHelper.clone(insns);
		return builder;
	}
	
	private AbstractInsnNode cast(int targetType) {
		if(type == INT && targetType == LONG) {
			longValue = (long)intValue;
			return new InsnNode(I2L);
		}
		if(type == INT && targetType == FLOAT) {
			floatValue = (float)intValue;
			return new InsnNode(L2F);
		}
		if(type == INT && targetType == DOUBLE) {
			doubleValue = (double)intValue;
			return new InsnNode(I2D);
		}
		if(type == LONG && targetType == INT) {
			intValue = (int)longValue;
			return new InsnNode(L2I);
		}
		if(type == LONG && targetType == FLOAT) {
			floatValue = (float)longValue;
			return new InsnNode(L2F);
		}
		if(type == LONG && targetType == DOUBLE) {
			doubleValue = (double)longValue;
			return new InsnNode(L2D);
		}
		if(type == FLOAT && targetType == INT) {
			intValue = (int)floatValue;
			return new InsnNode(F2I);
		}
		if(type == FLOAT && targetType == LONG) {
			longValue = (long)floatValue;
			return new InsnNode(F2L);
		}
		if(type == FLOAT && targetType == DOUBLE) {
			doubleValue = (double)floatValue;
			return new InsnNode(F2D);
		}
		if(type == DOUBLE && targetType == INT) {
			intValue = (int)doubleValue;
			return new InsnNode(D2I);
		}
		if(type == DOUBLE && targetType == LONG) {
			longValue = (long)doubleValue;
			return new InsnNode(D2L);
		}
		if(type == DOUBLE && targetType == FLOAT) {
			floatValue = (float)doubleValue;
			return new InsnNode(D2F);
		}
		throw new IllegalStateException("Type cannot be casted");
	}

	private void checkType(int requiredType) {
		if(requiredType != type) {
			throw new IllegalStateException("Cast required!");
		}
	}

	private ConstantBuilder clear() {
		insns = new InsnList();
		return this;
	}
	
}
