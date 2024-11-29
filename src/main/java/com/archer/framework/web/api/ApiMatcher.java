package com.archer.framework.web.api;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.archer.framework.base.annotation.Controller;
import com.archer.framework.web.filter.AnnotationRequestFilter;
import com.archer.framework.web.filter.AnnotationResponseFilter;
import com.archer.framework.web.filter.FilterForwardComponnet;
import com.archer.framework.web.filter.FilterState;
import com.archer.net.http.HttpRequest;
import com.archer.net.http.HttpResponse;;

public class ApiMatcher {

	private Map<String, Api> apiMap;
	
	private List<Api> apis;
	
	private FilterForwardComponnet filterForward;
	
	public ApiMatcher(String prefixUri, List<Object> controllers, FilterForwardComponnet filter) {
		this.apiMap = new HashMap<>(512);
		this.apis = new ArrayList<>(256);
		this.filterForward = filter;
		if(null == prefixUri || prefixUri.isEmpty()) {
			prefixUri = "/";
		} else if (prefixUri.charAt(0) != '/') {
			prefixUri = "/" + prefixUri;
		}
		
		for(Object obj: controllers) {
			Class<?> cls = obj.getClass();
			Controller c = cls.getAnnotation(Controller.class);
			String apiPrefixUri = c.prefix();
			if(!apiPrefixUri.isEmpty()) {
				if(apiPrefixUri.charAt(0) == '/') {
					if(prefixUri.charAt(prefixUri.length() - 1) == '/') {
						prefixUri = prefixUri.substring(prefixUri.length() - 1);
					}
					prefixUri += apiPrefixUri;
				} else {
					prefixUri += "/" + apiPrefixUri;
				}
			}
			Api.findAndSaveApi(apis, apiMap, cls.getDeclaredMethods(), prefixUri, obj);
		}
		
		associateApiWithAnnotationFilter();
	}
	
	public ApiPathVal parseApi(String httpMethod, String uri) {
		String key = Api.toMappingKey(httpMethod, uri);
		Api api = apiMap.getOrDefault(key, null);
		if(api == null) {
			for(Api cApi: apis) {
				String[] uriSegs = uri.split("/");
				String[] cApiSegs = cApi.uriSegments();
				if(uriSegs.length != cApiSegs.length) {
					continue;
				}
				
				boolean ok = true;
				String[] pathVals = new String[cApi.pathValCount()];
				int pathValOff = 0;
				for(int i = 0; i < cApiSegs.length; i++) {
					if(Api.CN_PATTERN.equals(cApiSegs[i])) {
						pathVals[pathValOff++] = uriSegs[i];
						continue;
					}
					if(!cApiSegs[i].equals(uriSegs[i])) {
						ok = false;
						break;
					}
				}
				if(ok) {
					return new ApiPathVal(cApi, pathVals);
				}
			}
		}
		
		return api == null ? null : new ApiPathVal(api, new String[0]);
	}
	
	public FilterState filterRequest(HttpRequest req, HttpResponse res) {
		return this.filterForward.doFilter(req, res);
	}
	public FilterState filterResponse(HttpRequest req, HttpResponse res, Object data) {
		return this.filterForward.doFilter(req, res, data);
	}
	
	private void associateApiWithAnnotationFilter() {
		Map<Class<? extends Annotation>, AnnotationRequestFilter> antReqFilters = filterForward.getAntReqFilters();
		Map<Class<? extends Annotation>, AnnotationResponseFilter> antResFilters = filterForward.getAntResFilters();
		
		for(Map.Entry<Class<? extends Annotation>, AnnotationRequestFilter> entry : antReqFilters.entrySet()) {
			for(Api api: apis) {
				if(api.isBeforeOption()) {
					continue;
				}
				Annotation ant = api.getMethod().getAnnotation(entry.getKey());
				if(ant != null) {
					api.addResquestFilter(entry.getValue());
				}
			}
		}
		for(Map.Entry<Class<? extends Annotation>, AnnotationResponseFilter> entry : antResFilters.entrySet()) {
			for(Api api: apis) {
				if(api.isBeforeOption()) {
					continue;
				}
				Annotation ant = api.getMethod().getAnnotation(entry.getKey());
				if(ant != null) {
					api.addResponseFilter(entry.getValue());
				}
			}
		}
		for(Api api: apis) {
			api.sortAnnotationFilters();
		}
	}
}
