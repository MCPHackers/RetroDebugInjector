package org.mcphackers.rdi.injector.data.constants;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;

import static org.mcphackers.rdi.injector.data.constants.ConstantPool.constant;

public class PiConstants extends Constant {
	public final List<Constant> piDoubles = new ArrayList<>();
	public final List<Constant> piFloats = new ArrayList<>();
	
	// Math.PI
	private final ConstantBuilder pi = new ConstantBuilder().set("java/lang/Math", "PI", Math.PI);
	// (float)Math.PI
	private final ConstantBuilder piFloat = new ConstantBuilder().set("java/lang/Math", "PI", Math.PI).toFloat();
	// (double)(float)Math.PI
	private final ConstantBuilder piFloatDouble = new ConstantBuilder().set("java/lang/Math", "PI", Math.PI).toFloat().toDouble();
	
	public PiConstants() {
		add(pi);
		add(piFloat);
		add(piFloatDouble);
	}
	
	private void add(ConstantBuilder constant) {
		if(constant.type == ConstantBuilder.DOUBLE) {
			piDoubles.add(constant.build());
			piDoubles.add(constant.copy().divide(180D).build());
			piDoubles.add(constant.copy().multiply(1.85D).build()); // TODO dividing by PI loses precision sometimes
			piDoubles.add(constant.copy().multiply(0.185D).build());
			piDoubles.add(constant.copy().multiply(3D).divide(2D).build());
			piDoubles.add(constant.copy().multiply(2D).divide(9D).build());
			piDoubles.add(constant.copy().multiply(4D).divide(3D).build());
			piDoubles.add(constant().set(180D).divide(constant).build());
		}
		else if(constant.type == ConstantBuilder.FLOAT) {
			piFloats.add(constant.build());
			piFloats.add(constant.copy().divide(180F).build());
			piFloats.add(constant.copy().multiply(1.85F).build()); // TODO dividing by PI loses precision sometimes
			piFloats.add(constant.copy().multiply(0.185F).build());
			piFloats.add(constant.copy().multiply(3F).divide(2F).build());
			piFloats.add(constant.copy().multiply(2F).divide(9F).build());
			piFloats.add(constant.copy().multiply(4F).divide(3F).build());
			piFloats.add(constant().set(180F).divide(constant).build());
		}
	}

	public boolean replace(InsnList sourceList, LdcInsnNode constant) {
		if(constant.cst instanceof Double) {
			for(Constant constantReplacer : piDoubles) {
				if(constantReplacer.replace(sourceList, constant)) {
					return true;
				}
			}
			double constD = (double)constant.cst;
			String d = Double.toString(constD);
			if(d.length() - d.indexOf('.') > 5) {
				for(ConstantBuilder piConst : new ConstantBuilder[] { pi, piFloatDouble }) {
					double cstDouble = constD / piConst.doubleValue;
					d = Double.toString(cstDouble);
					if(d.length() - d.indexOf('.') - 1 <= 3) {
						ConstantBuilder piMul = piConst.copy();
						if(cstDouble != 1D) {
							piMul.multiply(cstDouble);
						}
						Constant piConstant = piMul.build();
						if(piConstant.replace(sourceList, constant)) {
							return true;
						}
					}
				}
			}
		}
		else if(constant.cst instanceof Float) {
			for(Constant constantReplacer : piFloats) {
				if(constantReplacer.replace(sourceList, constant)) {
					return true;
				}
			}
			float constF = (float)constant.cst;
			String f = Float.toString(constF);
			if(f.length() - f.indexOf('.') > 5) {
				float cstFloat = constF / piFloat.floatValue;
				f = Float.toString(cstFloat);
				if(f.length() - f.indexOf('.') - 1 <= 3) {
					ConstantBuilder piMul = piFloat.copy();
					if(cstFloat != 1F) {
						piMul.multiply(cstFloat);
					}
					Constant piConstant = piMul.build();
					if(piConstant.replace(sourceList, constant)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public PiConstants copy() {
		return this;
	}

}
