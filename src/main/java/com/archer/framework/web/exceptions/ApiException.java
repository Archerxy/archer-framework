package com.archer.framework.web.exceptions;

public class ApiException extends RuntimeException {

    static final long serialVersionUID = -15334229948L;
    
    static final int END_OFFSET = 32;
    
    public ApiException(Throwable e) {
    	super(e);
    }
    
    public ApiException(String msg) {
    	super(msg);
    }
    
    public ApiException(String msg, Throwable e) {
    	super(msg, e);
    }
}
