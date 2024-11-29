package com.archer.framework.base.component;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.archer.framework.base.annotation.Component;
import com.archer.framework.base.annotation.Config;
import com.archer.framework.base.annotation.Controller;
import com.archer.framework.base.annotation.Service;
import com.archer.framework.base.conf.Conf;
import com.archer.framework.base.exceptions.ArcherApplicationException;
import com.archer.framework.base.logger.LoggerInitliazer;
import com.archer.framework.base.timer.Timer;

public class ClassContainer {
	
	private List<Class<?>> classes;
	private Conf conf;
	private ComponentContainer components;
	private Timer timer;
	
	public ClassContainer(Conf conf) {
		this.timer = new Timer();
		this.classes = listAllClasses();
		this.conf = conf;
		this.components = new ComponentContainer(classes, this.conf, new LoggerInitliazer(conf));
	}
	
	public ComponentContainer components() {
		return components;
	}
	
	public void loadComponents() {
		components.loadForwardComponents();
		components.loadAllComponents();
		components.logger().info("Archer Application started in {}ms", timer.calculateCost());
	}
	
	private List<Class<?>> listAllClasses() {
		List<Class<?>> classes = new LinkedList<>();
		ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        if (classLoader instanceof URLClassLoader) {
            URL[] urls = ((URLClassLoader) classLoader).getURLs();
            for (URL url : urls) {
            	String path = url.getPath();
            	try {
                	if(new File(path).isDirectory()) {
                		classes.addAll(getClassesFromPath(path, null));
                	} else if(path.endsWith(".jar")) {
                		classes.addAll(getClassesFromJar(path));
                	}
            	} catch(Exception ignore) {}
            }
        }
        
        return classes;
	}

	private List<Class<?>> getClassesFromPath(String path, String parentPkg) {
        List<Class<?>> classes = new ArrayList<>();
        File directory = new File(path);
        if(parentPkg == null) {
        	parentPkg = "";
        } else {
        	parentPkg += ".";
        }
        File[] files = directory.listFiles();
        if(files == null) {
        	return classes;
        }
        for (File file : files) {
            if (file.getName().endsWith(".class")) {
                String className = parentPkg + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (ClassNotFoundException ignore) {}
            } else if(file.isDirectory()) {
            	List<Class<?>> subClasses = getClassesFromPath(file.getAbsolutePath(), parentPkg + file.getName());
            	classes.addAll(subClasses);
            }
        }
        return classes;
    }
	
	private List<Class<?>> getClassesFromJar(String jarFilePath) throws IOException {
    	List<Class<?>> classes = new LinkedList<>();
        try(JarFile jarFile = new JarFile(jarFilePath)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                	if(IgnoredClass.isIgnored(entry.getName())) {
                		continue ;
                	}
                    try {
                    	String className = entry.getName().replace('/', '.').substring(0, entry.getName().length() - 6);
                        Class<?> clazz = Class.forName(className);
                        if(checkClass(clazz)) {
                            classes.add(clazz);
                        }
                    } catch (ClassNotFoundException | NoClassDefFoundError ignore) {}
                }
            }
        }
        return classes;
    }
	
	
	
	private boolean checkClass(Class<?> cls) {
		if(ForwardComponent.class.isAssignableFrom(cls) && !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers())) {
			return true;
		}

		boolean annotationed = false, isNormalClass = (!cls.isInterface() && !Modifier.isAbstract(cls.getModifiers()));
		Config config = cls.getAnnotation(Config.class);
		if(config != null) {
			annotationed = true;
			if(isNormalClass) {
				return true;
			}
		}
		
		Controller controller = cls.getAnnotation(Controller.class);
		if(controller != null) {
			annotationed = true;
			if(isNormalClass) {
				return true;
			}
		}
		
		Component component = cls.getAnnotation(Component.class);
		if(component != null) {
			annotationed = true;
			if(isNormalClass) {
				return true;
			}
		}

		Service service = cls.getAnnotation(Service.class);
		if(service != null) {
			annotationed = true;
			if(isNormalClass) {
				return true;
			}
		}
		
		if(annotationed) {
			throw new ArcherApplicationException("class '" + cls.getName() + "' must not be interface or abstract");
		}
		return false;
	}
}
