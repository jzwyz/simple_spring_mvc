package com.jzw.framework;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.PersistentMBean;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.jzw.annotation.JAutowired;
import com.jzw.annotation.JController;
import com.jzw.annotation.JRequestMapping;
import com.jzw.annotation.JRequestParam;
import com.jzw.annotation.JService;

/**
 * 模拟Spring核心处理器 min版
 * @author zwenJs
 */
public class JDispatcherServlet extends HttpServlet {
	
	/**
	 * 保存spring配置信息
	 */
	private Map<String, Map<String, Object>> configLocation = new HashMap<String, Map<String, Object>>();
	
	/**
	 * 保存所有相关的类路径  不是所有的牛奶都叫特仑苏
	 */
	private List<String> classs = new ArrayList<String>();
	
	/**
	 * 保存所有加载的类            IOC容器
	 */
	private Map<String, Object> iocMap = new HashMap<String, Object>();
	
	/**
	 * URL映射到方法                Spring映射器容器
	 */
	private List<Handler> handlerMapping = new ArrayList<Handler>();
	
	
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}
	
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		try {
			
			//处理用户请求  控制器、方法映射
			doDispatch(req, resp);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void init(ServletConfig config) throws ServletException {
		
		/**
		 * 模拟Spring的9大组件  Min版
		 */
		
		//1、加载相关的配置文件信息
		doLoadConfig(config.getInitParameter("contextConfigLocation"));
		
		//2、扫描所有相关的类
		doScanner((String)configLocation.get("component-scan").get("package"));
		
		//3、实例化所有相关的class，并保存到相应的IOC容器中
		doInstance();
		
		//4、自动化依赖注入
		doAutowired();
		
		//5、初始化HandlerMapping
		initHandlerMapping();
		
		System.out.println("JSpring Init 完成!");
		
	}
	
	
	
	/**
	 * 加载初始化参数
	 * 这里使用 DOM4J 对XML进行解析，需要提前导入DOM4J的jar包
	 * @param location
	 */
	private void doLoadConfig(String location) {
		System.out.println("配置文件的路径："+location);
		//将配置文件加到内存中
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(location);
		
		//实列化DOM4J解析工具
		SAXReader readerXML = new SAXReader();
		try {
			//开始解析
			resolveXML(readerXML.read(is));
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			/**
			 * 处理完成之后记得关闭InputStearm流
			 */
			if (null != is) {
				try {
					is.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
	
	/**
	 * 根据配置文件指定路径下的类文件  进行扫描
	 * @param packagePath
	 */
	private void doScanner(String packagePath) {
		URL urlClass = this.getClass().getClassLoader().getResource("/"+packagePath.replaceAll("\\.", "/"));
	
		/**
		 * 处理urlClass.getFile()的路径
		 */
		String filePath = urlClass.getFile().substring(1, urlClass.getFile().length());
		filePath = filePath.replace("%20", " ");
		System.out.println(filePath);
		
		File classDir = new File(filePath);
		
		for (File fileCla : classDir.listFiles()) {
			//如果是个文件夹，则迭代查找
			if (fileCla.isDirectory()) {
				doScanner(packagePath+"/"+fileCla.getName());
			} 
			else {
				String classPath = packagePath+"."+fileCla.getName().replace(".class", "");
				classs.add(classPath.replaceAll("/", "."));
			}
		}
	
	}
	
	/**
	 * 实列化特殊的类 不是所有的牛奶都叫特仑苏
	 */
	private void doInstance() {
		
		if(classs.isEmpty()) {return ;}
		
		try {
			for (String className : classs) {
				
				Class<?> loadClass = Class.forName(className);
				
				//实例化类？ 原则问题
				//判断，不是所有的牛奶都叫特仑苏
				if (loadClass.isAnnotationPresent(JController.class)) {
					
					String iocName = lowerFirst(loadClass.getSimpleName());
					iocMap.put(iocName, loadClass.newInstance());
					
				}
				else if (loadClass.isAnnotationPresent(JService.class)) {
					//1、默认以类名（首字母小写）为别名
					//2、如果自己定义了别名则使用自定义
					//3、根据类型配置，利用接口作为key
					
					String iocName = loadClass.getAnnotation(JService.class).value();
					if ("".equals(iocName.trim())) {
						iocName = lowerFirst(loadClass.getSimpleName());
					}
					
					Object newClass = loadClass.newInstance();
					iocMap.put(iocName, newClass);
					
					/**
					 * 获取实列化的这个类 所有的接口，保存！
					 */
					Class<?>[] interfaces = loadClass.getInterfaces();
					for (Class<?> i : interfaces) {
						iocMap.put(i.getName(), newClass);
					}
					
				}
				else {
					continue;
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 依赖注入
	 */
	private void doAutowired() {
		
		if(iocMap.isEmpty()) {return;}
		
		for (Entry<String, Object> entry : iocMap.entrySet()) {
			//1、在spring中 类 没有隐私，
			//2、依赖注入，咋们只认识 @JAutowired 注解的成员变量
			Field[] fields = entry.getValue().getClass().getDeclaredFields();
			for (Field field : fields) {
				
				if (!field.isAnnotationPresent(JAutowired.class)) {
					continue;
				}
				
				String beanName = field.getAnnotation(JAutowired.class).value().trim();
				if ("".equals(beanName)) {
					beanName = field.getType().getName();
					System.out.println("获取自动注入的名称："+beanName);
					System.out.println("从ioc容器中获取的类为："+iocMap.get(beanName));
				}
				
				//授权，
				field.setAccessible(true);
				
				try {
					field.set(entry.getValue(), iocMap.get(beanName));
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
				
			}
		}
		
	}
	
	/**
	 * 控制器、方法映射
	 */
	private void initHandlerMapping() {
		if (iocMap.isEmpty()) {return;}
		
		for (Entry<String, Object> entry : iocMap.entrySet()) {
			
			Class<?> classEntry = entry.getValue().getClass();
			//我们只认识拥有JController注解的   否则跳过
			if (!classEntry.isAnnotationPresent(JController.class)) {
				continue;
			}
			
			//获取类的URL配置
			String url = "";
			if (classEntry.isAnnotationPresent(JRequestMapping.class)) {
				JRequestMapping jRequestMapping = classEntry.getAnnotation(JRequestMapping.class);
				url += jRequestMapping.value();
			}
			
			//获取该方法的URL配置（public）
			Method[] methods = classEntry.getMethods();
			for (Method method : methods) {
				
				//不存在@JReqeustMapping的方法直接跳过
				if (!method.isAnnotationPresent(JRequestMapping.class)) {
					continue;
				}
				
				//映射URL
				JRequestMapping jRequestMapping = method.getAnnotation(JRequestMapping.class);
				String methodUrl = url+jRequestMapping.value();
				Pattern pattern = Pattern.compile(methodUrl);
				
				//保存到Handler中
				handlerMapping.add(new Handler(entry.getValue(), method, pattern));
				
				System.out.println("Mapping : "+methodUrl+" ---> "+method.getName());
			}
			
		}
	}
	
	/**
	 * 执行
	 * @param req
	 * @param resp
	 */
	private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		
		try {
			//1、获取Handler
			Handler handler = getHandler(req);
			
			//没有找到则返回404（找不到对应的资源处理用户的请求）
			if (null == handler) {
				resp.getWriter().write("404 Not Found");
				return ;
			}
			
			//创建需要赋值的参数数组
			Object[] methodObjecj = new Object[0];
			
			//2、对需要自动赋值的参数进行赋值
			if (null != handler.method.getParameterTypes()) {
				//获取该方法的所有参数
				Class<?>[] methodClass = handler.method.getParameterTypes();
				methodObjecj = new Object[methodClass.length];
				
				Map<String, String[]> parameMap = req.getParameterMap();
				for (Entry<String, String[]> parame : parameMap.entrySet()) {
					String parameValue = Arrays.toString(parame.getValue()).replaceAll("\\[|\\]", "").replace(",\\s", ",");
					
					//给该参数赋值保存
					if (!handler.paramIndex.containsKey(parame.getKey())) {continue;}
					int index = handler.paramIndex.get(parame.getKey());
					methodObjecj[index] = convert(methodClass[index], parameValue);
				}
				
				//3、填充request和response参数
				if (null != handler.paramIndex.get(HttpServletRequest.class.getName())) {
					int indexReq = handler.paramIndex.get(HttpServletRequest.class.getName());
					methodObjecj[indexReq] = req;
				}
				if (null != handler.paramIndex.get(HttpServletResponse.class.getName())) {
					int indexResp = handler.paramIndex.get(HttpServletResponse.class.getName());
					methodObjecj[indexResp] = resp;
				}
			}
			
			//执行控制器的方法
			Object execMethode = handler.method.invoke(handler.controller, methodObjecj);
			
			//4、处理返回结果
			if (null != execMethode) {
				if (execMethode instanceof String) {
					req.getRequestDispatcher((String)execMethode).forward(req, resp);
				}
				else {
					System.out.println("返回不是String类型，");
					resp.getWriter().write("500 Server Error");
				}
				return ;
			}
			
			req.getRequestDispatcher("/index.html").forward(req, resp);
			
		} catch (Exception e) {
			throw e;
		}
		
	}
	

	/**
	 * 映射器匹配
	 * @param req
	 * @return
	 */
	private Handler getHandler(HttpServletRequest req) {
		//如果映射器集合为NULL则返回NULL
		if (handlerMapping.isEmpty()) {return null;}
		
		//获取一条整得路径   如： /myspring_20180226/add/dog.json
		String url = req.getRequestURI();
		//获取项目名称         如：/myspring_20180226
		String contextPath = req.getContextPath();
		//我们只需要项目路径的匹配部分     如：/add/dog.json
		url = url.replace(contextPath, "");
		
		/**
		 * 1、遍历 HandlerMapping 容器
		 * 2、将url放入 HandlerMapping 的 Pattern 中进行匹配（正则匹配）
		 * 3、返回匹配成功的 Handler 对象
		 */
		for (Handler handler : handlerMapping) {
			
			try {
				Matcher matcher = handler.pattern.matcher(url);
				
				//不匹配则继续对后面的进行匹配
				if (!matcher.matches()) {
					continue;
				}
				
				return handler;
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			
		}
		
		return null;
	}

	
	/**
	 * 解析XML配置文件信息
	 * @param document
	 */
	private void resolveXML(Document document) {
		if (null == document) {return ;}
		
		/**
		 * 开始DOM4J解析
		 */
		Element rootElement = document.getRootElement();
		Iterator iterator = rootElement.elementIterator();
		while(iterator.hasNext()) {
			
			//获取当前节点
			Element element = (Element)iterator.next();
			
			//创建保存当前节点属性的容器
			Map<String, Object> mapAttr = new HashMap<String, Object>();
			
			System.out.println("当前节点名称："+element.getName());
			System.out.println("当前节点的：属性名称 ---- 值");
			
			//遍历当前节点的属性
			List<Attribute> attrs = element.attributes();
			for (Attribute attribute : attrs) {
				//如果这个属性的值为null则跳过该属性继续遍历
				if (null == attribute) {continue;}
				
				System.out.println(attribute.getName()+" ---- "+attribute.getValue());
				mapAttr.put(attribute.getName(), attribute.getValue());
				
			}
			
			//保存配置信息
			configLocation.put(element.getName(), mapAttr);
		}
	}

	
	/**
	 * 首字母大写
	 * @param name
	 * @return
	 */
	private String lowerFirst(String name) {
		/**
		 * 思路：1、将字符串分解成char数组，
		 *      2、对该数组的第一个元素（字符串的第一个字母）＋32（Ascii码）
		 *                                           A:45  a:77   相差32，所以+32
		 *      3、重组该char数组
		 */
		char[] charStr = name.toCharArray();
		charStr[0] += 32;
		return String.valueOf(charStr);
	}
	
	
	/**
	 * 针对Integer类型的参数转换
	 * @param type
	 * @param value
	 * @return
	 */
	private Object convert(Class<?> type, String value) {
		if (Integer.class == type) {
			return Integer.valueOf(value);
		}
		return value;
	}
	
	
	/**
	 * JSpring 映射器
	 * @author zwenJs
	 *
	 */
	private class Handler {
		
		/**
		 * protected 作用域： 
		 * 					当前类    同 package    子孙类    其他package
		 *                   √        √          √         ×
		 */
		
		protected Object controller;			       //方法对应的实列
		protected Method method;				       //映射的方法
		protected Pattern pattern;			       //url的正则匹配
		protected Map<String, Integer> paramIndex;   //参数的顺序
		
		private Handler () {}
		
		public Handler (Object controller, Method method, Pattern pattern) {
			this.controller = controller;
			this.method = method;
			this.pattern = pattern;
			
			paramIndex = new HashMap<String, Integer>();
			putParameIndexMapping(method);
		}
		
		/**
		 * 处理方法参数顺序
		 * @param method
		 */
		private void putParameIndexMapping(Method method) {
			//提取方法中加了注解的参数
			Annotation[] [] annotations = method.getParameterAnnotations();
			for (int i = 0; i < annotations.length; i++) {
				for (Annotation anno : annotations[i]) {
					if (anno instanceof JRequestParam) {
						String parameName = ((JRequestParam) anno).value();
						if (!"".equals(parameName.trim())) {
							paramIndex.put(parameName, i);
						}
					}
				}
			}
			
			//提取方法中的request 和 response参数
			Class<?>[] classParames = method.getParameterTypes();
			for (int i = 0; i < classParames.length; i++) {
				Class<?> type = classParames[i];
				if (type == HttpServletRequest.class || 
						type == HttpServletResponse.class) {
					paramIndex.put(type.getName(), i);
				}
			}
		}
		
		
	}
	
	
	
	private static void test(Object obj) {
		if (obj instanceof String) {
			System.out.println(obj+" 是 String类型");
		}
		else if (obj instanceof Integer) {
			System.out.println(obj+" 是Integer类型");
		}
		
	}
	
	
	public static void main(String[] args) {
		test("123");
	}
	
}
