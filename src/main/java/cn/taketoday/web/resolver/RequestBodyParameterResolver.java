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
package cn.taketoday.web.resolver;

import java.io.IOException;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.view.MessageConverter;

/**
 * @author TODAY <br>
 * 2019-07-12 22:23
 */
public class RequestBodyParameterResolver
        extends OrderedAbstractParameterResolver implements ParameterResolver {

  private MessageConverter messageConverter;

  public RequestBodyParameterResolver() {
    setOrder(HIGHEST_PRECEDENCE);
  }

  public RequestBodyParameterResolver(MessageConverter messageConverter) {
    setMessageConverter(messageConverter);
  }

  @Override
  public boolean supports(final MethodParameter parameter) {
    return parameter.isAnnotationPresent(RequestBody.class);
  }

  @Override
  protected Object resolveInternal(final RequestContext context, final MethodParameter parameter) throws Throwable {
    try {
      return messageConverter.read(context, parameter);
    }
    catch (IOException e) {
      throw new RequestBodyParsingException("Request body read failed", e);
    }
  }

  public MessageConverter getMessageConverter() {
    return messageConverter;
  }

  public void setMessageConverter(MessageConverter messageConverter) {
    this.messageConverter = messageConverter;
  }

}
