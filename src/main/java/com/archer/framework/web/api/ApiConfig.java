package com.archer.framework.web.api;

import com.archer.framework.base.annotation.Config;
import com.archer.framework.base.annotation.ConfigComponent;
import com.archer.framework.base.annotation.Inject;
import com.archer.framework.base.annotation.Value;
import com.archer.framework.base.component.ComponentContainer;
import com.archer.framework.web.filter.FilterForwardComponnet;

@Config
public class ApiConfig {
	
	@Value(id = "archer.contextPath", defaultVal = "")
	String prefix;
	
	@Inject
	ComponentContainer container;
	
	@Inject
	FilterForwardComponnet filter;
	
	@ConfigComponent
	public ApiMatcher initControllerMatcher() {
		return new ApiMatcher(prefix, container.getControllers(), filter);
	}
}
