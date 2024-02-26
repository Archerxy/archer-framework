package com.archer.framework.base.component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.archer.framework.base.annotation.Component;
import com.archer.framework.base.annotation.Config;
import com.archer.framework.base.annotation.ConfigComponent;
import com.archer.framework.base.annotation.Controller;
import com.archer.framework.base.annotation.Inject;
import com.archer.framework.base.annotation.Service;
import com.archer.framework.base.annotation.Value;
import com.archer.framework.base.conf.Conf;
import com.archer.framework.base.exceptions.ContainerException;
import com.archer.framework.base.util.ClassUtil;
import com.archer.framework.base.util.ValueUtil;
import com.archer.log.Logger;

public class ComponentContainer {
	
	private Map<String, Object> components;
	private List<Object> controllers;
	private List<Class<?>> classes;
	private Conf conf;

	private Logger log;

	protected ComponentContainer(List<Class<?>> classes, Conf conf) {
		this.components = new HashMap<>(1024);
		this.controllers = new ArrayList<>();
		this.classes = classes;
		this.conf = conf;
		
		this.components.put(getClass().getName(), this);
	}
	
	protected void loadForwardComponents() {
		if(log == null) {
			throw new ContainerException("Logger component has not been Injected beafore load other components");
		}
		for(Class<?> cls: classes) {
			if(ForwardComponent.class.isAssignableFrom(cls) && !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers())) {
				ForwardComponent fcop = (ForwardComponent) ClassUtil.newInstance(cls);
				this.components.put(fcop.getClass().getName(), fcop);
				for(Object cop: fcop.listForwardComponents(classes)) {
					this.components.put(cop.getClass().getName(), cop);
				}
			}
		}
	}
	
	protected void loadAllComponents() {
		if(log == null) {
			throw new ContainerException("Logger component has not been Injected beafore load other components");
		}
		log.info("loading components");
		List<Object> confInstances = new ArrayList<>(16);
		for(Class<?> cls: classes) {
			Config config = cls.getAnnotation(Config.class);
			if(config != null) {
				try {
					confInstances.add(cls.newInstance());
				} catch (InstantiationException | IllegalAccessException e) {
					throw new ContainerException("can not construct instance of '" + cls.getName() + "'");
				}
				continue;
			}
			

			Controller c = cls.getAnnotation(Controller.class);
			if(c != null) {
				Object ins = null;
				try {
					ins = cls.newInstance();
					this.components.put(cls.getName(), ins);
					this.controllers.add(ins);
				} catch (InstantiationException | IllegalAccessException e) {
					throw new ContainerException("can not construct instance of '" + cls.getName() + "'");
				}
				continue;
			}
			
			Component component = cls.getAnnotation(Component.class);
			if(component != null) {
				String name = component.name().isEmpty() ? cls.getName() : component.name();
				try {
					components.put(name, cls.newInstance());
				} catch (InstantiationException | IllegalAccessException e) {
					throw new ContainerException("can not construct instance of '" + cls.getName() + "'");
				}
				continue;
			}

			Service service = cls.getAnnotation(Service.class);
			if(service != null) {
				try {
					components.put(cls.getName(), cls.newInstance());
				} catch (InstantiationException | IllegalAccessException e) {
					throw new ContainerException("can not construct instance of '" + cls.getName() + "'");
				}
			}
		}

		log.info("injecting components");
		List<UnknownComponent> knownComponents = injectKnownComponents();

		log.info("running configs");
		for(Object ins: confInstances) {
			Config config = ins.getClass().getAnnotation(Config.class);
			runConfigs(ins, config);
		}

		injectUnknownComponents(knownComponents);
	}
	
	private List<UnknownComponent> injectKnownComponents() {
		List<UnknownComponent> unknownComponents = new LinkedList<>();
		for(Object cop: components.values()) {
			Field[] fields = cop.getClass().getDeclaredFields();
			for(Field f: fields) {
				Inject inj = f.getAnnotation(Inject.class);
				if(inj != null) {
					String name = inj.name().isEmpty() ? f.getType().getName() : inj.name();
					Object dep = getComponent(name);
					if(dep == null) {
						unknownComponents.add(new UnknownComponent(name, f, cop));
						continue;
					}

					f.setAccessible(true);
					try {
						f.set(cop, dep);
					} catch (Exception e) {
						throw new ContainerException("can not set Component instance to '" + 
								cop.getClass().getName() + "." + f.getName() + "'");
					}
					continue ;
				}
				
				Value val = f.getAnnotation(Value.class);
				if(val != null) {
					try {
						if(!ValueUtil.setValue(f, conf, cop, val.id(), val.defaultVal().isEmpty() ? null : val.defaultVal())) {
							throw new ContainerException("can not found Value '" + val.id() + 
									"' at '" + cop.getClass().getName() + "." + f.getName() + "'");
						}
					} catch (Exception e) {
						throw new ContainerException("can not set Value '" + conf.getString(val.id()) + "' to '" + 
								cop.getClass().getName() + "." + f.getName() + "'");
					}
				}
			}
		}
		
		return unknownComponents;
	}
	
	private void runConfigs(Object ins, Config config) {
		Class<?> cls = ins.getClass();
		for(Field f: cls.getDeclaredFields()) {
			Value val = null;
			if((val = f.getAnnotation(Value.class)) != null) {
				f.setAccessible(true);
				try {
					if(!ValueUtil.setValue(f, conf, ins, val.id(), val.defaultVal().isEmpty() ? null : val.defaultVal())) {
						throw new ContainerException("can not found Value '" + val.id() + 
								"' at '" + cls.getName() + "." + f.getName() + "'");
					}
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new ContainerException("can not found Value '" + val.id() + 
							"' at '" + cls.getName() + "." + f.getName() +"'", e);
				}
				continue;
			}
			Inject inj = null;
			if((inj = f.getAnnotation(Inject.class)) != null) {
				f.setAccessible(true);
				String name = inj.name().isEmpty() ? f.getType().getName() : inj.name();
				Object dep = getComponent(name);
				if(dep == null) {
					throw new ContainerException("can not set Component instance to '" + 
							cls.getName() + "." + f.getName() + "'");
				}
				try {
					f.set(ins, dep);
				} catch (Exception e) {
					throw new ContainerException("can not set Component instance to '" + 
							cls.getName() + "." + f.getName() + "'", e);
				}
			}
		}
		Method[] methods = cls.getDeclaredMethods();
		for(Method m: methods) {
			ConfigComponent cop = m.getAnnotation(ConfigComponent.class);
			if(cop != null) {
				String name = cop.name().isEmpty() ? m.getReturnType().getName() : cop.name();
				try {
					m.setAccessible(true);
					Object v = m.invoke(ins);
					components.put(name, v);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new ContainerException("can not invoke ConfigComponent '" + cls.getName() + "." + 
							m.getName() + "' to construct '" + name + "'", e);
				}
			}
		}
	}
	
	private void injectUnknownComponents(List<UnknownComponent> knownComponents) {
		for(UnknownComponent uncop: knownComponents) {
			Object dep = getComponent(uncop.getName());
			if(dep == null) {
				throw new ContainerException("can not found Component of type '" + uncop.getName() + 
						"' at '" + uncop.getIns().getClass().getName() + "." + uncop.getF().getName() +"'");
			}
			try {
				uncop.getF().setAccessible(true);
				uncop.getF().set(uncop.getIns(), dep);
			} catch (IllegalAccessException | IllegalArgumentException e) {
				throw new ContainerException("can not set Component instance to '" + 
						uncop.getIns().getClass().getName() + "." + uncop.getF().getName() + "'");
			}
		}
	}
	
	public void componentLogger(Logger logger) {
		log = logger;
		components.put(logger.getClass().getName(), logger);
	}
	
	public List<Object> getControllers() {
		return controllers;
	}
	
	public Object getComponent(Class<?> cls) {
		return getComponent(cls.getName());
	}
	
	public Object getComponent(String name) {
		return components.getOrDefault(name, null);
	}
	
	public Logger logger() {
		return log;
	}
	
	public List<Class<?>> listClasses() {
		return classes;
	}
}
