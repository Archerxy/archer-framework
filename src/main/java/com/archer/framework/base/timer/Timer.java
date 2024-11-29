package com.archer.framework.base.timer;

public class Timer {
	
	long startTime;
	
	public Timer() {
		this.startTime = System.currentTimeMillis();
	}
	
	public long calculateCost() {
		return System.currentTimeMillis() - startTime;
	}
}
