package com.archer.framework.web.filter;

import com.archer.net.http.HttpRequest;
import com.archer.net.http.HttpResponse;

public interface ResponseFilter {

	FilterState onResponse(HttpRequest req, HttpResponse res, Object ret);
	
	int priority();
}
