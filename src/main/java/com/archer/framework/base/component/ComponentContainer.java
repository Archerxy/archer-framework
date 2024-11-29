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

import com.archer.framework.base.annotation.Async;
import com.archer.framework.base.annotation.Component;
import com.archer.framework.base.annotation.Config;
import com.archer.framework.base.annotation.ConfigComponent;
import com.archer.framework.base.annotation.Controller;
import com.archer.framework.base.annotation.Inject;
import com.archer.framework.base.annotation.Log;
import com.archer.framework.base.annotation.Service;
import com.archer.framework.base.annotation.Value;
import com.archer.framework.base.conf.Conf;
import com.archer.framework.base.exceptions.ArcherApplicationException;
import com.archer.framework.base.logger.LoggerInitliazer;
import com.archer.framework.base.util.ClassUtil;
import com.archer.framework.base.util.ValueUtil;
import com.archer.log.Logger;
import com.archer.net.ThreadPool;

public class ComponentContainer {
	
	private Map<String, ContainerInstance> components;
	private List<Object> controllers;
	private List<Class<?>> classes;
	private Conf conf;
	
	private ThreadPool pool;

	private LoggerInitliazer logIniter;
	private Logger log;

	protected ComponentContainer(List<Class<?>> classes, Conf conf, LoggerInitliazer logIniter) {
		this.components = new HashMap<>(1024);
		this.controllers = new ArrayList<>();
		this.classes = classes;
		this.conf = conf;

		this.logIniter = logIniter;
		this.log = logIniter.newLogger();
		this.pool = new ThreadPool(2);
		
		putComponent(getClass().getName(), this);
	}
	
	protected void loadForwardComponents() {
		if(log == null) {
			throw new ArcherApplicationException("Logger component has not been Injected beafore load other components");
		}
		for(Class<?> cls: classes) {
			if(ForwardComponent.class.isAssignableFrom(cls) && !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers())) {
				ForwardComponent fcop = (ForwardComponent) ClassUtil.newInstance(cls);
				putComponent(fcop.getClass().getName(), fcop);
				for(Object cop: fcop.listForwardComponents(classes)) {
					putComponent(cop.getClass().getName(), cop);
				}
			}
		}
	}
	
	protected void loadAllComponents() {
		if(log == null) {
			throw new ArcherApplicationException("Logger component has not been Injected beafore load other components");
		}
		log.info("loading components");
		List<Object> confInstances = new ArrayList<>(16);
		List<ContainerInstance> componentsWithParam = new ArrayList<>(16);
		for(Class<?> cls: classes) {
			Config config = cls.getAnnotation(Config.class);
			if(config != null) {
				try {
					confInstances.add(cls.newInstance());
				} catch (InstantiationException | IllegalAccessException e) {
					throw new ArcherApplicationException("can not construct instance of '" + cls.getName() + "'");
				}
				continue;
			}
			

			Controller c = cls.getAnnotation(Controller.class);
			if(c != null) {
				Object ins = null;
				try {
					ins = cls.newInstance();
					putComponent(cls.getName(), ins);
					this.controllers.add(ins);
				} catch (InstantiationException | IllegalAccessException e) {
					throw new ArcherApplicationException("can not construct instance of '" + cls.getName() + "'");
				}
				continue;
			}
			
			Component component = cls.getAnnotation(Component.class);
			if(component != null) {
				String name = component.name().isEmpty() ? cls.getName() : component.name();
				ContainerInstance insOrNull;
				if(ifNeedAsyncProxy(cls)) {
					insOrNull = ClassUtil.tryNewProxyInstance(cls);
				} else {
					insOrNull = ClassUtil.tryNewInstance(cls);
				}
				insOrNull.setName(name);
				if(insOrNull.getInstance() == null) {
					componentsWithParam.add(insOrNull);
				} else {
					if(insOrNull.isProxy()) {
						setProxyInstance(insOrNull.getProxyClass(), insOrNull.getInstance());
					}
					putComponent(name, insOrNull);
				}
				continue;
			}

			Service service = cls.getAnnotation(Service.class);
			if(service != null) {
				ContainerInstance insOrNull;
				if(ifNeedAsyncProxy(cls)) {
					insOrNull = ClassUtil.tryNewProxyInstance(cls);
				} else {
					insOrNull = ClassUtil.tryNewInstance(cls);
				}
				insOrNull.setName(cls.getName());
				if(insOrNull.getInstance() == null) {
					componentsWithParam.add(insOrNull);
				} else {
					if(insOrNull.isProxy()) {
						setProxyInstance(insOrNull.getProxyClass(), insOrNull.getInstance());
					}
					putComponent(cls.getName(), insOrNull);
				}
			}
		}

		log.info("load components with params");
		loadComponentsWithParams(componentsWithParam);

		log.info("injecting components");
		List<UnknownComponent> unknownComponents = injectKnownComponents();

		log.info("running configs");
		for(Object ins: confInstances) {
			Config config = ins.getClass().getAnnotation(Config.class);
			runConfigs(ins, config);
		}

		injectUnknownComponents(unknownComponents);
		
		this.pool.start();
	}
	
	private void loadComponentsWithParams(List<ContainerInstance> componentsWithParam) {
		for(ContainerInstance cwp: componentsWithParam) {
			cwp.newInstance(components);
			if(cwp.isProxy()) {
				setProxyInstance(cwp.getProxyClass(), cwp.getInstance());
			}
			putComponent(cwp.getName(), cwp);
		}
	}
	
	private List<UnknownComponent> injectKnownComponents() {
		List<UnknownComponent> unknownComponents = new LinkedList<>();
		for(ContainerInstance cwp: components.values()) {
			Field[] fields = cwp.getCls().getDeclaredFields();
			Object cop = cwp.getInstance();
			for(Field f: fields) {
				f.setAccessible(true);
				Log log = f.getAnnotation(Log.class);
				if(log != null) {
					if(!f.getType().equals(Logger.class)) {
						throw new ArcherApplicationException("can not set Logger instance to '" + 
								cwp.getCls() + "." + f.getName() + "' with type '" + f.getType().getName() + "'");
					}
					try {
						f.set(cop, getLogger(f.getType(), log));
					} catch (Exception e) {
						throw new ArcherApplicationException("can not set Component instance to '" + 
								cwp.getCls() + "." + f.getName() + "'");
					}
					continue ;
				}
				
				Inject inj = f.getAnnotation(Inject.class);
				if(inj != null) {
					String name = inj.name().isEmpty() ? f.getType().getName() : inj.name();
					Object dep = getComponent(name);
					if(dep == null) {
						unknownComponents.add(new UnknownComponent(name, f, cwp));
						continue;
					}
					try {
						f.set(cop, dep);
					} catch (Exception e) {
						throw new ArcherApplicationException("can not set Component instance to '" + 
								cwp.getCls() + "." + f.getName() + "'");
					}
					continue ;
				}
				
				Value val = f.getAnnotation(Value.class);
				if(val != null) {
					try {
						if(!ValueUtil.setValue(f, conf, cop, val.id(), val.defaultVal().isEmpty() ? null : val.defaultVal())) {
							throw new ArcherApplicationException("can not found Value '" + val.id() + 
									"' at '" + cwp.getCls() + "." + f.getName() + "'");
						}
					} catch (Exception e) {
						throw new ArcherApplicationException("can not set Value '" + conf.getString(val.id()) + "' to '" + 
								cwp.getCls() + "." + f.getName() + "'", e);
					}
				}
			}
		}
		
		return unknownComponents;
	}
	
	private void runConfigs(Object ins, Config config) {
		Class<?> cls = ins.getClass();
		for(Field f: cls.getDeclaredFields()) {
			f.setAccessible(true);
			
			Log log = f.getAnnotation(Log.class);
			if(log != null) {
				if(!f.getType().equals(Logger.class)) {
					throw new ArcherApplicationException("can not set Logger instance to '" + 
							cls.getName() + "." + f.getName() + "' with type '" + f.getType().getName() + "'");
				}
				try {
					f.set(ins, getLogger(f.getType(), log));
				} catch (Exception e) {
					throw new ArcherApplicationException("can not set Component instance to '" + 
							cls.getName() + "." + f.getName() + "'");
				}
				continue ;
			}
			
			Value val = null;
			if((val = f.getAnnotation(Value.class)) != null) {
				try {
					if(!ValueUtil.setValue(f, conf, ins, val.id(), val.defaultVal().isEmpty() ? null : val.defaultVal())) {
						throw new ArcherApplicationException("can not found Value '" + val.id() + 
								"' at '" + cls.getName() + "." + f.getName() + "'");
					}
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new ArcherApplicationException("can not found Value '" + val.id() + 
							"' at '" + cls.getName() + "." + f.getName() +"'", e);
				}
				continue;
			}
			Inject inj = null;
			if((inj = f.getAnnotation(Inject.class)) != null) {
				String name = inj.name().isEmpty() ? f.getType().getName() : inj.name();
				Object dep = getComponent(name);
				if(dep == null) {
					throw new ArcherApplicationException("can not set Component instance to '" + 
							cls.getName() + "." + f.getName() + "'");
				}
				try {
					f.set(ins, dep);
				} catch (Exception e) {
					throw new ArcherApplicationException("can not set Component instance to '" + 
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
					putComponent(name, v);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new ArcherApplicationException("can not invoke ConfigComponent '" + cls.getName() + "." + 
							m.getName() + "' to construct '" + name + "'", e);
				}
			}
		}
	}
	
	private void injectUnknownComponents(List<UnknownComponent> knownComponents) {
		for(UnknownComponent uncop: knownComponents) {
			Object dep = getComponent(uncop.getName());
			if(dep == null) {
				throw new ArcherApplicationException("can not found Component of type '" + uncop.getName() + 
						"' at '" + uncop.getIns().getClass().getName() + "." + uncop.getF().getName() +"'");
			}
			try {
				uncop.getF().setAccessible(true);
				uncop.getF().set(uncop.getIns().getInstance(), dep);
			} catch (IllegalAccessException | IllegalArgumentException e) {
				throw new ArcherApplicationException("can not set Component instance to '" + 
						uncop.getIns().getClass().getName() + "." + uncop.getF().getName() + "'");
			}
		}
	}
	
	public List<Object> getControllers() {
		return controllers;
	}
	
	public Object getComponent(Class<?> cls) {
		ContainerInstance obj = components.getOrDefault(cls.getName(), null);
		if(obj == null) {
			return getImplementComponent(cls);
		}
		return obj.getInstance();
	}
	
	public Object getComponent(String name) {
		ContainerInstance obj = components.getOrDefault(name, null);
		if(obj == null) {
			Class<?> cls;
			try {
				cls = Class.forName(name);
			} catch (ClassNotFoundException ignore) {
				return null;
			}
			return getImplementComponent(cls);
		}
		return obj.getInstance();
	}
	
	private void putComponent(String key, Object comp) {
		if(components.containsKey(key)) {
			throw new ArcherApplicationException("duplicate component '" + key + "'");
		}
		components.put(key, new ContainerInstance(comp));
	}
	
	private void putComponent(String key, ContainerInstance ins) {
		if(components.containsKey(key)) {
			throw new ArcherApplicationException("duplicate component '" + key + "'");
		}
		components.put(key, ins);
	}
	
	private Object getImplementComponent(Class<?> cls) {
		Object obj = null;
		for(Map.Entry<String, ContainerInstance> entry: components.entrySet()) {
			Class<?> keyCls;
			try {
				keyCls = Class.forName(entry.getKey());
			} catch (ClassNotFoundException ignore) {
				continue;
			}
			if(cls.isAssignableFrom(keyCls)) {
				if(obj == null) {
					obj = entry.getValue().getInstance();
				} else {
					throw new ArcherApplicationException("dumplicated implements of '"+cls.getName()+
							"', first is '"+obj.getClass().getName()+
							"', second is '"+entry.getValue().getClass().getName()+"'");
				}
			}
		}
		return obj;
	}
	
	private boolean ifNeedAsyncProxy(Class<?> clazz) {
		for(Method m: clazz.getDeclaredMethods()) {
			Async async = m.getAnnotation(Async.class);
			if(async != null && !"<init>".equals(m.getName())) {
				if(m.getReturnType() != void.class) {
					throw new ArcherApplicationException("async method '" + clazz.getName() + "." + m.getName() + "' must return void");
				}
				return true;
			}
		}
		return false;
	}
	
	private Logger getLogger(Class<?> clazz, Log log) {
		String name = log.name();
		if(name.isEmpty()) {
			return this.log;
		}
		return logIniter.newLogger(name, name, log.level());
	}
	
	private void setProxyInstance(Class<?> proxyCls, Object ins) {
		try {
			Field poolField = proxyCls.getDeclaredField("pool");
			poolField.setAccessible(true);
			poolField.set(ins, pool);
		} catch(Exception e) {
			throw new ArcherApplicationException(e);
		}
	}
	
	public Logger logger() {
		return log;
	}
	
	public List<Class<?>> listClasses() {
		return classes;
	}
}
