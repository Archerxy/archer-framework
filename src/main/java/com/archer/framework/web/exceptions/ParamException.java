package com.archer.framework.web.exceptions;

public class ParamException extends RuntimeException {

    static final long serialVersionUID = -39624229948L;
    
    static final int END_OFFSET = 32;
    
    public ParamException(Throwable e) {
    	super(e);
    }
    
    public ParamException(String msg) {
    	super(msg);
    }
    
    public ParamException(String msg, Throwable e) {
    	super(msg, e);
    }
}
