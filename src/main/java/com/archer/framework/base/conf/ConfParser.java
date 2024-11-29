package com.archer.framework.base.conf;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.archer.framework.base.exceptions.ConfigException;

class ConfParser {
	
	private static final int CONFIG_SIZE = 256;
	

	static final char LINE = '-';
	static final char COLON = ':';
	static final char QUOTE = '"';
	static final char SINGLE_QUOTE = '\'';
	static final char WELL = '#';
	static final char SPACE = ' ';
	static final char ENTER = '\n';
	static final char LINEB = '\r';
	static final char TAB = '\t';
	static final char BACKSLASH = '\\';
	static final char SLASH = '/';
	
	
	static final char a = 'a';
	static final char z = 'z';
	static final char A = 'A';
	static final char Z = 'Z';
	
	public static Conf parseFromString(String content) {
		return new Conf(parse(content));
	}
	
	static List<ConfNode> parse(String content) {
		char[] chars = content.toCharArray();
		char[][] lines = new char[CONFIG_SIZE][];
		int idx = 0, p = 0, contains = 0, ok = 0;
		for(int i = 0; i < content.length(); i++) {
			if(content.charAt(i) == ENTER) {
				if(idx < i && contains > 0) {
					int e = i;
					if(content.charAt(i-1) == LINEB) {
						e = i - 1;
					} else if(i < content.length() - 1 && content.charAt(i+1) == LINEB) {
						e = i;
						i++;
					}
					ok = 0;
					if(WELL != content.charAt(idx)) {
						for(int j = idx; j <= e; j++) {
							if(SLASH != content.charAt(j) && LINEB != content.charAt(j) && 
								TAB != content.charAt(j) && BACKSLASH != content.charAt(j) &&
								SPACE != content.charAt(j)) {
								ok = 1;
								break ;
							}
						}
					}
					if(ok == 1) {
						lines[p++] = Arrays.copyOfRange(chars, idx, e);
					}
				}
				contains = 0;
				idx++;
				continue ;
			}
			if(contains == 0 && SLASH != content.charAt(i) && LINEB != content.charAt(i) && 
				TAB != content.charAt(i) && BACKSLASH != content.charAt(i)) {
				idx = i;
				contains = 1;
			}
		}
		int linespace = 0, type = 0, notes = 0, ks = 0, ke = 0, off = 0;
		KeyNode[] keys = new KeyNode[p << 1];
		for(int i = 0; i < p; i++) {
			
			char[] l = lines[i];
			linespace = 0;
			type = 0; // 0=default 1 = key, 2 = key + val , 3 = val
			notes = 0;
			ks = 0;
			ke = 0;

			keys[off] = new KeyNode();
			for(int j = 0; j < l.length; j++) {
				if(SPACE == l[j]) {
					linespace++;
					continue;
				}
				if(WELL == l[j]) {
					notes = 1;
					break ;
				}
				if(LINEB != l[j] && TAB != l[j] && BACKSLASH != l[j] && SLASH != l[j]) {
					keys[off].spaces = linespace;
					ks = j;
					break;
				}
				throw new ConfigException("Invalid line: " + new String(l));
			}
			if(notes == 1) {
				continue ;
			}
			for(int j = l.length - 1; j > 0; j--) {
				if(ke == 0 && SPACE == l[j]) {
					continue;
				}
				if(ke == 0) {
					ke = j + 1;
				} 
				if(ke > 0) {
					if(WELL == l[j] && SPACE == l[j-1]) {
						j--;
						while(j > 0 && SPACE == l[j]) {
							j--;
						}
						ke = j + 1;
					}
				}
			}
			if(ks >= ke - 1) {
				System.out.println("before line = " + new String(lines[i-1]));
				System.out.println("cur line = " + new String(lines[i]));
				throw new ConfigException("Invalid line: " + new String(l));
			}

			int colonIdx = ks;
			if(COLON == l[ke - 1]) {
				type = 1;
			} else if(LINE == l[ks] && SPACE == l[ks+1]) {
				type = 2;
				ks += 2;
				keys[off].isVal = true;
				keys[off].isArr = true;
			} else {
				type = 2;
				for(int j = ks; j < ke - 1; j++) {
					if(COLON == l[j] &&  SPACE == l[j+1]) {
						type = 3;
						colonIdx = j;
						break ;
					}
				}
			}
			if(type == 1) {
				int t = ke - 2;
				while(t > ks && l[t] == SPACE) {
					t--;
				}
				if(t == ks) {
					throw new ConfigException("Invalid line: " + new String(l));
				}
				keys[off].val = new String(Arrays.copyOfRange(l, ks, t+1));
			} else if(type == 2) {
				int t = ks;
				while(t < ke && l[t] == SPACE) {
					t++;
				}
				if(SINGLE_QUOTE == l[t] && SINGLE_QUOTE == l[ke - 1]) {
					t++;
					ke--;
				}
				if(QUOTE == l[t] && QUOTE == l[ke - 1]) {
					t++;
					ke--;
				}
				if(t >= ke) {
					throw new ConfigException("Invalid line: " + new String(l));
				}
				String val = new String(Arrays.copyOfRange(l, t, ke));
				if(keys[off].isArr) {
					if(keys[off-1].isVal) {
						off--;
						keys[off].val += ConfNode.SEP + val;
						keys[off].isArr = true;
					} else {
						keys[off].val = val;
					}
				} else {
					keys[off].val = val;
				}
			} else {
				int t = colonIdx - 1;
				while(t > ks && SPACE == l[t]) {
					t--;
				}
				if(t == ks) {
					throw new ConfigException("Invalid line: " + new String(l));
				}
				keys[off].val = new String(Arrays.copyOfRange(l, ks, t + 1));
				
				off++;
				keys[off] = new KeyNode();
				keys[off].spaces = keys[off - 1].spaces + 2;
				keys[off].isVal = true;
				
				t = colonIdx + 1;
				while(t < ke && SPACE == l[t]) {
					t++;
				}
				if(SINGLE_QUOTE == l[t] && SINGLE_QUOTE == l[ke - 1]) {
					t++;
					ke--;
				}
				if(QUOTE == l[t] && QUOTE == l[ke - 1]) {
					t++;
					ke--;
				}
				if(t >= ke) {
					throw new ConfigException("Invalid line: " + new String(l));
				}
				keys[off].val = new String(Arrays.copyOfRange(l, t, ke));
			}
			off++;
		}
		List<ConfNode> configs = new ArrayList<>(CONFIG_SIZE);
		KeyNode[] keyLine = new KeyNode[p];
		int keyOff = 0;
		for(int i = 0; i < off; i++) {
			if(keys[i].isVal) {
				if(i == 0 || keys[i].spaces <= keys[i - 1].spaces) {
					throw new ConfigException("Invalid line: " + keys[i].val);
				}
				String[] configKeys = new String[keyOff];
				for(int j = 0; j < keyOff; j++) {
					configKeys[j] = keyLine[j].val;
				}
				configs.add(new ConfNode(configKeys, keys[i].val, keys[i].isArr));
			} else {
				while(keyOff > 0 && keys[i].spaces <= keyLine[keyOff - 1].spaces) {
					keyOff--;
				}
				keyLine[keyOff++] = keys[i];
			}
		}
		return configs;
	}
	
	
	static class KeyNode {
		String val;
		int spaces;
		boolean isVal;
		boolean isArr;
	}
	
	public static void main(String[] args) {
		System.out.println((int)'a');
		System.out.println((int)'A');
		try {
			byte[] bytes =  Files.readAllBytes(Paths.get("E:\\projects\\javaProject\\maven-package\\archer-framework\\src\\main\\resources\\app.yml"));
			List<ConfNode> ret = parse(new String(bytes, StandardCharsets.UTF_8));
			for(ConfNode node : ret) {
				System.out.println(node.key+": " + node.val);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
