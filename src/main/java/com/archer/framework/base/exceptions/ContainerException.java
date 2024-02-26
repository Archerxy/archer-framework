package com.archer.framework.base.exceptions;

public class ContainerException extends RuntimeException {

    static final long serialVersionUID = -3121413124229948L;
    
    static final int END_OFFSET = 32;
    
    public ContainerException(Throwable e) {
    	super(e);
    }
    
    public ContainerException(String msg) {
    	super(msg);
    }
    
    public ContainerException(String msg, Throwable e) {
    	super(msg, e);
    }
}
