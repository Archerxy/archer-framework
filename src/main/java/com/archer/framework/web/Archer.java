package com.archer.framework.web;

import com.archer.framework.web.exceptions.HttpServerException;
import com.archer.net.ssl.SslContext;
import com.archer.net.http.HttpServer;

public class Archer {
	
	private SslContext sslCtx;
	HttpServer server;
	private int threadNum = 0;
	
	public Archer() {
		this(null);
	}
	
	public Archer(SslContext sslCtx) {
		this.sslCtx = sslCtx;
	}
	
	public Archer setThreadNum(int threadNum) {
		this.threadNum = threadNum;
		return this;
	}
	
	public void listen(String host, int port, HttpHandler handler) throws HttpServerException {
		server = new HttpServer(sslCtx);
		if(threadNum > 0) {
			server.setThreadNum(threadNum);
		}
		try {
			server.listen(host, port, handler);
		} catch (com.archer.net.http.HttpServerException e) {
			throw new HttpServerException(e);
		}
	}
	
	public void destroy() {
		server.destroy();
	}
}
