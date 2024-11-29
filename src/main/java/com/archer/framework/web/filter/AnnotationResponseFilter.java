package com.archer.framework.web.filter;

import java.lang.annotation.Annotation;

import com.archer.net.http.HttpRequest;
import com.archer.net.http.HttpResponse;

public interface AnnotationResponseFilter {

	FilterState onResponse(HttpRequest req, HttpResponse res, Object ret);
	
	Class<? extends Annotation> getAnnotationType();
	
	int priority();
}
