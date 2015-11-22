package com.connector.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @date 2015-9-8
   @author Allen
 */
public class PropertiesUtils {

    private static final String configFile = "connector.properties";
    private static Logger logger = LoggerFactory.getLogger(PropertiesUtils.class);
    private static Properties properties = null;	
    
    static{
    	properties = getProperties();
    }
    
    public static Properties getProperties() {
        Properties properties = new Properties();
        InputStream inputStream = PropertiesUtils.class.getClassLoader().getResourceAsStream(configFile);
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            logger.error("load properties file error: " + e.getMessage());
        }finally{
        	try {
        		if(inputStream != null){
        			inputStream.close();
        		}
			} catch (IOException e) {
				logger.error("close stream error: " + e.getMessage());
			}
        }
        return properties;
    }
    
	public static String get(String name,String defalutValue){
		String value = properties.getProperty(name);
		if(StringUtils.isBlank(value)){
			return defalutValue;
		}
		return value;
	}
	
	public static int getInt(String name,int defaultValue){
		String strValue = get(name, "");
		if(StringUtils.isBlank(strValue)){
			return defaultValue;
		}
		try{
			int value = Integer.parseInt(strValue);
			return value;
		}catch(Exception e){
			logger.error("transfer string to int error: " + e.getMessage());
		}
		return defaultValue;
	}
	
	public static Map<String, List<String>> getListWithKeyPrefix(String keyPrefix){
		Set<Object> set = properties.keySet();
		if(set == null || set.size() == 0){
			return null;
		}
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		for(Object key : set){
			String keyStr = (String)key;
			if(keyStr.startsWith(keyPrefix)){
				List<String> list = getStringList(keyStr);
				if(list != null){
					map.put(keyStr, list);
				}
			}
		}
		return map;
	}
	
	public static List<String> getStringList(String key){
		String value = properties.getProperty(key);
		if(StringUtils.isBlank(value)){
			return null;
		}
		String[] arrays = value.split(",");
		List<String> list = Arrays.asList(arrays);
		return list;
	}
}
