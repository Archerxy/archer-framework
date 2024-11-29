package com.archer.framework.base.exceptions;

public class TypeException  extends RuntimeException {

    static final long serialVersionUID = -312129624229948L;
    
    static final int END_OFFSET = 32;
    
    public TypeException(Throwable e) {
    	super(e);
    }
    
    public TypeException(String msg) {
    	super(msg);
    }
}
