package com.archer.framework.base.exceptions;

public class ArcherApplicationException extends RuntimeException {

    static final long serialVersionUID = -3121413124229948L;
    
    static final int END_OFFSET = 32;
    
    public ArcherApplicationException(Throwable e) {
    	super(e);
    }
    
    public ArcherApplicationException(String msg) {
    	super(msg);
    }
    
    public ArcherApplicationException(String msg, Throwable e) {
    	super(msg, e);
    }
}
