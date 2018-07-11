package com.yhj.web.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yhj.web.handler.impl.DispatcherHandler;

public final class ViewsDispatcher extends HttpServlet {

	private static final long						serialVersionUID	= 1L;

	private static final DispatcherHandler			viewsHandler		= new DispatcherHandler();

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		viewsHandler.doInit(config);
		this.getServletContext().setAttribute("contextPath", config.getServletContext().getContextPath());
	}

	/**
	 * 解析url
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	protected final void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setCharacterEncoding("UTF-8");
		String requestURI = request.getRequestURI();
		// 进入处理器处理相应的请求
		try {
			viewsHandler.doDispatchHandle(requestURI, request, response);
		} catch (Exception e) {
			response.sendError(500);
		}
		
	}

	@Override
	public void destroy() {
		System.out.println("------ shutdown ------");
	}


}
