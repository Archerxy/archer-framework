//package com.archer.framework.base.util;
//
//import java.lang.reflect.GenericArrayType;
//import java.lang.reflect.Member;
//import java.lang.reflect.Method;
//import java.lang.reflect.Type;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//
//
//public class ParamerUtil {
//	
//	public static String[] getParameterNames(Method method) {
//		Method originalMethod = BridgeMethodResolver.findBridgedMethod(method);
//		Class<?> declaringClass = originalMethod.getDeclaringClass();
//		Map<Member, String[]> map = this.parameterNamesCache.get(declaringClass);
//		if (map == null) {
//			map = inspectClass(declaringClass);
//			this.parameterNamesCache.put(declaringClass, map);
//		}
//		if (map != NO_DEBUG_INFO_MAP) {
//			return map.get(originalMethod);
//		}
//		return null;
//	}
//	
//	public static Method findBridgedMethod(Method bridgeMethod) {
//		if (!bridgeMethod.isBridge()) {
//			return bridgeMethod;
//		}
//
//		// Gather all methods with matching name and parameter size.
//		List<Method> candidateMethods = new ArrayList<>();
//		Method[] methods = ReflectionUtil.getAllDeclaredMethods(bridgeMethod.getDeclaringClass());
//		for (Method candidateMethod : methods) {
//			if (isBridgedCandidateFor(candidateMethod, bridgeMethod)) {
//				candidateMethods.add(candidateMethod);
//			}
//		}
//
//		// Now perform simple quick check.
//		if (candidateMethods.size() == 1) {
//			return candidateMethods.get(0);
//		}
//
//		// Search for candidate match.
//		Method bridgedMethod = searchCandidates(candidateMethods, bridgeMethod);
//		if (bridgedMethod != null) {
//			// Bridged method found...
//			return bridgedMethod;
//		}
//		else {
//			// A bridge method was passed in but we couldn't find the bridged method.
//			// Let's proceed with the passed-in method and hope for the best...
//			return bridgeMethod;
//		}
//	}
//	
//	private static boolean isBridgedCandidateFor(Method candidateMethod, Method bridgeMethod) {
//		return (!candidateMethod.isBridge() && !candidateMethod.equals(bridgeMethod) &&
//				candidateMethod.getName().equals(bridgeMethod.getName()) &&
//				candidateMethod.getParameterCount() == bridgeMethod.getParameterCount());
//	}
//	
//	private static Method searchCandidates(List<Method> candidateMethods, Method bridgeMethod) {
//		if (candidateMethods.isEmpty()) {
//			return null;
//		}
//		Method previousMethod = null;
//		boolean sameSig = true;
//		for (Method candidateMethod : candidateMethods) {
//			if (isBridgeMethodFor(bridgeMethod, candidateMethod, bridgeMethod.getDeclaringClass())) {
//				return candidateMethod;
//			}
//			else if (previousMethod != null) {
//				sameSig = sameSig &&
//						Arrays.equals(candidateMethod.getGenericParameterTypes(), previousMethod.getGenericParameterTypes());
//			}
//			previousMethod = candidateMethod;
//		}
//		return (sameSig ? candidateMethods.get(0) : null);
//	}
//	
//	private static boolean isBridgeMethodFor(Method bridgeMethod, Method candidateMethod, Class<?> declaringClass) {
//		if (isResolvedTypeMatch(candidateMethod, bridgeMethod, declaringClass)) {
//			return true;
//		}
//		Method method = findGenericDeclaration(bridgeMethod);
//		return (method != null && isResolvedTypeMatch(method, candidateMethod, declaringClass));
//	}
//	
//	private static boolean isResolvedTypeMatch(Method genericMethod, Method candidateMethod, Class<?> declaringClass) {
//		Type[] genericParameters = genericMethod.getGenericParameterTypes();
//		Class<?>[] candidateParameters = candidateMethod.getParameterTypes();
//		if (genericParameters.length != candidateParameters.length) {
//			return false;
//		}
//		for (int i = 0; i < candidateParameters.length; i++) {
//			ResolvableType genericParameter = ResolvableType.forMethodParameter(genericMethod, i, declaringClass);
//			Class<?> candidateParameter = candidateParameters[i];
//			if (candidateParameter.isArray()) {
//				return false;
//			}
//			// A non-array type: compare the type itself.
//			if (!candidateParameter.equals(genericParameter.toClass())) {
//				return false;
//			}
//		}
//		return true;
//	}
//	
//	private static ResolvableType getComponentType(Method genericMethod, int i, Class<?> declaringClass) {
//		if (this.componentType != null) {
//			return this.componentType;
//		}
//		if (this.type instanceof Class) {
//			Class<?> componentType = ((Class<?>) this.type).getComponentType();
//			return forType(componentType, this.variableResolver);
//		}
//		if (this.type instanceof GenericArrayType) {
//			return forType(((GenericArrayType) this.type).getGenericComponentType(), this.variableResolver);
//		}
//		return resolveType().getComponentType();
//	}
//
//	private static Method findGenericDeclaration(Method bridgeMethod) {
//		// Search parent types for method that has same signature as bridge.
//		Class<?> superclass = bridgeMethod.getDeclaringClass().getSuperclass();
//		while (superclass != null && Object.class != superclass) {
//			Method method = searchForMatch(superclass, bridgeMethod);
//			if (method != null && !method.isBridge()) {
//				return method;
//			}
//			superclass = superclass.getSuperclass();
//		}
//
//		Class<?>[] interfaces = ClassUtils.getAllInterfacesForClass(bridgeMethod.getDeclaringClass());
//		return searchInterfaces(interfaces, bridgeMethod);
//	}
//
//}
