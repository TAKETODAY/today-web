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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.factory.BeanMetadata;
import cn.taketoday.context.factory.DataBinder;
import cn.taketoday.context.factory.PropertyValue;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.MultiValueMap;
import cn.taketoday.web.handler.MethodParameter;

/**
 * resolve collection parameter
 *
 * <pre>
 *  class UserForm {
 *     int age;
 *
 *     String name;
 *
 *     String[] arr;
 *
 *     List<String> stringList;
 *
 *     Map<String, Integer> map;
 *  }
 *
 *  void test(List<UserForm> userList) { }
 *
 * </pre>
 *
 * @author TODAY 2021/4/8 14:33
 * @since 3.0
 */
public class DataBinderCollectionParameterResolver extends AbstractDataBinderParameterResolver {

  @Override
  public boolean supports(MethodParameter parameter) {
    return parameter.isCollection() && !ClassUtils.primitiveTypes.contains(parameter.getParameterClass());
  }

  @Override
  protected Object doBind(MultiValueMap<String, PropertyValue> propertyValues, MethodParameter parameter) {
    final Collection<Object> collection = createCollection(propertyValues, parameter);
    final boolean isList = collection instanceof List;

    final DataBinder dataBinder = new DataBinder();
    final Class<?> parameterClass = getComponentType(parameter);
    final BeanMetadata parameterMetadata = BeanMetadata.ofClass(parameterClass);


    for (final Map.Entry<String, List<PropertyValue>> entry : propertyValues.entrySet()) {
      final Object rootObject = parameterMetadata.newInstance();
      final List<PropertyValue> propertyValueList = entry.getValue();
      dataBinder.bind(rootObject, parameterMetadata, propertyValueList);

      if (isList) {
        try {
          final String key = entry.getKey();
          final int valueIndex = Integer.parseInt(key);
          CollectionUtils.setValue((List)collection, valueIndex, rootObject);
        }
        catch (NumberFormatException e) {
          throw new ParameterFormatException(parameter, e);
        }
      }
      else {
        collection.add(rootObject);
      }
    }
    return collection;
  }

  protected Collection<Object> createCollection(MultiValueMap<String, PropertyValue> propertyValues, MethodParameter parameter) {
    return CollectionUtils.createCollection(parameter.getParameterClass(), propertyValues.size());
  }

  protected Class<?> getComponentType(MethodParameter parameter) {
    return (Class<?>) parameter.getGenerics(0);
  }


}
