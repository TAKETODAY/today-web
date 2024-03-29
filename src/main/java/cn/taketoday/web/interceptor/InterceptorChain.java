/*
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

package cn.taketoday.web.interceptor;

import cn.taketoday.web.RequestContext;

/**
 * @author TODAY 2021/8/8 14:57
 * @since 4.0
 */
public abstract class InterceptorChain implements HandlerInterceptorsCapable {

  private final HandlerInterceptor[] interceptors;
  private int currentIndex = 0;

  protected InterceptorChain(HandlerInterceptor[] interceptors) {
    this.interceptors = interceptors;
  }

  public final Object proceed(RequestContext context, Object handler) throws Throwable {
    HandlerInterceptor[] interceptors = getInterceptors();
    if (currentIndex < interceptors.length) {
      HandlerInterceptor chainInterceptor = interceptors[currentIndex++];
      return chainInterceptor.intercept(context, handler, this);
    }
    return proceedTarget(context, handler);
  }

  protected abstract Object proceedTarget(RequestContext context, Object handler) throws Throwable;

  @Override
  public HandlerInterceptor[] getInterceptors() {
    return interceptors;
  }
}
