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

package cn.taketoday.web.view;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.context.AttributeAccessorSupport;

/**
 * @author TODAY 2021/4/1 15:56
 * @since 3.0
 */
public class ModelAttributes extends AttributeAccessorSupport implements Model, Serializable {
  private static final long serialVersionUID = 1L;

  @Override
  public boolean containsAttribute(String name) {
    return super.hasAttribute(name);
  }

  @Override
  public Map<String, Object> asMap() {
    return getAttributes();
  }

  @Override
  protected HashMap<String, Object> createAttributes() {
    return new LinkedHashMap<>();
  }
}
