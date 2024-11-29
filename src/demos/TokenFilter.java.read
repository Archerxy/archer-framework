package com.archer.test.run;

import java.lang.annotation.Annotation;

import com.archer.framework.base.annotation.Log;
import com.archer.framework.web.filter.AnnotationRequestFilter;
import com.archer.framework.web.filter.FilterState;
import com.archer.log.Logger;
import com.archer.net.http.ContentType;
import com.archer.net.http.HttpRequest;
import com.archer.net.http.HttpResponse;
import com.archer.net.http.HttpStatus;

public class TokenFilter implements AnnotationRequestFilter {
	
	@Log
	Logger log;
	
	@Override
	public Class<? extends Annotation> getAnnotationType() {
		return Token.class;
	}

	@Override
	public FilterState onRequest(HttpRequest req, HttpResponse res) {
		if(req.getHeader("access-token") == null) {
			
			log.info("access denied cause access-token is null");
			
			res.setStatus(HttpStatus.UNAUTHORIZED);
			res.setContentType(ContentType.APPLICATION_JSON);
			res.setContent("{\"msg\":\"token is required\"}".getBytes());
			
			return FilterState.END;
		}
		return FilterState.CONTINUE;
	}

	@Override
	public int priority() {
		return 0;
	}
	
}
