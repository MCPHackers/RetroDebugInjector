package org.mcphackers.rdi.injector.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mcphackers.rdi.util.MethodReference;

public class Exceptions {

	Map<MethodReference, List<String>> exceptions = new HashMap<MethodReference, List<String>>();

	public List<String> getExceptions(MethodReference ref) {
		List<String> ret = this.exceptions.get(ref);
		return ret == null ? new ArrayList<String>() : ret;
	}

	public void setExceptions(MethodReference ref, List<String> excs) {
		this.exceptions.put(ref, excs);
	}
}
