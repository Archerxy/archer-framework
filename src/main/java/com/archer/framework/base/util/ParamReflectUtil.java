package com.archer.framework.base.util;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import com.archer.framework.base.exceptions.TypeException;

public class ParamReflectUtil {
	

	static final String BOOL_TYPE = "boolean";
	static final String BYTE_TYPE = "byte";
	static final String CHAR_TYPE = "char";
	static final String SHORT_TYPE = "short";
	static final String INT_TYPE = "int";
	static final String LONG_TYPE = "long";
	static final String FLOAT_TYPE = "float";
	static final String DOUBLE_TYPE = "double";
	

	static final DateFormat DEFAULT_DATE_FORMAT = 
			new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static final DateTimeFormatter DEFAULT_TIME_FORMAT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public static Object reflectFromJavaType(Type javaType, String val) {
		Class<?> cls = (Class<?>) javaType;
		if(cls.isPrimitive()) {
			return reflectToPrimitive(cls, val);
		} else {
			return reflectToPopularClass(cls, val);
		}
	}
	
	public static Object reflectToPrimitive(Class<?> cls,  String val) {
		if(BOOL_TYPE.equals(cls.getName())) {
			return Boolean.getBoolean(val);
		} else if(BYTE_TYPE.equals(cls.getName())) {
			return Byte.parseByte(val);
		} else if(CHAR_TYPE.equals(cls.getName())) {
			char[] chars = val.toCharArray();
			if(chars.length != 1) {
				throw new TypeException("can not parse " + val + " to char.");
			}
			return chars[0];
		} else if(SHORT_TYPE.equals(cls.getName())) {
			return Short.parseShort(val);
		} else if(INT_TYPE.equals(cls.getName())) {
			return Integer.parseInt(val);
		} else if(LONG_TYPE.equals(cls.getName())) {
			return Long.parseLong(val);
		} else if(FLOAT_TYPE.equals(cls.getName())) {
			return Float.parseFloat(val);
		} else if(DOUBLE_TYPE.equals(cls.getName())) {
			return Double.parseDouble(val);
		}
		throw new TypeException("unknown primitive type '" 
				+ cls.getName() + "'");
	}
	
	public static Object reflectToPopularClass(Class<?> cls,  String val) {
		if(cls.equals(String.class)) {
			return val;
		}
		if(cls.equals(Byte.class)) {
			return Byte.valueOf(val);
		}
		if(cls.equals(Character.class)) {
			char[] chars = val.toCharArray();
			if(chars.length != 1) {
				throw new TypeException("can not parse " + val + " to char.");
			}
			return new Character(chars[0]);
		}
		if(cls.equals(Integer.class)) {
			return Integer.valueOf(val);
		}
		if(cls.equals(Long.class)) {
			return Long.valueOf(val);
		}
		if(cls.equals(Float.class)) {
			return Float.valueOf(val);
		}
		if(cls.equals(Double.class)) {
			return Double.valueOf(val);
		}
		if(Date.class.isAssignableFrom(cls)) {
			try {
				return DEFAULT_DATE_FORMAT.parse(val);
			} catch(Exception ignore) {
				throw new TypeException("can not parse '"+
						val+"' to Date");
			}
		} else if(cls.equals(LocalDate.class)) {
			try {
				return LocalDate.parse(val, DEFAULT_TIME_FORMAT);
			} catch(Exception ignore) {
				throw new TypeException("can not parse '"+
						val+"' to LocalDate");
			}
		} else if(cls.equals(LocalTime.class)) {
			try {
				return LocalTime.parse(val, DEFAULT_TIME_FORMAT);
			} catch(Exception ignore) {
				throw new TypeException("can not parse '"+
						val+"' to LocalTime");
			}
		} else if(cls.equals(LocalDateTime.class)) {
			try {
				return LocalDateTime.parse((String)val, DEFAULT_TIME_FORMAT);
			} catch(Exception ignore) {
				throw new TypeException("can not parse '"+
						val+"' to LocalDateTime");
			}
		} else if(cls.equals(BigInteger.class)) {
			try {
				return new BigInteger(val);
			} catch(Exception ignore) {
				throw new TypeException("can not parse '"+
						val+"' to BigInteger");
			}
		} else if(cls.equals(BigDecimal.class)) {
			try {
				return new BigDecimal(val);
			} catch(Exception ignore) {
				throw new TypeException("can not parse '"+
						val+"' to BigDecimal");
			}
		}
		return val;
	}
	
}
