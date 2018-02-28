package com.jzw.code.controller;

import javax.servlet.http.HttpServletRequest;

import com.jzw.annotation.JAutowired;
import com.jzw.annotation.JController;
import com.jzw.annotation.JRequestMapping;
import com.jzw.annotation.JRequestParam;
import com.jzw.code.service.IAddService;

/**
 * 模拟Controller
 * @author zwenJs
 *
 */
@JController
@JRequestMapping("/add")
public class AddController {

	/**
	 * 模拟自动注入
	 */
	@JAutowired
	private IAddService addService;
	
	
	/**
	 * 模拟增加
	 * @param dogName
	 * @param request
	 * @return
	 */
	@JRequestMapping("/dog.json")
	public String addDog(@JRequestParam("dogName") String dogName, HttpServletRequest request) {
	
		System.out.println(addService.add("一条狗，名字是："+dogName));
		request.setAttribute("status", "success");
		request.setAttribute("type", "DOG");
		request.setAttribute("name", dogName);
		
		return "/add.jsp";
	}
	
	
}
