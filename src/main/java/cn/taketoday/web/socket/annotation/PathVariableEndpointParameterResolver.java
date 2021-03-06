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

package cn.taketoday.web.socket.annotation;

import java.util.Map;

import cn.taketoday.context.conversion.support.DefaultConversionService;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.resolver.MissingPathVariableParameterException;
import cn.taketoday.web.socket.WebSocketSession;

/**
 * @author TODAY 2021/5/9 21:59
 * @since 3.0.1
 */
public class PathVariableEndpointParameterResolver implements EndpointParameterResolver {

  @Override
  public boolean supports(MethodParameter parameter) {
    return parameter.isAnnotationPresent(PathVariable.class);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object resolve(WebSocketSession session, MethodParameter parameter) {
    final Object attribute = session.getAttribute(WebSocketSession.URI_TEMPLATE_VARIABLES);
    if (attribute instanceof Map) {
      final String value = ((Map<String, String>) attribute).get(resolveName(parameter));
      return DefaultConversionService.getSharedInstance()
              .convert(value, parameter.getGenericDescriptor());
    }
    throw new MissingPathVariableParameterException(parameter);
  }

  protected String resolveName(MethodParameter parameter) {
    return parameter.getName();
  }

}
