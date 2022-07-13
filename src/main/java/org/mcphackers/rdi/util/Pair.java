package org.mcphackers.rdi.util;

public class Pair<T, U> {
	
	public T left;
	public U right;
	
	public Pair(T val, U val2) {
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

}
