package com.archer.framework.base.util;

import java.util.List;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;

import com.archer.framework.base.conf.Conf;
import com.archer.framework.base.exceptions.TypeException;
import com.archer.xjson.XJSON;

public class ValueUtil {
	private static final String BOOL_TYPE = "boolean";
	private static final String INT_TYPE = "int";
	private static final String LONG_TYPE = "long";
	private static final String FLOAT_TYPE = "float";
	private static final String DOUBLE_TYPE = "double";
	
	private static final XJSON json = new XJSON();
	
	public static boolean setValue(Field f, Conf conf, Object ins, String key, String def) 
			throws IllegalArgumentException, IllegalAccessException {
		Class<?> cls = f.getType();
		boolean primitive = false; 
		f.setAccessible(true);
		if(cls.equals(String.class)) {
			String v = conf.getString(key);
			if(v == null) {
				if(def == null) {
					return false;
				}
				v = def;
			}
			f.set(ins, v);
			return true;
		} else if((primitive = BOOL_TYPE.equals(cls.getName())) || cls.equals(Boolean.class)) {
			Boolean v = conf.getBoolean(key);
			if(v == null) {
				if(def == null) {
					return false;
				}
				v = Boolean.valueOf(def);
			}
			if(primitive) {
				f.set(ins, v.booleanValue());
			} else {
				f.set(ins, v);
			}
			return true;
		} else if((primitive = INT_TYPE.equals(cls.getName())) || cls.equals(Integer.class)) {
			Integer v = conf.getInteger(key);
			if(v == null) {
				if(def == null) {
					return false;
				}
				v = Integer.valueOf(def);
			}
			if(primitive) {
				f.set(ins, v.intValue());
			} else {
				f.set(ins, v);
			}
			return true;
		} else if((primitive = LONG_TYPE.equals(cls.getName())) || cls.equals(Long.class)) {
			Long v = conf.getLong(key);
			if(v == null) {
				if(def == null) {
					return false;
				}
				v = Long.valueOf(def);
			}
			if(primitive) {
				f.set(ins, v.longValue());
			} else {
				f.set(ins, v);
			}
			return true;
		} else if((primitive = FLOAT_TYPE.equals(cls.getName())) || cls.equals(Float.class)) {
			Float v = conf.getFloat(key);
			if(v == null) {
				if(def == null) {
					return false;
				}
				v = Float.valueOf(def);
			}
			if(primitive) {
				f.set(ins, v.floatValue());
			} else {
				f.set(ins, v);
			}
			return true;
		} else if((primitive = DOUBLE_TYPE.equals(cls.getName())) || cls.equals(Double.class)) {
			Double v = conf.getDouble(key);
			if(v == null) {
				if(def == null) {
					return false;
				}
				v = Double.valueOf(def);
			}
			if(primitive) {
				f.set(ins, v.doubleValue());
			} else {
				f.set(ins, v);
			}
			return true;
		} else if(List.class.isAssignableFrom(cls)) {
			Class<?> genericCls = (Class<?>) (((ParameterizedType)f.getGenericType()).getActualTypeArguments()[0]);
			Object v = null;
			if(genericCls.equals(String.class)) {
				v = conf.getStringList(key);
				if(v == null && def != null) {
					v = json.parseList(def, String.class);
				}
			} else if(genericCls.equals(Integer.class)) {
				v = conf.getIntegerList(key);
				if(v == null && def != null) {
					v = json.parseList(def, Integer.class);
				}
			} else if(genericCls.equals(Long.class)) {
				v = conf.getLongList(key);
				if(v == null && def != null) {
					v = json.parseList(def, Long.class);
				}
			} else if(genericCls.equals(Float.class)) {
				v = conf.getFloatList(key);
				if(v == null && def != null) {
					v = json.parseList(def, Float.class);
				}
			} else if(genericCls.equals(Double.class)) {
				v = conf.getDoubleList(key);
				if(v == null && def != null) {
					v = json.parseList(def, Double.class);
				}
			}
			if(v == null) {
				return false;
			}
			f.set(ins, v);
			return true;
		}
		throw new TypeException("can not set Value to type " + cls.getName() + 
				" at " + ins.getClass().getName() + "." + f.getName());
	}
}
