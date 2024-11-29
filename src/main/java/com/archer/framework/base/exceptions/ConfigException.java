package com.archer.framework.base.exceptions;

public class ConfigException extends RuntimeException {

    static final long serialVersionUID = -312146793124229948L;
    
    static final int END_OFFSET = 32;
    
    public ConfigException(Throwable e) {
    	super(e);
    }
    
    public ConfigException(String msg) {
    	super(msg);
    }
}
