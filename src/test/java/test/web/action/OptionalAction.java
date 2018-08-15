/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package test.web.action;

import java.util.Optional;

import cn.taketoday.context.annotation.RestProcessor;
import cn.taketoday.web.annotation.GET;

/**
 * @author Today
 * @date 2018年7月7日 下午8:57:05
 */
@RestProcessor
public final class OptionalAction {

	public OptionalAction() {
		
	}

	@GET("/optional")
	public String optional(Optional<String> opt) {
		
		opt.ifPresent(opts -> {
			System.out.println(opts);
		});
		
		return "Optional";
	}
	
}
