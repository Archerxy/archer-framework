package com.archer.framework.web.api;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.archer.framework.base.exceptions.ContainerException;
import com.archer.framework.base.exceptions.TypeException;
import com.archer.framework.base.util.ParamReflectUtil;
import com.archer.framework.web.annotation.BodyParam;
import com.archer.framework.web.annotation.Delete;
import com.archer.framework.web.annotation.Get;
import com.archer.framework.web.annotation.Option;
import com.archer.framework.web.annotation.PathParam;
import com.archer.framework.web.annotation.Post;
import com.archer.framework.web.annotation.Put;
import com.archer.framework.web.annotation.QueryParam;
import com.archer.framework.web.exceptions.ApiException;
import com.archer.framework.web.exceptions.ParamException;
import com.archer.framework.web.filter.AnnotationFilter;
import com.archer.framework.web.filter.Filter;
import com.archer.framework.web.filter.FilterState;
import com.archer.framework.web.util.MultipartUtil;
import com.archer.net.http.ContentType;
import com.archer.net.http.HttpRequest;
import com.archer.net.http.HttpResponse;
import com.archer.net.http.multipart.Multipart;
import com.archer.net.http.multipart.MultipartParser;
import com.archer.xjson.XJSON;
import com.archer.xjson.XJSONException;

public class Api {

	static final String GET = "GET";
	static final String POST = "POST";
	static final String PUT = "PUT";
	static final String DELETE = "DELETE";
	static final String OPTION = "OPTION";
	
	public static final String OPTION_RES = "{\"OPTION\":\"ok\"}";
	
	public static final String CN_PATTERN = "*";
	
	
	public static void findAndSaveApi(List<Api> pathApis, Map<String, Api> apis, Filter filter, 
			Method[] methods, String prefixUri, Object instance) {
		for(Method m: methods) {
			String uri = null, httpMethod = null, reqContentType = null, resContentType= null;
			Get get = m.getDeclaredAnnotation(Get.class);
			if(get != null) {
				uri = get.pattern();
				httpMethod = GET;
				reqContentType = get.reqContentType();
				resContentType = get.resContentType();
			}
			
			Post post = m.getDeclaredAnnotation(Post.class);
			if(post != null) {
				if(httpMethod != null) {
					throw new ApiException("duplicated http method " + httpMethod + " " + POST + 
							" at " + instance.getClass().getName() + "." + m.getName());
				}
				uri = post.pattern();
				httpMethod = POST;
				reqContentType = post.reqContentType();
				resContentType = post.resContentType();
			}
			
			Put put = m.getDeclaredAnnotation(Put.class);
			if(put != null) {
				if(httpMethod != null) {
					throw new ApiException("duplicated http method " + httpMethod + " " + PUT + 
							" at " + instance.getClass().getName() + "." + m.getName());
				}
				uri = put.pattern();
				httpMethod = PUT;
				reqContentType = put.reqContentType();
				resContentType = put.resContentType();
			}
			
			Delete delete = m.getDeclaredAnnotation(Delete.class);
			if(delete != null) {
				if(httpMethod != null) {
					throw new ApiException("duplicated http method " + httpMethod + " " + DELETE + 
							" at " + instance.getClass().getName() + "." + m.getName());
				}
				uri = delete.pattern();
				httpMethod = DELETE;
				reqContentType = delete.reqContentType();
				resContentType = delete.resContentType();
			}
			
			Option option = m.getDeclaredAnnotation(Option.class);
			if(option != null) {
				if(httpMethod != null) {
					throw new ApiException("duplicated http method " + httpMethod + " " + OPTION + 
							" at " + instance.getClass().getName() + "." + m.getName());
				}
				uri = option.pattern();
				httpMethod = OPTION;
				reqContentType = option.reqContentType();
				resContentType = option.resContentType();
			} 
			
			if(null != uri) {
				if(!prefixUri.isEmpty()) {
					if(prefixUri.charAt(prefixUri.length() - 1) == '/') {
						if(uri.charAt(0) == '/') {
							uri = prefixUri + uri.substring(1);
						} else {
							uri = prefixUri + uri;
						}
					} else {
						if(uri.charAt(0) == '/') {
							uri = prefixUri + uri;
						} else {
							uri = prefixUri + "/" + uri;
						}
					}
				}
				Api api = new Api(instance, m, filter, httpMethod, uri, reqContentType, resContentType);
				if(api.isPathApi()) {
					pathApis.add(api);
				} else {
					apis.put(api.getMappingKey(), api);
				}
				
				Api opApi = new Api(uri);
				if(opApi.isPathApi()) {
					pathApis.add(opApi);
				} else {
					apis.put(opApi.getMappingKey(), opApi);
				}
			}
		}
	}
	
	static String toMappingKey(String httpMethod, String uri) {
		return httpMethod + ":" + uri;
	}
	
	Object instance;
	
	Method method;
	
	String httpMethod;
	
	String rawUri;
	
	String uri;
	
	boolean pathApi;
	
	String[] uriSegments;
	
	String reqContentType;
	
	String resContentType;
	
	int paramsCount;
	
	boolean hasPathVar;
	
	int pathValCount = 0;
	
	boolean beforeOption;
	
	ArrayList<RequestParam> pathParams;
	
	RequestParam bodyParam;
	
	RequestParam[] orderedParams;
	
	List<AnnotationFilter> antFilters;
	
	Api(String uri) {
		this(null, null, null, OPTION, uri, 
				ContentType.APPLICATION_JSON.getName(), 
				ContentType.APPLICATION_JSON.getName());
		this.beforeOption = true;
	}
	
	Api(Object instance, Method method, Filter filter,
			String httpMethod, String uri, String reqContentType,
			String resContentType) {
		super();
		this.instance = instance;
		this.method = method;
		this.httpMethod = httpMethod;
		this.rawUri = uri;
		this.uri = uri;
		this.reqContentType = reqContentType;
		this.resContentType = resContentType;
		this.pathParams = new ArrayList<>(8);
		
		parseUri();
		if(method != null) {
			this.method.setAccessible(true);
			this.paramsCount = method.getParameterCount();
			this.orderedParams = new RequestParam[paramsCount];
			ArrayList<Annotation> annotations = new ArrayList<>(16);
			for(Annotation ant: this.method.getDeclaredAnnotations()) {
				if(ant instanceof Get || ant instanceof Post || ant instanceof Put || 
						ant instanceof Delete || ant instanceof Option) {
					continue;
				}
				annotations.add(ant);
			}
			this.antFilters = filter.getAnnotationFilters(annotations);
			
			parseParam();
		}
	}
	
	private void parseUri() {
		int off = 0, s = 0, e = 0;
		while((s = uri.indexOf("/{", off)) >= 0 && (e = uri.indexOf("}/", off)) > 0)  {
			if(e <= s) {
				throw new ContainerException("can not parse uri " + uri);
			}
			this.pathApi = true;
			this.pathValCount++;
			String k = uri.substring(s + 2, e);
			this.pathParams.add(new RequestParam(RequestParam.PATH, k));
			uri = uri.substring(0, s + 1) + CN_PATTERN + uri.substring(e + 1);
			off += s + 5;
		}
		this.uriSegments = this.uri.split("/");
	}
	
	private void parseParam() {
		int idx = 0;
		for(Parameter p: method.getParameters()) {
			PathParam pathVar = p.getAnnotation(PathParam.class);
			if(pathVar != null) {
				boolean ok = false;
				for(RequestParam var: pathParams) {
					if(pathVar.name().equals(var.k)) {
						ok = true;
						var.setParam(p);
						orderedParams[idx++] = var;
						break ;
					}
				}
				if(!ok) {
					throw new ContainerException("invalid path variable " + pathVar.name());
				}
				continue ;
			}
			
			QueryParam queryVar = p.getAnnotation(QueryParam.class);
			if(queryVar != null) {
				String name = queryVar.name();
				orderedParams[idx++] = new RequestParam(RequestParam.QUERY, name, p);
				continue;
			}
			if(this.bodyParam != null) {
				throw new ContainerException("duplicated body variable at " + 
						instance.getClass().getName() + "." + method.getName());
			}
			String name = p.getName();
			BodyParam bodyVar = p.getAnnotation(BodyParam.class);
			if(bodyVar != null && !bodyVar.name().isEmpty()) {
				name = bodyVar.name();
			}
			bodyParam = new RequestParam(RequestParam.BODY, name, p);
			orderedParams[idx++] = bodyParam;
		}
	}
	
	public Object invoke(HttpRequest req, HttpResponse res, String[] pathVals, Map<String, String> queryVals, XJSON xjson) {
		if(isBeforeOption()) {
			res.setContent(OPTION_RES.getBytes());
			return null;
		}
		int reqParamsCount = pathVals.length + queryVals.size() + (req.getContent() == null ? 0 : 1);
		if(reqParamsCount != paramsCount) {
			throw new ParamException("request contains " + reqParamsCount + " params but api '" 
							+ this.rawUri + "' requires " + paramsCount + " params");
		}

		Object[] paramInstances = new Object[paramsCount];
		int pathValOff = 0;
		for(int i = 0; i < paramsCount; i++) {
			RequestParam p = orderedParams[i];
			if(p.type() == RequestParam.BODY) {
				byte[] body = req.getContent();
				if(ContentType.APPLICATION_JSON.getName().equals(req.getContentType())) {
					String bodyStr;
					try {
						bodyStr = new String(body, req.getContentEncoding());
					} catch (UnsupportedEncodingException e) {
						throw new ParamException("invalid request content-encoding " + req.getContentEncoding());
					}
					try {
						Object v = xjson.parse(bodyStr, p.param().getParameterizedType());
						paramInstances[i] = v;
					} catch (XJSONException e) {
						throw new ParamException("can not parse " + bodyStr + " to " + p.param().getParameterizedType().getTypeName(), e);
					}
					continue;
				}
				if(ContentType.MULTIPART_FORMDATA.getName().equals(req.getContentType())) { 
					List<Multipart> multiparts = MultipartParser.parse(req);
					String json;
					try {
						json = MultipartUtil.multipartsToJSONString(multiparts, req.getContentEncoding());
					} catch (UnsupportedEncodingException e) {
						throw new TypeException("invalid content-encoding " + req.getContentEncoding());
					}
					try {
						Object v = xjson.parse(json, p.param().getParameterizedType());
						paramInstances[i] = v;
					} catch (XJSONException e) {
						throw new ParamException("can not parse " + json + " to " + p.param().getParameterizedType().getTypeName());
					}
				}
			} else {
				String val = null;
				if(p.type() == RequestParam.PATH) {
					val = pathVals[pathValOff++];
				} else if(p.type() == RequestParam.QUERY) {
					val = queryVals.getOrDefault(p.k(), null);
				} else {
					throw new ParamException("unkonwn param type " + p.type() + " at " + 
							instance.getClass().getName() + "." + method.getName() + ", param name is '" + p.k() + "'");
				}
				Object v = ParamReflectUtil.reflectFromJavaType(p.param().getParameterizedType(), val);
				paramInstances[i] = v;
			}
		}
		
		try {
			return method.invoke(instance, paramInstances);
		} catch (Exception e) {
			throw new ContainerException("call " + instance.getClass().getName() + "." + method.getName() + 
					" failed", e);
		}
	}

	public FilterState filterAnnotation(HttpRequest req, HttpResponse res) {
		for(AnnotationFilter filter: antFilters) {
			if(FilterState.END.equals(filter.inputMessage(req, res))) {
				return FilterState.END;
			}
		}
		return FilterState.CONTINUE;
	}
	
	public String getMappingKey() {
		return toMappingKey(httpMethod, uri);
	}

	public Object getInstance() {
		return instance;
	}

	public Method getMethod() {
		return method;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public String getUri() {
		return uri;
	}

	public String getReqContentType() {
		return reqContentType;
	}

	public String getResContentType() {
		return resContentType;
	}

	public int getParamsCount() {
		return paramsCount;
	}

	public boolean isBeforeOption() {
		return beforeOption;
	}
	
	public boolean isPathApi() {
		return this.pathApi;
	}
	
	public String[] uriSegments() {
		return this.uriSegments;
	}
	
	public int pathValCount() {
		return this.pathValCount;
	}
}