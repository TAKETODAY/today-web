package com.yhj.web.utils;

import javax.servlet.http.HttpServletRequest;

public final class Tools  {

	public static boolean isAjax(HttpServletRequest request) {
		return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
	}

}
