package com.archer.framework.web.api;

public class ApiPathVal {

	Api api;
	
	String[] pathVals;

	ApiPathVal(Api api, String[] pathVals) {
		super();
		this.api = api;
		this.pathVals = pathVals;
	}

	public Api getApi() {
		return api;
	}

	public String[] getPathVals() {
		return pathVals;
	}
}
