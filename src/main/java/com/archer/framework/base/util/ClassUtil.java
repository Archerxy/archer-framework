package com.archer.framework.base.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.archer.framework.base.annotation.ConstructorParam;
import com.archer.framework.base.async.AsyncProxy;
import com.archer.framework.base.component.ContainerInstance;
import com.archer.framework.base.exceptions.ArcherApplicationException;
import com.archer.framework.base.exceptions.TypeException;
import com.archer.net.Bytes;
import com.archer.tools.bytecode.ClassBytecode;

/**
 * @author xuyi
 */
public class ClassUtil {

    private static final String JAR = ".jar";
    private static final String CLASS = ".class";
    private static final char DOT = '.';
	public static final String CLASS_FILE_SUFFIX = ".class";
	
	private static HashMap<Class<?>, InnerConstructor> classConstructor = new HashMap<>();

    public static List<Class<?>> findImplementsClass(Class<?> clazz) {
        List<Class<?>> collected = new LinkedList<>();
        String root = PathUtil.getCurrentWorkDir();
        recursePath(new File(root), clazz, collected);
        return collected;
    }

    @SuppressWarnings("resource")
	private static void recursePath(File path, Class<?> clazz, List<Class<?>> classList) {
        if(path.isDirectory()) {
            File[] subPaths = path.listFiles();
            if(subPaths == null) {
                return ;
            }
            for(File subPath: subPaths) {
                recursePath(subPath, clazz, classList);
            }
        }
        if(path.getName().endsWith(JAR)) {
            try {
                JarFile jar = new JarFile(path.getAbsolutePath());
                Enumeration<JarEntry> entries = jar.entries();
                while(entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if(entry.getName().endsWith(CLASS)) {
                        collectClass(entry.getName(), clazz, classList);
                    }
                }
            } catch (IOException ignore) {}
        }
        if(path.getName().endsWith(CLASS)) {
            collectClass(path.getAbsolutePath(), clazz, classList);
        }
    }

    private static void collectClass(String className, Class<?> clazz, List<Class<?>> classList) {
        String classPath = PathUtil.getClassPath();
        if(className.startsWith(classPath)) {
            className = className.replace(classPath, "");
        }
        className = className.replace(File.separatorChar, DOT);
        className = className.replace(CLASS, "");
        try {
            Class<?> implClass = Class.forName(className);
            if(clazz.isAssignableFrom(implClass)
                    && !implClass.isInterface()
                    && !Modifier.isAbstract(implClass.getModifiers())) {
                classList.add(implClass);
            }
        } catch (ClassNotFoundException ignore) {}
    }
    
    public static String getClassFileName(Class<?> clazz) {
		String className = clazz.getName();
		int lastDotIndex = className.lastIndexOf(DOT);
		return className.substring(lastDotIndex + 1) + CLASS_FILE_SUFFIX;
	}
    public static Object newInstance(Class<?> cls) {
		InnerConstructor innerConstructor = classConstructor.getOrDefault(cls, null);
		if(innerConstructor != null) {
			return innerConstructor.newInstance();
		}
		Constructor<?>[] constructors = cls.getConstructors();
		for(Constructor<?> constructor : constructors) {
			Object[] params = null;
			if(constructor.getParameterCount() == 0) {
				params = new Object[0];
			} else if(cls.isMemberClass() && constructor.getParameterCount() == 1) { //non-static inner class
				params = new Object[1];
			}
			if(params != null) {
				innerConstructor = new InnerConstructor(cls, constructor, params);
				classConstructor.put(cls, innerConstructor);
				try {
					constructor.setAccessible(true);
					return constructor.newInstance(params);
				} catch (Exception e) {
					throw new ArcherApplicationException("can not construct class '" +
							cls.getName() + "'", e);
				}
			}
		}
		throw new ArcherApplicationException(
				"no arguments constructor is required with class '" 
				+ cls.getName() + "'"); 
	}
    

    public static ContainerInstance tryNewInstance(Class<?> cls) {
		InnerConstructor innerConstructor = classConstructor.getOrDefault(cls, null);
		if(innerConstructor != null) {
			return new ContainerInstance(innerConstructor.newInstance());
		}
		Constructor<?>[] constructors = cls.getConstructors();
		Parameter[] theParams = null;
		Constructor<?> theCons = null;
		boolean ok = false;
		for(Constructor<?> constructor : constructors) {
			Object[] params = null;
			if(constructor.getParameterCount() == 0) {
				params = new Object[0];
				innerConstructor = new InnerConstructor(cls, constructor, params);
				classConstructor.put(cls, innerConstructor);
				try {
					constructor.setAccessible(true);
					return new ContainerInstance(constructor.newInstance(params));
				} catch (Exception e) {
					throw new ArcherApplicationException("can not construct class '" +
							cls.getName() + "'", e);
				}
			}
			ok = true;
			for(Parameter p: constructor.getParameters()) {
				ConstructorParam anp = p.getAnnotation(ConstructorParam.class);
				if(anp == null) {
					ok = false;
					break ;
				}
			}
			if(ok) {
				if(theCons != null) {
					throw new ArcherApplicationException("duplicate constructors in class '" + cls.getName() + "'");
				}
				theCons = constructor;
				theParams = constructor.getParameters();
			}
		}
		if(theCons != null && theParams != null) {
			return new ContainerInstance(cls, theCons, theParams);
		}
		throw new ArcherApplicationException("can not found proper constructor in class '" +
				cls.getName() + "'");
	}
	

    public static ContainerInstance tryNewProxyInstance(Class<?> cls) {
    	String className = replaceDot2Slash(cls.getName());
		try(InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(className + ".class")) {
			Bytes rawClass = new Bytes();
			byte[] buf = new byte[1024];
			int off = 0;
			while((off = in.read(buf)) >= 0) {
				rawClass.write(buf, 0, off);
			}
			ClassBytecode rawBytecode = new ClassBytecode();
			rawBytecode.decodeClassBytes(rawClass);
			
			AsyncProxy async = new AsyncProxy(rawBytecode);
			Class<?> proxyClass = async.newAsyncClass();
			
			InnerConstructor innerConstructor = classConstructor.getOrDefault(proxyClass, null);
			if(innerConstructor != null) {
				return new ContainerInstance(cls, innerConstructor.newInstance(), proxyClass);
			}

			Constructor<?>[] oldConstructors = cls.getConstructors();
			Constructor<?>[] constructors = proxyClass.getConstructors();
			Parameter[] theParams = null;
			Constructor<?> theCons = null;
			boolean ok = false;
			for(int i = 0; i < constructors.length; i++) {
				Constructor<?> constructor = constructors[i];
				Object[] params = null;
				if(constructor.getParameterCount() == 0) {
					params = new Object[0];
					innerConstructor = new InnerConstructor(proxyClass, constructor, params);
					classConstructor.put(proxyClass, innerConstructor);
					try {
						constructor.setAccessible(true);
						return new ContainerInstance(cls, constructor.newInstance(params), proxyClass);
					} catch (Exception e) {
						throw new ArcherApplicationException("can not construct class '" +
								cls.getName() + "'", e);
					}
				}
				ok = true;
				Constructor<?> oldConstructor = oldConstructors[i];
				for(Parameter p: oldConstructor.getParameters()) {
					ConstructorParam anp = p.getAnnotation(ConstructorParam.class);
					if(anp == null) {
						ok = false;
						break ;
					}
				}
				if(ok) {
					if(theCons != null) {
						throw new ArcherApplicationException("duplicate constructors in class '" + cls.getName() + "'");
					}
					theCons = constructor;
					theParams = constructor.getParameters();
				}
			}
			if(theCons != null && theParams != null) {
				return new ContainerInstance(cls, theCons, theParams, proxyClass);
			}
			throw new ArcherApplicationException("can not found proper constructor in class '" +
					cls.getName() + "'");
			
		} catch (Exception e) {
			throw new TypeException(e);
		}
	}
    
    public static String replaceDot2Slash(String name) {
    	byte[] bs = name.getBytes();
    	for(int i = 0; i < bs.length; i++) {
    		if('.' == bs[i]) {
    			bs[i] = '/';
    		}
    	}
    	return new String(bs);
    }
	
	
	static class InnerConstructor {
		
		Class<?> cls;
		
		Constructor<?> constructor;
		
		Object[] params;

		public InnerConstructor(Class<?> cls, Constructor<?> constructor, Object[] params) {
			this.cls = cls;
			this.constructor = constructor;
			this.params = params;
		}
		
		public Object newInstance() {
			try {
				return constructor.newInstance(params);
			} catch (Exception e) {
				throw new RuntimeException("can not construct class '" +
						cls.getName() + "'");
			}
		}
	}
}

