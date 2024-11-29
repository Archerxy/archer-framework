package com.archer.framework.base.conf;

import java.util.Arrays;
import java.util.LinkedList;

import com.archer.framework.base.exceptions.ConfigException;

public class ConfNode {
	
	static final char SEP = '\n';
	
	String[] keys;
	String rawKey;
	String key;
	String val;
	boolean isArr;
	
	public ConfNode(String[] keys, String val, boolean isArr) {
		this.keys = keys;
		this.rawKey = String.join(".", keys);
		this.val = val;
		this.isArr = isArr;
		StringBuilder sb = new StringBuilder(keys.length * 128);
		for(int i = 0; i < keys.length; i++) {
			char[] kcs = keys[i].toCharArray(), nk = new char[kcs.length];
			int off = 0, cg = 0;
			for(int j = 0; j < kcs.length; j++) {
				if(kcs[j] == ConfParser.LINE) {
					cg = 1;
				} else {
					if(cg == 1 && 'a' <= kcs[j] && kcs[j] <= 'z') {
						nk[off++] = (char) (kcs[j] - 32);
					} else {
						nk[off++] = kcs[j];
					}
					cg = 0;
				}
			}
			if(i != 0) {
				sb.append('.');
			} 
			sb.append(Arrays.copyOfRange(nk, 0, off));
		}
		this.key = sb.toString();
	}
	
	public String getValue() {
		if(isArr) {
			throw new ConfigException("Key '" + key +"' contains Array-Like item");
		}
		return val;
	}
	
	public LinkedList<String> getStringListVal() {
		if(!isArr) {
			throw new ConfigException("Key '" + key +"' does not contain Array-Like item");
		}
		LinkedList<String> ret = new LinkedList<String>();
		char[] valChars = val.toCharArray();
		int idx = 0;
		for(int i = 0; i < valChars.length; i++) {
			if(valChars[i] == ConfParser.ENTER) {
				ret.add(new String(Arrays.copyOfRange(valChars, idx, i)));
				i++;
				idx = i;
			}
		}
		if(idx < valChars.length) {
			ret.add(new String(Arrays.copyOfRange(valChars, idx, valChars.length)));
		}
		return ret;
	}
}
