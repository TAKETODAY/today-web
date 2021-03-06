/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.view;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.HandlerMethod;

/**
 * @author TODAY <br>
 * 2019-12-13 13:52
 */
public abstract class HandlerMethodResultHandler extends AbstractResultHandler {

  @Override
  public boolean supportsHandler(final Object handler) {
    return supportHandlerMethod(handler) && supports((HandlerMethod) handler);
  }

  public static boolean supportHandlerMethod(final Object handler) {
    return handler instanceof HandlerMethod;
  }

  protected abstract boolean supports(HandlerMethod handler);

  @Override
  public void handleResult(RequestContext context,
                           Object handler, Object result) throws Throwable {
    if (result != null) {
      handleInternal(context, (HandlerMethod) handler, result);
    }
  }

  protected void handleInternal(RequestContext context,
                                HandlerMethod handler,
                                Object result) throws Throwable { }
}
