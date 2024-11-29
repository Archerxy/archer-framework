package com.archer.framework.web;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.archer.framework.base.annotation.Component;
import com.archer.framework.base.annotation.Inject;
import com.archer.framework.base.annotation.Log;
import com.archer.framework.base.annotation.Value;
import com.archer.framework.web.api.Api;
import com.archer.framework.web.api.ApiMatcher;
import com.archer.framework.web.api.ApiPathVal;
import com.archer.framework.web.exceptions.ParamException;
import com.archer.framework.web.filter.FilterState;
import com.archer.log.Logger;
import com.archer.net.http.ContentType;
import com.archer.net.http.HttpRequest;
import com.archer.net.http.HttpResponse;
import com.archer.net.http.HttpStatus;
import com.archer.net.http.HttpWrappedHandler;
import com.archer.xjson.XJSON;

@Component
public final class HttpHandler extends HttpWrappedHandler {
	
	private static final String DEFAULT_ENCODING = "utf-8";
	
	@Value(id = "archer.http.header", defaultVal = "[]")
	List<String> configHeaders;
	
	@Value(id = "archer.http.maxBody", defaultVal = "0")
	String maxBodyStr;
	
	@Inject
	ApiMatcher matcher;
	
	@Inject
	XJSON xjson;
	
	@Log
	Logger log;
	
	Map<String, String> responseHeaders = new HashMap<>();
	
	int maxBody = -1;

	@Override
	public void handle(HttpRequest req, HttpResponse res) throws Exception {
		res.setContentEncoding(DEFAULT_ENCODING);
		ApiPathVal apiPatch = matcher.parseApi(req.getMethod(), req.getUri());
		if(apiPatch == null) {
			responseNotFound(res);
			return ;
		}
		if(maxBody < 0) {
			parseMaxBody();
		}
		if(maxBody > 0 && req.getContentLength() > maxBody) {
			responseBadRequest(res, "request body length overflow");
			return ;
		}
		if(FilterState.END.equals(matcher.filterRequest(req, res))) {
			return ;
		}
		Api api = apiPatch.getApi();
		if(FilterState.END.equals(api.doFilter(req, res))) {
			return ;
		}
		
		Object ret;
		try {
			ret = api.invoke(req, res, apiPatch.getPathVals(), req.getQueryParams(), xjson);
			res.setHeader("content-type", api.getResContentType());
		} catch(Exception e) {
			if(e instanceof ParamException) {
				responseBadRequest(res, "Invalid params");
			} else {
				responseBadRequest(res, "Internal Server Error");
			}
			e.printStackTrace();
			return ;
		}
		
		if(FilterState.END.equals(api.doFilter(req, res, ret))) {
			return ;
		}
		if(FilterState.END.equals(matcher.filterResponse(req, res, ret))) {
			return ;
		}
		
		setResponseHeaders(res);
		
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
	
	private void responseBadRequest(HttpResponse res, String reason) {
		String body = "{" +
				"\"server\": \"Archer Http Server\"," +
				"\"time\": \"" + LocalDateTime.now().toString() + "\"," +
				"\"status\": \"" + HttpStatus.BAD_REQUEST + "\"," +
				"\"reason\": \"" + reason + "\"" +
			"}";
		res.setStatus(HttpStatus.NOT_FOUND);
		res.setContentType(ContentType.APPLICATION_JSON);
		res.setContent(body.getBytes());
	}
	
	private void setResponseHeaders(HttpResponse res) {
		if(responseHeaders.size() == 0) {
			parseHeaders();
		}
		for(Map.Entry<String, String> entry: responseHeaders.entrySet()) {
			res.setHeader(entry.getKey(), entry.getValue());
		}
	} 
	
	private synchronized void parseHeaders() {
		if(responseHeaders.size() == 0) {
			for(String s: configHeaders) {
				int i = s.indexOf('=');
				if(i < 0) {
					log.warn("can not parse header config '{}'", s);
					continue ;
				}
				responseHeaders.put(s.substring(0, i), s.substring(i+1));
			}
		}
	}
	
	private synchronized void parseMaxBody() {
		if(maxBody < 0) {
			int length = maxBodyStr.length(), off = length - 1;
			try {
				if(maxBodyStr.charAt(off) == 'M') {
					maxBody = Integer.parseInt(maxBodyStr.substring(0,  off)) * 1024 * 1024;
				} else if(maxBodyStr.charAt(off) == 'K') {
					maxBody = Integer.parseInt(maxBodyStr.substring(0,  off)) * 1024;
				} else {
					maxBody = Integer.parseInt(maxBodyStr);
				}
			} catch(Exception ignore) {
				log.warn("can not parse archer.http.max-body '{}'", maxBodyStr);
				maxBody = 0;
			}
		}
	}
}
