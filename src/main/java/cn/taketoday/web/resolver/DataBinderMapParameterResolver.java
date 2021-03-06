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

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.factory.BeanMetadata;
import cn.taketoday.context.factory.DataBinder;
import cn.taketoday.context.factory.PropertyValue;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.MultiValueMap;
import cn.taketoday.web.handler.MethodParameter;

/**
 * @author TODAY 2019-07-09 22:49
 * @see <a href='https://taketoday.cn/articles/1616819014712'>TODAY Context 之 BeanPropertyAccessor</a>
 */
public class DataBinderMapParameterResolver
        extends AbstractDataBinderParameterResolver implements ParameterResolver {

  public DataBinderMapParameterResolver() {
    this(LOWEST_PRECEDENCE - HIGHEST_PRECEDENCE - 90);
  }

  public DataBinderMapParameterResolver(final int order) {
    setOrder(order);
  }

  @Override
  public boolean supportsInternal(final MethodParameter parameter) {
    if (isMap(parameter)) {
      final Type valueType = parameter.getGeneric(1);
      if (valueType instanceof Class) {
        return supportsSetProperties(valueType);
      }
    }
    return false;
  }

  public static boolean isMap(MethodParameter parameter) {
    return parameter.is(Map.class);
  }

  /**
   * Resolve {@link Map} parameter.
   */
  @Override
  protected Object doBind(MultiValueMap<String, PropertyValue> propertyValues, MethodParameter parameter) {
    final Map<String, Object> map = CollectionUtils.createMap(parameter.getParameterClass(), propertyValues.size());

    final DataBinder dataBinder = new DataBinder();
    final Class<?> parameterClass = (Class<?>) parameter.getGeneric(1);
    final BeanMetadata parameterMetadata = BeanMetadata.ofClass(parameterClass);
    for (final Map.Entry<String, List<PropertyValue>> entry : propertyValues.entrySet()) {
      final Object rootObject = parameterMetadata.newInstance();
      final List<PropertyValue> propertyValueList = entry.getValue();
      dataBinder.bind(rootObject, parameterMetadata, propertyValueList);

      map.put(entry.getKey(), rootObject);
    }
    return map;
  }

}
