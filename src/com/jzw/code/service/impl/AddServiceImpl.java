package com.jzw.code.service.impl;

import com.jzw.annotation.JService;
import com.jzw.code.service.IAddService;

@JService
public class AddServiceImpl implements IAddService{
	
	@Override
	public String add(String name) {
		return "服务层添加了："+name;
	}

}
