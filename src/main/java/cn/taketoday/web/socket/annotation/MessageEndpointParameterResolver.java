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

import cn.taketoday.context.conversion.ConversionService;
import cn.taketoday.context.conversion.TypeConverter;
import cn.taketoday.context.conversion.support.DefaultConversionService;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.GenericDescriptor;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.socket.WebSocketSession;

/**
 * {@link Message}
 *
 * @author TODAY 2021/5/13 21:17
 * @since 3.0.1
 */
public class MessageEndpointParameterResolver implements EndpointParameterResolver {

  private final Class<?> supportParameterType;
  private ConversionService conversionService;

  private TypeConverter converter;

  public MessageEndpointParameterResolver(Class<?> supportParameterType) {
    this(supportParameterType, DefaultConversionService.getSharedInstance());
  }

  public MessageEndpointParameterResolver(Class<?> supportParameterType, TypeConverter converter) {
    this(supportParameterType);
    this.converter = converter;
  }

  public MessageEndpointParameterResolver(Class<?> supportParameterType, ConversionService conversionService) {
    this.supportParameterType = supportParameterType;
    this.conversionService = conversionService;
  }

  @Override
  public boolean supports(MethodParameter parameter) {
    return parameter.isAnnotationPresent(Message.class)
            && parameter.is(supportParameterType);
  }

  @Override
  public Object resolve(
          WebSocketSession session, cn.taketoday.web.socket.Message<?> message, MethodParameter parameter) {
    final Object payload = message.getPayload();
    if (supportParameterType.isInstance(payload)) {
      return payload;
    }

    final TypeConverter converter = getConverter();
    final GenericDescriptor targetType = parameter.getGenericDescriptor();
    if (converter != null && converter.supports(targetType, payload.getClass())) {
      return converter.convert(targetType, payload);
    }
    final ConversionService conversionService = getConversionService();
    Assert.state(conversionService != null, "No ConversionService");
    return conversionService.convert(payload, targetType);
  }

  public ConversionService getConversionService() {
    return conversionService;
  }

  public void setConversionService(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  public void setConverter(TypeConverter converter) {
    this.converter = converter;
  }

  public TypeConverter getConverter() {
    return converter;
  }
}
