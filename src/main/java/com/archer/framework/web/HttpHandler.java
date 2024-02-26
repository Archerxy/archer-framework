package com.archer.framework.web;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import com.archer.framework.base.annotation.Component;
import com.archer.framework.base.annotation.Inject;
import com.archer.framework.web.api.Api;
import com.archer.framework.web.api.ApiMatcher;
import com.archer.framework.web.api.ApiPathVal;
import com.archer.framework.web.filter.FilterState;
import com.archer.net.http.ContentType;
import com.archer.net.http.HttpRequest;
import com.archer.net.http.HttpResponse;
import com.archer.net.http.HttpStatus;
import com.archer.net.http.HttpWrappedHandler;
import com.archer.xjson.XJSON;

@Component
public final class HttpHandler extends HttpWrappedHandler {
	
	private static final String DEFAULT_ENCODING = "utf-8";
	
	@Inject
	ApiMatcher matcher;
	
	@Inject
	XJSON xjson;

	@Override
	public void handle(HttpRequest req, HttpResponse res) throws Exception {
		res.setContentEncoding(DEFAULT_ENCODING);
		ApiPathVal apiPatch = matcher.parseApi(req.getMethod(), req.getUri());
		if(apiPatch == null) {
			responseNotFound(res);
			return ;
		}
		if(FilterState.END.equals(matcher.filterRequest(req, res))) {
			return ;
		}
		
		Api api = apiPatch.getApi();
		if(FilterState.END.equals(api.filterAnnotation(req, res))) {
			return ;
		}
		Object ret = api.invoke(req, res, apiPatch.getPathVals(), req.getQueryParams(), xjson);
		res.setHeader("content-type", api.getResContentType());
		
		if(FilterState.END.equals(matcher.filterResponse(req, res))) {
			return ;
		}

		if(ret instanceof String) {
			res.setContent(((String)ret).getBytes(StandardCharsets.UTF_8));
		} else {
			res.setContent(xjson.stringify(ret).getBytes(StandardCharsets.UTF_8));
		}
	}

	@Override
	public void handleException(HttpRequest req, HttpResponse res, Throwable t) {
		t.printStackTrace();
		
		if(res.getStatus() == null) {
			String body = "{" +
					"\"server\": \"Archer Http Server\"," +
					"\"time\": \"" + LocalDateTime.now().toString() + "\"," +
					"\"status\": \"" + HttpStatus.SERVICE_UNAVAILABLE.getStatus() + "\"" +
				"}";
			
			res.setStatus(HttpStatus.SERVICE_UNAVAILABLE);
			res.setContentType(ContentType.APPLICATION_JSON);
			res.setContent(body.getBytes());
		}
	}
	
	private void responseNotFound(HttpResponse res) {
		String body = "{" +
				"\"server\": \"Archer Http Server\"," +
				"\"time\": \"" + LocalDateTime.now().toString() + "\"," +
				"\"status\": \"" + HttpStatus.NOT_FOUND.getStatus() + "\"" +
			"}";
		res.setStatus(HttpStatus.NOT_FOUND);
		res.setContentType(ContentType.APPLICATION_JSON);
		res.setContent(body.getBytes());
	}
}
