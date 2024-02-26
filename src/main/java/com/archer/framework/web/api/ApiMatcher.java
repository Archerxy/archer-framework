package com.archer.framework.web.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.archer.framework.base.annotation.Controller;
import com.archer.framework.web.filter.Filter;
import com.archer.framework.web.filter.FilterState;
import com.archer.net.http.HttpRequest;
import com.archer.net.http.HttpResponse;;

public class ApiMatcher {

	private Map<String, Api> apis;
	
	private List<Api> pathApis;
	
	private Filter filter;
	
	public ApiMatcher(String prefixUri, List<Object> controllers, Filter filter) {
		this.apis = new HashMap<>(512);
		this.pathApis = new ArrayList<>(256);
		this.filter = filter;
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
			Api.findAndSaveApi(pathApis, apis, filter, cls.getDeclaredMethods(), prefixUri, obj);
		}
	}
	
	public ApiPathVal parseApi(String httpMethod, String uri) {
		String key = Api.toMappingKey(httpMethod, uri);
		Api api = apis.getOrDefault(key, null);
		if(api == null) {
			for(Api cApi: pathApis) {
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
		return this.filter.requestFilter(req, res);
	}
	
	public FilterState filterResponse(HttpRequest req, HttpResponse res) {
		return this.filter.responseFilter(req, res);
	}
}
