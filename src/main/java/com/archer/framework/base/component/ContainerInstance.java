package com.archer.framework.base.component;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Map;

import com.archer.framework.base.annotation.ConstructorParam;
import com.archer.framework.base.exceptions.ArcherApplicationException;

public class ContainerInstance {
	
	private Object instance;
	
	private Class<?> cls;
	
	private Parameter[] params;
	
	private Constructor<?> constructor;
	
	private String name;
	
	private boolean proxy;

	private Class<?> proxyClass;

	public ContainerInstance(Object instance) {
		this(instance.getClass(), null, null, null);
		this.instance = instance;
	}
	
	public ContainerInstance(Class<?> cls, Object instance, Class<?> proxyClass) {
		this(cls, null, null, proxyClass);
		this.instance = instance;
	}

	
	public ContainerInstance(Class<?> cls, Constructor<?> constructor, Parameter[] params) {
		this(cls, constructor, params, null);
	}
	
	public ContainerInstance(Class<?> cls, Constructor<?> constructor, Parameter[] params, Class<?> proxyClass) {
		this.cls = cls;
		this.constructor = constructor;
		this.params = params;
		this.proxyClass = proxyClass;
		this.proxy = proxyClass != null;
	}
	
	public Object newInstance(Map<String, ContainerInstance> components) {
		Object[] paramVals = new Object[params.length];
		for(int i = 0; i < params.length; i++) {
			Parameter p = params[i];
			ConstructorParam anp = p.getAnnotation(ConstructorParam.class);
			if(anp == null) {
				throw new ArcherApplicationException("class '"+p.getClass().getName()+"' constructor params is not a 'ConstructorParam' ");
			}
			String name = anp.name();
			if(name.isEmpty()) {
				name = p.getClass().getName();
			}
			ContainerInstance pobj = components.getOrDefault(name, null);
			if(pobj == null) {
				throw new ArcherApplicationException("can not found component (or whitch has no argurment constructor) '" + name + "' to construct " + cls.getName());
			}
			paramVals[i] = pobj.getInstance();
		}
		try {
			instance = constructor.newInstance(paramVals);
			return instance;
		} catch (Exception e) {
			throw new ArcherApplicationException("can not construct instance of '" + cls.getName() + "'", e);
		}
	}

	public Object getInstance() {
		return instance;
	}

	public Class<?> getCls() {
		return cls;
	}

	public Parameter[] getParams() {
		return params;
	}

	public Constructor<?> getConstructor() {
		return constructor;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isProxy() {
		return proxy;
	}

	public Class<?> getProxyClass() {
		return proxyClass;
	}
}
