package com.archer.framework.web.filter;

import java.lang.annotation.Annotation;

import com.archer.net.http.HttpRequest;
import com.archer.net.http.HttpResponse;

public interface AnnotationFilter {
	
	FilterState inputMessage(HttpRequest req, HttpResponse res);
	
	Class<? extends Annotation> getAnnotationType();
	
	int priority();
}
