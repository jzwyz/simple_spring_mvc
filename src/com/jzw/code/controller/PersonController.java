package com.jzw.code.controller;

import java.time.MonthDay;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jzw.annotation.JController;
import com.jzw.annotation.JRequestMapping;
import com.jzw.annotation.JRequestParam;
import com.jzw.framework.JModeAndView;


@JController
@JRequestMapping("/person")
public class PersonController {

	
	/**
	 * 模拟人在吃${type}
	 * @param type
	 * @param req
	 * @param resp
	 * @return
	 */
	@JRequestMapping("/eat.json")
	public String eat(@JRequestParam("type") String type, HttpServletRequest req, HttpServletResponse resp) {
		
		req.setAttribute("type", type);
		
		return "/person_result.jsp";
	}
	
	@JRequestMapping("/get.json")
	public JModeAndView getView() {
		return new JModeAndView("/person_result.jsp");
	}
	
}
