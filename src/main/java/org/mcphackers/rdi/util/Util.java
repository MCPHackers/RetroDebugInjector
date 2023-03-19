package org.mcphackers.rdi.util;

import java.lang.reflect.Array;

public class Util {
    @SuppressWarnings("unchecked")
    public static <T> T[] arrayCopy(T[] original, int newLength) {
        return (T[]) arrayCopy(original, newLength, original.getClass());
    }
	
    public static <T,U> T[] arrayCopy(U[] original, int newLength, Class<? extends T[]> newType) {
        @SuppressWarnings("unchecked")
        T[] copy = ((Object)newType == (Object)Object[].class)
            ? (T[]) new Object[newLength]
            : (T[]) Array.newInstance(newType.getComponentType(), newLength);
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }
}
