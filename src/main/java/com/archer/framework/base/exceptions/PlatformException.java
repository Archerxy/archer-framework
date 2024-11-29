package com.archer.framework.base.exceptions;

public class PlatformException extends RuntimeException {

    static final long serialVersionUID = -561413124229948L;
    
    static final int END_OFFSET = 32;
    
    public PlatformException(Throwable e) {
    	super(e);
    }
    
    public PlatformException(String msg) {
    	super(msg);
    }
}
