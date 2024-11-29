package com.archer.framework.base.conf;

import java.util.LinkedList;
import java.util.List;

public final class Conf {
	
	static final int CONFIG_SIZE = 128;
	
	private List<ConfNode> configs;
	
	protected Conf() {
		this(new LinkedList<>());
	}
	
	protected Conf(List<ConfNode> configs) {
		this.configs = configs;
	}
	
	public String getString(String key) {
		for(ConfNode node: configs) {
			if(node.key.equals(key)) {
				return node.getValue();
			}
		}
		return null;
	}
	
	public Boolean getBoolean(String key) {
		for(ConfNode node: configs) {
			if(node.key.equals(key)) {
				return Boolean.parseBoolean(node.getValue());
			}
		}
		return null;
	}
	
	public Integer getInteger(String key) {
		for(ConfNode node: configs) {
			if(node.key.equals(key)) {
				return Integer.parseInt(node.getValue());
			}
		}
		return null;
	}
	
	public Long getLong(String key) {
		for(ConfNode node: configs) {
			if(node.key.equals(key)) {
				return Long.parseLong(node.getValue());
			}
		}
		return null;
	}
	
	public Float getFloat(String key) {
		for(ConfNode node: configs) {
			if(node.key.equals(key)) {
				return Float.valueOf(node.getValue());
			}
		}
		return null;
	}
	
	public Double getDouble(String key) {
		for(ConfNode node: configs) {
			if(node.key.equals(key)) {
				return Double.valueOf(node.getValue());
			}
		}
		return null;
	}
	
	public LinkedList<String> getStringList(String key) {
		LinkedList<String> ret = new LinkedList<>();
		for(ConfNode node: configs) {
			if(node.key.equals(key)) {
				return node.getStringListVal();
			}
		}
		return ret;
	}

	public LinkedList<Integer> getIntegerList(String key) {
		LinkedList<Integer> ret = new LinkedList<>();
		for(ConfNode node: configs) {
			if(node.key.equals(key)) {
				LinkedList<String> arr = node.getStringListVal();
				for(String s : arr) {
					ret.add(Integer.parseInt(s));
				}
				return ret;
			}
		}
		return ret;
	}
	
	public LinkedList<Long> getLongList(String key) {
		LinkedList<Long> ret = new LinkedList<>();
		for(ConfNode node: configs) {
			if(node.key.equals(key)) {
				LinkedList<String> arr = node.getStringListVal();
				for(String s : arr) {
					ret.add(Long.parseLong(s));
				}
				return ret;
			}
		}
		return ret;
	}
	
	public LinkedList<Float> getFloatList(String key) {
		LinkedList<Float> ret = new LinkedList<>();
		for(ConfNode node: configs) {
			if(node.key.equals(key)) {
				LinkedList<String> arr = node.getStringListVal();
				for(String s : arr) {
					ret.add(Float.valueOf(s));
				}
				return ret;
			}
		}
		return ret;
	}
	
	public LinkedList<Double> getDoubleList(String key) {
		LinkedList<Double> ret = new LinkedList<>();
		for(ConfNode node: configs) {
			if(node.key.equals(key)) {
				LinkedList<String> arr = node.getStringListVal();
				for(String s : arr) {
					ret.add(Double.valueOf(s));
				}
				return ret;
			}
		}
		return ret;
	}
	
	public LinkedList<ConfNode> getPrefixVals(String prefix) {
		LinkedList<ConfNode> ret = new LinkedList<>();
		for(ConfNode node: configs) {
			if(node.key.startsWith(prefix)) {
				ret.add(node);
			}
		}
		return ret;
	}
}
