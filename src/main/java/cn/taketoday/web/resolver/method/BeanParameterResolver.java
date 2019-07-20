/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
package cn.taketoday.web.resolver.method;

import static cn.taketoday.context.utils.NumberUtils.toArrayObject;

import java.lang.reflect.Field;
import java.util.Enumeration;

import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mapping.MethodParameter;

/**
 * @author TODAY <br>
 *         2019-07-13 01:11
 */
public class BeanParameterResolver implements OrderedParameterResolver {

    @Override
    public boolean supports(MethodParameter parameter) {
        return true;
    }

    @Override
    public Object resolveParameter(RequestContext requestContext, MethodParameter parameter) throws Throwable {

        final Class<?> parameterClass = parameter.getParameterClass();

        final Object bean = ClassUtils.newInstance(parameterClass);

        final Enumeration<String> parameterNames = requestContext.parameterNames();

        while (parameterNames.hasMoreElements()) {
            // 遍历参数
            final String parameterName = parameterNames.nextElement();
            // 寻找参数
            try {
                resolvePojoParameter(requestContext, parameterName, bean, //
                        parameterClass.getDeclaredField(parameterName), parameter);
            }
            catch (NoSuchFieldException e) {
                // continue;
            }
        }

        return bean;
    }

    private void resolvePojoParameter(RequestContext request, //
            String parameterName, Object bean, Field field, MethodParameter methodParameter) throws Throwable //
    {

        final Class<?> type = field.getType();
        if (type.isArray()) {
            ClassUtils.makeAccessible(field)//
                    .set(bean, toArrayObject(request.parameters(parameterName), type));
        }
        else {
            final String parameter = request.parameter(parameterName);
            if (parameter != null) {
                ClassUtils.makeAccessible(field)//
                        .set(bean, ConvertUtils.convert(parameter, type));
            }
        }
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - HIGHEST_PRECEDENCE - 100;
    }
}