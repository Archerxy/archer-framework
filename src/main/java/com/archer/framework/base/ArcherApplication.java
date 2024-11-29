package com.archer.framework.base;

import com.archer.framework.base.component.ClassContainer;
import com.archer.framework.base.conf.Conf;
import com.archer.framework.base.conf.ConfLoader;
import com.archer.framework.base.logger.LoggerInitliazer;
import com.archer.framework.base.util.PlatformUtil;

public class ArcherApplication {
	
	public static void go(String[] args) {

		if(!PlatformUtil.isWindows() && !PlatformUtil.isLinux()) {
			System.out.println("paltform " + System.getProperty("os.name") + " is not supported.");
			System.exit(0);
		}
		
		Conf conf  = ConfLoader.load();
		ClassContainer classes = new ClassContainer(conf);
		classes.loadComponents();
	}
}
