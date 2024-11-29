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

public final class FilterForwardComponnet implements ForwardComponent {

	private List<RequestFilter> reqFilters;
	private List<ResponseFilter> resFilters;

	private Map<Class<? extends Annotation>, AnnotationRequestFilter> antReqFilters;
	private Map<Class<? extends Annotation>, AnnotationResponseFilter> antResFilters;
	
	private List<Object> allFilters;
	
	public FilterForwardComponnet() {
		antReqFilters = new HashMap<>(16);
		reqFilters = new ArrayList<>(16);
		antResFilters = new HashMap<>(16);
		resFilters = new ArrayList<>(16);
		allFilters = new ArrayList<>(16 * 3);
	}
	

	@Override
	public List<Object> listForwardComponents(List<Class<?>> classes) {
		findAllFilters(classes);
		return allFilters;
	}
	
	public FilterState doFilter(HttpRequest req, HttpResponse res) {
		for(RequestFilter filter: reqFilters) {
			if(FilterState.END.equals(filter.onRequest(req, res))) {
				return FilterState.END;
			}
		}
		return FilterState.CONTINUE;
	}
	
	public FilterState doFilter(HttpRequest req, HttpResponse res, Object ret) {
		for(ResponseFilter filter: resFilters) {
			if(FilterState.END.equals(filter.onResponse(req, res, ret))) {
				return FilterState.END;
			}
		}
		return FilterState.CONTINUE;
	}
	
	private void findAllFilters(List<Class<?>> classes) {
		for(Class<?> cls: classes) {
			Object v = null;
			if(AnnotationRequestFilter.class.isAssignableFrom(cls) && !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers())) {
				AnnotationRequestFilter ins = (AnnotationRequestFilter) ClassUtil.newInstance(cls);
				v = ins;
				antReqFilters.put(ins.getAnnotationType(), ins);
			}
			if(AnnotationResponseFilter.class.isAssignableFrom(cls) && !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers())) {
				AnnotationResponseFilter ins = (AnnotationResponseFilter) ClassUtil.newInstance(cls);
				v = ins;
				antResFilters.put(ins.getAnnotationType(), ins);
			}
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


	public List<RequestFilter> getReqFilters() {
		return reqFilters;
	}


	public List<ResponseFilter> getResFilters() {
		return resFilters;
	}


	public Map<Class<? extends Annotation>, AnnotationRequestFilter> getAntReqFilters() {
		return antReqFilters;
	}


	public Map<Class<? extends Annotation>, AnnotationResponseFilter> getAntResFilters() {
		return antResFilters;
	}
	
}
