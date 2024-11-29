package com.archer.framework.base.component;

import java.lang.reflect.Field;

class UnknownComponent {
	
	private String name;
	
	private Field f;
	
	private ContainerInstance ins;

	public UnknownComponent(String name, Field f, ContainerInstance ins) {
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

	public ContainerInstance getIns() {
		return ins;
	}
}
