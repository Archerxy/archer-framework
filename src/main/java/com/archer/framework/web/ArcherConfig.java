package com.archer.framework.web;

import com.archer.framework.base.annotation.Config;
import com.archer.framework.base.annotation.ConfigComponent;
import com.archer.framework.base.annotation.Inject;
import com.archer.framework.base.annotation.Value;
import com.archer.framework.web.exceptions.HttpServerException;
import com.archer.log.Logger;

@Config
public class ArcherConfig {
	
	@Value(id = "archer.http.port", defaultVal = "9617")
	int port;

	@Inject
	Logger log;
	
	@Inject
	HttpHandler handler;
	
	@ConfigComponent
	public Archer initArcher() {
		Archer archer = new Archer();
		try {
			archer.listen(port, handler);
			log.info("Archer Server started on {}", port);
		} catch (HttpServerException e) {
			log.error("server listening {} failed, {}", port, e);
			System.exit(0);
		}
		return archer;
	}
}
