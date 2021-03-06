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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.web.Constant;

/**
 * This annotation may be used to annotate method parameters on request mappings
 * where a URI-template has been used in the path-mapping of the {@link ActionMapping}
 * annotation. The method parameter may be of type String, any Java primitive
 * type or any boxed version thereof.
 *
 * <p>For example:-
 * <pre><code>
 * &#64;RequestMapping("/bookings/{guest-id}")
 * public class BookingController {
 *
 *     &#64;RequestMapping
 *     public void processBookingRequest(@PathVariable("guest-id") String guestID) {
 *         // process booking from the given guest here
 *     }
 * }
 * </code></pre>
 *
 * <p>For example:-
 * <pre><code>
 * &#64;RequestMapping("/rewards/{vip-level}")
 * public class RewardController {
 *
 *     &#64;RequestMapping
 *     public void processReward(@PathVariable("vip-level") Integer vipLevel) {
 *         // process reward here
 *     }
 * }
 * </code></pre>
 *
 * @author TODAY 2018-06-29 16:27:12
 */
@RequestParam
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface PathVariable {

  /** Request parameter name in path */
  String value() default Constant.BLANK;

}
