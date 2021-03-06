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

package cn.taketoday.web.resolver;

import java.lang.reflect.Type;
import java.util.Map;

import cn.taketoday.context.factory.BeanPropertyAccessor;
import cn.taketoday.context.factory.PropertyValue;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.DefaultMultiValueMap;
import cn.taketoday.context.utils.MultiValueMap;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.handler.MethodParameter;

/**
 * @author TODAY 2021/4/8 17:33
 * @see <a href='https://taketoday.cn/articles/1616819014712'>TODAY Context 之 BeanPropertyAccessor</a>
 * @since 3.0
 */
public abstract class AbstractDataBinderParameterResolver extends OrderedAbstractParameterResolver {

  @Override
  public final boolean supports(MethodParameter parameter) {
    return !parameter.isAnnotationPresent(RequestBody.class) && supportsInternal(parameter);
  }

  /**
   * @since 3.0.3 fix request body
   */
  protected abstract boolean supportsInternal(MethodParameter parameter);

  @Override
  protected Object resolveInternal(RequestContext context, MethodParameter parameter) throws Throwable {
    final String parameterName = parameter.getName();
    final int parameterNameLength = parameterName.length();
    // prepare property values
    final Map<String, String[]> parameters = context.getParameters();

    final DefaultMultiValueMap<String, PropertyValue> propertyValues = new DefaultMultiValueMap<>();
    for (final Map.Entry<String, String[]> entry : parameters.entrySet()) {
      final String[] paramValues = entry.getValue();
      if (ObjectUtils.isNotEmpty(paramValues)) {
        final String requestParameterName = entry.getKey();
        // users[key].userName=TODAY&users[key].age=20
        if (requestParameterName.startsWith(parameterName)
                && requestParameterName.charAt(parameterNameLength) == '[') {
          // userList[0].name  '.' 's index
          final int separatorIndex = BeanPropertyAccessor.getNestedPropertySeparatorIndex(requestParameterName);
          final String property = requestParameterName.substring(separatorIndex + 1);
          final int closeKey = requestParameterName.indexOf(']');
          final String key = requestParameterName.substring(parameterNameLength + 1, closeKey);

          final PropertyValue propertyValue = new PropertyValue(property, paramValues[0]);

          propertyValues.add(key, propertyValue);
        }
      }
    }

    return doBind(propertyValues, parameter);
  }

  /**
   * Bind {@code propertyValues} to object
   */
  protected abstract Object doBind(
          MultiValueMap<String, PropertyValue> propertyValues, MethodParameter parameter);

  protected boolean supportsSetProperties(final Type valueType) {
    return !ClassUtils.primitiveTypes.contains(valueType);
  }

}
