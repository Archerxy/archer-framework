package com.archer.framework.base.logger;

import com.archer.framework.base.component.ComponentContainer;
import com.archer.framework.base.conf.Conf;
import com.archer.log.LogProperties;
import com.archer.log.Logger;


public class LoggerInitliazer {

	private static final String LOGGER_NAME = "archer-framework-log";
	private static final String PREFIX = "archer.log.";
	private static final String LEVEL = PREFIX + "level";
	private static final String TIME_PATTERN = PREFIX + "timePattern";
	private static final String CLASS_PATTERN = PREFIX + "classPattern";
	private static final String KEEP_DAYS = PREFIX + "keepDays";
	private static final String LOG_PATH = PREFIX + "logPath";
	private static final String FILE_NAME = PREFIX + "fileName";
	
	
	ComponentContainer components;
	
	public LoggerInitliazer(ComponentContainer components) {
		this.components = components;
	}
	
	public void init(Conf conf) {
		LogProperties properties = LogProperties.getDefault();
		properties.appendFile(true);
		
		String level = conf.getString(LEVEL);
		properties.level(level);
		
		String timePattern = conf.getString(TIME_PATTERN);
		properties.timePattern(timePattern);
		
		String classPattern = conf.getString(CLASS_PATTERN);
		properties.classPattern(classPattern);
		
		Integer keepDaysObj = conf.getInteger(KEEP_DAYS);
		int keepDays = -1;
		if(keepDaysObj != null) {
			keepDays = keepDaysObj;
		}
		properties.keepDays(keepDays);
		
		String logPath = conf.getString(LOG_PATH);
		properties.logPath(logPath);

		String fileName = conf.getString(FILE_NAME);
		properties.fileName(fileName);
		
		components.componentLogger(Logger.getLoggerAndSetPropertiesIfNotExits(LOGGER_NAME, properties));
	}
}
