package org.mcphackers.rdi.injector.data.constants;

import java.util.ArrayList;
import java.util.List;

public class ConstantPool {
	
	public static final List<Constant> CONSTANTS = new ArrayList<Constant>();

	static {
		add(constant().set("java/lang/Short", "MAX_VALUE", Short.MAX_VALUE));
		add(constant().set("java/lang/Short", "MIN_VALUE", Short.MIN_VALUE));
		add(constant().set("java/lang/Integer", "MAX_VALUE", Integer.MAX_VALUE));
		add(constant().set("java/lang/Integer", "MIN_VALUE", Integer.MIN_VALUE));
		add(constant().set("java/lang/Long", "MAX_VALUE", Long.MAX_VALUE));
		add(constant().set("java/lang/Long", "MIN_VALUE", Long.MIN_VALUE));
		add(constant().set("java/lang/Float", "MAX_VALUE", Float.MAX_VALUE));
		add(constant().set("java/lang/Float", "MIN_VALUE", Float.MIN_VALUE));
		add(constant().set("java/lang/Float", "NaN", Float.NaN));
		add(constant().set("java/lang/Float", "POSITIVE_INFINITY", Float.POSITIVE_INFINITY));
		add(constant().set("java/lang/Float", "NEGATIVE_INFINITY", Float.NEGATIVE_INFINITY));
		add(constant().set("java/lang/Double", "MAX_VALUE", Double.MAX_VALUE));
		add(constant().set("java/lang/Double", "MIN_VALUE", Double.MIN_VALUE));
		add(constant().set("java/lang/Double", "NaN", Double.NaN));
		add(constant().set("java/lang/Double", "POSITIVE_INFINITY", Double.POSITIVE_INFINITY));
		add(constant().set("java/lang/Double", "NEGATIVE_INFINITY", Double.NEGATIVE_INFINITY));
		add(constant().set("java/lang/Math", "E", Math.E));
		add(constant().set("java/lang/Math", "E", Math.E).toFloat());
		add(constant().set("java/lang/Math", "E", Math.E).toFloat().toDouble());
		add(constant().set("java/lang/Integer", "MAX_VALUE", Integer.MAX_VALUE).toDouble());
		
		add(constant().set(1F).divide(16F));
		add(constant().set(2F).divide(16F));
		add(constant().set(3F).divide(16F));
		add(constant().set(5F).divide(16F));
		add(constant().set(6F).divide(16F));
		add(constant().set(7F).divide(16F));
		add(constant().set(9F).divide(16F));
		add(constant().set(10F).divide(16F));
		add(constant().set(11F).divide(16F));
		add(constant().set(12F).divide(16F));
		add(constant().set(13F).divide(16F));
		add(constant().set(14F).divide(16F));
		add(constant().set(15F).divide(16F));
		add(constant().set(0.1F).divide(16F));
		add(constant().set(9F).divide(32F));
		add(constant().set(25F).divide(32F));
		
		add(constant().set(0.999F).divide(4F));
		add(constant().set(0.999F).divide(16F));
		add(constant().set(0.999F).divide(64F));
		//TODO Implement custom behavior for color constants
		add(constant().set(216F).divide(255F));
		add(constant().set(192F).divide(255F));
		add(constant().set(224F).divide(360F));

		add(constant().set(0.1F).multiply(0.1F).multiply(100F));
		add(constant().set(0.1F).multiply(0.1F).multiply(106F));
		add(constant().set(0.1F).multiply(0.1F).multiply(58.8F));
		add(constant().set(546F).multiply(0.1F).multiply(0.1F).multiply(0.1F));
		add(constant().set(11F).divide(32F));
		add(constant().set(0.5F).divide(1024F));
		add(constant().set(1F).divide(1024F));
		add(constant().set(6D).divide(256D));
		add(constant().set(9D).divide(256D));
		add(constant().set(1D).divide(3D));
		add(constant().set(2D).divide(3D));
		add(constant().set(4D).divide(3D));
		add(constant().set(1F).divide(3F));
		add(constant().set(2F).divide(3F));
		add(constant().set(4F).divide(3F));
		add(constant().set(1F).divide(9F));
		add(constant().set(7F).divide(320F));
		add(constant().set(385F).divide(512F));
		add(constant().set(10F).multiply(0.01F));
		add(constant().set(0.1D).multiply(0.1D));
		add(constant().set(1.25D).divide(64D));
		add(constant().set(1.75D).divide(64D));
		add(constant().set(2F).divide(3F).multiply(0.01F));
		add(constant().set(1D).divide(60D).toFloat());
		add(new PiConstants());
		add(new FloatCastedToDouble());
		add(new OneDivideByPowerOfTwo());
		
	}

	private static void add(ConstantBuilder constant) {
		CONSTANTS.add(constant.build());
	}

	private static void add(Constant constant) {
		CONSTANTS.add(constant);
	}
	
	public static ConstantBuilder constant() {
		return ConstantBuilder.newInstance(); 
	}

}
