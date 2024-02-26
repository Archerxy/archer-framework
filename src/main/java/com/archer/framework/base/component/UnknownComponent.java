package com.archer.framework.base.component;

import java.lang.reflect.Field;

class UnknownComponent {
	
	private String name;
	
	private Field f;
	
	private Object ins;

	public UnknownComponent(String name, Field f, Object ins) {
		super();
		this.name = name;
		this.f = f;
		this.ins = ins;
	}

	public String getName() {
		return name;
	}

	public Field getF() {
		return f;
	}

	public Object getIns() {
		return ins;
	}
}
