package com.archer.framework.web.filter;

import com.archer.net.http.HttpRequest;
import com.archer.net.http.HttpResponse;

public interface ResponseFilter {
	
	FilterState outputMessage(HttpRequest req, HttpResponse res);

	int priority();
}
