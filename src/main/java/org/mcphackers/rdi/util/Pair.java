package org.mcphackers.rdi.util;

public class Pair<T, U> {
	
	public T left;
	public U right;
	
	private Pair(T val, U val2) {
		left = val;
		right = val2;
	}
	
	public T getLeft() {
		return left;
	}
	
	public U getRight() {
		return right;
	}
	
	public void setLeft(T val) {
		left = val;
	}
	
	public void setRight(U val) {
		right = val;
	}
	
	public boolean equals(Object o) {
		if(o instanceof Pair) {
			Pair<?,?> pair = (Pair<?,?>)o;
			boolean leftEqual = left == pair.left || (left != null && left.equals(pair.left));
			boolean rightEqual = right == pair.right || (right != null && right.equals(pair.right));
			return leftEqual && rightEqual;
		}
		return false;
	}
	
	public static <A, B> Pair<A, B> of(A a, B b) {
		return new Pair<A, B>(a, b);
	}

}
