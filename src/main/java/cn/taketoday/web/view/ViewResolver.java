<<<<<<< HEAD
/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.view;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.web.core.WebApplicationContext;

/**
 * @author Today
 * @date 2018年6月23日 上午11:59:50
 */
public interface ViewResolver {

	/**
	 * init View Resolver
	 * 
	 * @param configurationFactory
	 */
	void initViewResolver(WebApplicationContext applicationContext);

	/**
	 * resolve View
	 * 
	 * @param templateName
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	void resolveView(String templateName, HttpServletRequest request, HttpServletResponse response) throws Exception;

}
=======
/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.view;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.web.core.WebApplicationContext;

/**
 * 
 * @author Today <br>
 * 
 *         2018-06-23 11:59:50
 */
public interface ViewResolver {

	/**
	 * Init View Resolver.
	 * 
	 * @param applicationContext
	 *            application context
	 */
	void initViewResolver(WebApplicationContext applicationContext);

	/**
	 * Resolve View.
	 * 
	 * @param templateName
	 *            template name
	 * @param request
	 *            current request
	 * @param response
	 *            current response
	 * @throws Exception
	 */
	void resolveView(String templateName, HttpServletRequest request, HttpServletResponse response) throws Exception;

}
>>>>>>> 2.2.x
