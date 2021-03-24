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

import cn.taketoday.context.OrderedSupport;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.conversion.ConversionService;
import cn.taketoday.context.conversion.DefaultConversionService;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebDataBinder;
import cn.taketoday.web.handler.MethodParameter;

/**
 * Resolve Bean
 *
 * @author TODAY <br>
 * 2019-07-13 01:11
 */
@MissingBean(type = DataBinderParameterResolver.class)
public class DataBinderParameterResolver
        extends OrderedSupport implements ParameterResolver {

  private ConversionService conversionService = DefaultConversionService.getSharedInstance();

  public DataBinderParameterResolver() {
    this(LOWEST_PRECEDENCE - HIGHEST_PRECEDENCE - 100);
  }

  public DataBinderParameterResolver(final int order) {
    super(order);
  }

  public DataBinderParameterResolver(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public boolean supports(MethodParameter parameter) {
    return !ClassUtils.isSimpleType(parameter.getParameterClass());
  }

  /**
   * @return Pojo parameter
   */
  @Override
  public Object resolveParameter(final RequestContext context, final MethodParameter parameter) {
    final Class<?> parameterClass = parameter.getParameterClass();
    final WebDataBinder dataBinder = new WebDataBinder(parameterClass);
    dataBinder.setConversionService(conversionService);
    return dataBinder.bind(context);
  }

  public void setConversionService(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  public ConversionService getConversionService() {
    return conversionService;
  }
}