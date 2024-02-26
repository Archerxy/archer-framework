package com.archer.framework.web.util;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.List;

import com.archer.net.http.multipart.Multipart;
import com.archer.net.http.multipart.MultipartType;

public class MultipartUtil {
	
	public static String multipartsToJSONString(List<Multipart> multiparts, String encoding) throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder(multiparts.size() * 1024);
		sb.append("{");
		int idx = 0;
		for(Multipart part: multiparts) {
			if(idx != 0) {
				sb.append(",");
			}
			sb.append("\"").append(part.getName()).append("\":");
			if(part.getType() == MultipartType.FILE) {
				sb.append("\"").append(new String(Base64.getEncoder().encode(part.getContent())))
					.append("\"");
			}
			if(part.getType() == MultipartType.TEXT) {
				sb.append("\"").append(new String(part.getContent(), encoding))
					.append("\"");
			}
			idx = 1;
		}
		
		return sb.toString();
	}
}
