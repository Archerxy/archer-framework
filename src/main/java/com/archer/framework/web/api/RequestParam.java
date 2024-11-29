package com.archer.framework.web.api;

import java.lang.reflect.Parameter;

public class RequestParam {
	
	static final int PATH = 1;
	static final int QUERY = 2;
	static final int BODY = 3;
	
	int type;
	
	String k;
	
	Object v;
	
	Parameter p;
	
	public RequestParam(int type, String k) {
		this(type, k, null);
	}
	
	public RequestParam(int type, String k, Parameter p) {
		this.type = type;
		this.k = k;
		this.p = p;
	}
	
	public int type() {
		return this.type;
	}
	
	public String k() {
		return k;
	}
	
	public Object v() {
		return v;
	}
	
	public void setVal(Object v) {
		this.v = v;
	}
	
	public void setParam(Parameter p) {
		this.p = p;
	}
	
	public Parameter param() {
		return p;
	}

}

