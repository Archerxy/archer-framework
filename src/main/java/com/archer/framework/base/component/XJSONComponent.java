package com.archer.framework.base.component;

import com.archer.framework.base.annotation.Config;
import com.archer.framework.base.annotation.ConfigComponent;
import com.archer.xjson.XJSON;

@Config
public class XJSONComponent {
	
	@ConfigComponent
	public XJSON getXjson() {
		return new XJSON();
	}
}
