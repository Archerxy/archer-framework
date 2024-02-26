package com.archer.framework.web.filter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.archer.framework.base.component.ForwardComponent;
import com.archer.framework.base.util.ClassUtil;
import com.archer.net.http.HttpRequest;
import com.archer.net.http.HttpResponse;

public final class Filter implements ForwardComponent {
	
	private List<RequestFilter> reqFilters;

	private List<ResponseFilter> resFilters;

	private Map<Class<? extends Annotation>, AnnotationFilter> antFilters;
	
	private List<Object> allFilters;
	
	public Filter() {
		reqFilters = new ArrayList<>(16);
		resFilters = new ArrayList<>(16);
		antFilters = new HashMap<>(24);
		allFilters = new ArrayList<>(16 * 3);
	}
	

	@Override
	public List<Object> listForwardComponents(List<Class<?>> classes) {
		findAllFilters(classes);
		return allFilters;
	}

	public FilterState requestFilter(HttpRequest req, HttpResponse res) {
		for(RequestFilter filter: reqFilters) {
			if(FilterState.END.equals(filter.inputMessage(req, res))) {
				return FilterState.END;
			}
		}
		return FilterState.CONTINUE;
	}
	
	public FilterState responseFilter(HttpRequest req, HttpResponse res) {
		for(ResponseFilter filter: resFilters) {
			if(FilterState.END.equals(filter.outputMessage(req, res))) {
				return FilterState.END;
			}
		}
		return FilterState.CONTINUE;
	}
	
	public List<AnnotationFilter> getAnnotationFilters(List<Annotation> ants) {
		List<AnnotationFilter> filters = new ArrayList<>();
		for(Annotation ant: ants) {
			AnnotationFilter filter = antFilters.getOrDefault(ant.annotationType(), null);
			if(filter != null) {
				filters.add(filter);
			}
		}
		filters.sort((o1, o2) -> {
			return o1.priority() - o2.priority();
		});
		return filters;
	}
	
	private void findAllFilters(List<Class<?>> classes) {
		for(Class<?> cls: classes) {
			if(cls.getDeclaredAnnotation(com.archer.framework.web.annotation.Filter.class) == null) {
				continue ;
			}
			Object v = null;
			if(RequestFilter.class.isAssignableFrom(cls) && !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers())) {
				RequestFilter ins = (RequestFilter) ClassUtil.newInstance(cls);
				v = ins;
				reqFilters.add(ins);
			}
			if(ResponseFilter.class.isAssignableFrom(cls) && !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers())) {
				ResponseFilter ins = (ResponseFilter) ClassUtil.newInstance(cls);
				v = ins;
				resFilters.add(ins);
			}
			if(AnnotationFilter.class.isAssignableFrom(cls) && !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers())) {
				AnnotationFilter ins = (AnnotationFilter) ClassUtil.newInstance(cls);
				v = ins;
				antFilters.put(ins.getAnnotationType(), ins);
			}
			if(v != null) {
				allFilters.add(v);
			}
		}
		reqFilters.sort((o1, o2) -> {
			return o1.priority() - o2.priority();
		});
		resFilters.sort((o1, o2) -> {
			return o1.priority() - o2.priority();
		});
	}
	
	public List<Object> listFilters() {
		return allFilters;
	}
}
