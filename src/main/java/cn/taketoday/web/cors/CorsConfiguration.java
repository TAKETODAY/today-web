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
package cn.taketoday.web.cors;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.RequestMethod;
import cn.taketoday.web.annotation.CrossOrigin;


/**
 * A container for CORS configuration along with methods to check against the
 * actual origin, HTTP methods, and headers of a given request.
 *
 * <p>
 * By default a newly created {@code CorsConfiguration} does not permit any
 * cross-origin requests and must be configured explicitly to indicate what
 * should be allowed. Use {@link #applyPermitDefaultValues()} to flip the
 * initialization model to start with open defaults that permit all cross-origin
 * requests for GET, HEAD, and POST requests.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author TODAY <br>
 * 2019-12-08 16:39
 * @see <a href="https://www.w3.org/TR/cors/">CORS spec</a>
 * @since 2.3.7
 */
public class CorsConfiguration {

  /** Wildcard representing <em>all</em> origins, methods, or headers. */
  public static final String ALL = "*";
  private static final List<String> DEFAULT_METHODS = Collections.unmodifiableList(Arrays.asList("GET", "HEAD"));
  private static final List<String> DEFAULT_PERMIT_ALL = Collections.singletonList(ALL);
  private static final List<String> DEFAULT_PERMIT_METHODS = Collections.unmodifiableList(Arrays.asList("GET", "HEAD", "POST"));

  private Long maxAge;
  private Boolean allowCredentials;
  private List<String> allowedOrigins;
  private List<String> allowedMethods;
  private List<String> allowedHeaders;
  private List<String> exposedHeaders;
  private List<String> resolvedMethods = DEFAULT_METHODS;

  /** @since 3.0 */
  private List<OriginPattern> allowedOriginPatterns;
  /** @since 3.0 */
  private static final OriginPattern ALL_PATTERN = new OriginPattern("*");
  /** @since 3.0 */
  private static final List<OriginPattern> ALL_PATTERN_LIST = Collections.singletonList(ALL_PATTERN);
  /** @since 3.0 */
  private static final List<String> ALL_LIST = Collections.singletonList(ALL);

  /**
   * Construct a new {@code CorsConfiguration} instance with no cross-origin
   * requests allowed for any origin by default.
   *
   * @see #applyPermitDefaultValues()
   */
  public CorsConfiguration() {}

  /**
   * Construct a new {@code CorsConfiguration} instance by copying all values from
   * the supplied {@code CorsConfiguration}.
   */
  public CorsConfiguration(CorsConfiguration other) {
    this.maxAge = other.maxAge;
    this.allowedOrigins = other.allowedOrigins;
    this.allowedOriginPatterns = other.allowedOriginPatterns;
    this.allowedMethods = other.allowedMethods;
    this.resolvedMethods = other.resolvedMethods;
    this.allowedHeaders = other.allowedHeaders;
    this.exposedHeaders = other.exposedHeaders;
    this.allowCredentials = other.allowCredentials;
  }

  /**
   * Set the origins to allow, e.g. {@code "https://domain1.com"}.
   * <p>
   * The special value {@code "*"} allows all domains.
   * <p>
   * By default this is not set.
   */
  public void setAllowedOrigins(List<String> allowedOrigins) {
    this.allowedOrigins = (allowedOrigins != null ? new ArrayList<>(allowedOrigins) : null);
  }

  /**
   * Return the configured origins to allow, or {@code null} if none.
   *
   * @see #addAllowedOrigin(String)
   * @see #setAllowedOrigins(List)
   */
  public List<String> getAllowedOrigins() {
    return this.allowedOrigins;
  }

  /**
   * Add an origin to allow.
   */
  public void addAllowedOrigin(String origin) {
    if (this.allowedOrigins == null) {
      this.allowedOrigins = new ArrayList<>(4);
    }
    else if (this.allowedOrigins == DEFAULT_PERMIT_ALL && CollectionUtils.isEmpty(this.allowedOriginPatterns)) {
      setAllowedOrigins(DEFAULT_PERMIT_ALL);
    }
    this.allowedOrigins.add(origin);
  }

  /**
   * Set the HTTP methods to allow, e.g. {@code "GET"}, {@code "POST"},
   * {@code "PUT"}, etc.
   * <p>
   * The special value {@code "*"} allows all methods.
   * <p>
   * If not set, only {@code "GET"} and {@code "HEAD"} are allowed.
   * <p>
   * By default this is not set.
   * <p>
   * <strong>Note:</strong> CORS checks use values from "Forwarded"
   * (<a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>),
   * "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" headers, if
   * present, in order to reflect the client-originated address. Consider using
   * the {@code ForwardedHeaderFilter} in order to choose from a central place
   * whether to extract and use, or to discard such headers. See the
   * Framework reference for more on this filter.
   */
  public void setAllowedMethods(List<String> allowedMethods) {
    this.allowedMethods = (allowedMethods != null ? new ArrayList<>(allowedMethods) : null);
    if (ObjectUtils.isNotEmpty(allowedMethods)) {
      this.resolvedMethods = new ArrayList<>(allowedMethods.size());
      for (String method : allowedMethods) {
        if (ALL.equals(method)) {
          this.resolvedMethods = null;
          break;
        }
        this.resolvedMethods.add(method);
      }
    }
    else {
      this.resolvedMethods = DEFAULT_METHODS;
    }
  }

  /**
   * Return the allowed HTTP methods, or {@code null} in which case only
   * {@code "GET"} and {@code "HEAD"} allowed.
   *
   * @see #addAllowedMethod(RequestMethod)
   * @see #addAllowedMethod(String)
   * @see #setAllowedMethods(List)
   */
  public List<String> getAllowedMethods() {
    return this.allowedMethods;
  }

  /**
   * Add an HTTP method to allow.
   */
  public void addAllowedMethod(RequestMethod method) {
    addAllowedMethod(method.name());
  }

  /**
   * Add an HTTP method to allow.
   */
  public void addAllowedMethod(final String method) {
    if (StringUtils.isNotEmpty(method)) {
      if (this.allowedMethods == null) {
        this.allowedMethods = new ArrayList<>(4);
        this.resolvedMethods = new ArrayList<>(4);
      }
      else if (this.allowedMethods == DEFAULT_PERMIT_METHODS) {
        setAllowedMethods(DEFAULT_PERMIT_METHODS);
      }
      this.allowedMethods.add(method);
      if (ALL.equals(method)) {
        this.resolvedMethods = null;
      }
      else if (this.resolvedMethods != null) {
        this.resolvedMethods.add(method);
      }
    }
  }

  /**
   * Set the list of headers that a pre-flight request can list as allowed for use
   * during an actual request.
   * <p>
   * The special value {@code "*"} allows actual requests to send any header.
   * <p>
   * A header name is not required to be listed if it is one of:
   * {@code Cache-Control}, {@code Content-Language}, {@code Expires},
   * {@code Last-Modified}, or {@code Pragma}.
   * <p>
   * By default this is not set.
   */
  public void setAllowedHeaders(final List<String> allowedHeaders) {
    this.allowedHeaders = (allowedHeaders != null ? new ArrayList<>(allowedHeaders) : null);
  }

  /**
   * Return the allowed actual request headers, or {@code null} if none.
   *
   * @see #addAllowedHeader(String)
   * @see #setAllowedHeaders(List)
   */
  public List<String> getAllowedHeaders() {
    return this.allowedHeaders;
  }

  /**
   * Add an actual request header to allow.
   */
  public void addAllowedHeader(String allowedHeader) {
    if (this.allowedHeaders == null) {
      this.allowedHeaders = new ArrayList<>(4);
    }
    else if (this.allowedHeaders == DEFAULT_PERMIT_ALL) {
      setAllowedHeaders(DEFAULT_PERMIT_ALL);
    }
    this.allowedHeaders.add(allowedHeader);
  }

  /**
   * Set the list of response headers other than simple headers (i.e.
   * {@code Cache-Control}, {@code Content-Language}, {@code Content-Type},
   * {@code Expires}, {@code Last-Modified}, or {@code Pragma}) that an actual
   * response might have and can be exposed.
   * <p>
   * Note that {@code "*"} is not a valid exposed header value.
   * <p>
   * By default this is not set.
   */
  public void setExposedHeaders(final List<String> exposedHeaders) {
    this.exposedHeaders = (exposedHeaders != null ? new ArrayList<>(exposedHeaders) : null);
  }

  /**
   * Return the configured response headers to expose, or {@code null} if none.
   *
   * @see #addExposedHeader(String)
   * @see #setExposedHeaders(List)
   */
  public List<String> getExposedHeaders() {
    return this.exposedHeaders;
  }

  /**
   * Add a response header to expose.
   * <p>The special value {@code "*"} allows all headers to be exposed for
   * non-credentialed requests.
   */
  public void addExposedHeader(String exposedHeader) {
    if (this.exposedHeaders == null) {
      this.exposedHeaders = new ArrayList<>(4);
    }
    this.exposedHeaders.add(exposedHeader);
  }


  /**
   * Whether user credentials are supported.
   * <p>
   * By default this is not set (i.e. user credentials are not supported).
   */
  public void setAllowCredentials(Boolean allowCredentials) {
    this.allowCredentials = allowCredentials;
  }

  /**
   * Return the configured {@code allowCredentials} flag, or {@code null} if none.
   *
   * @see #setAllowCredentials(Boolean)
   */
  public Boolean getAllowCredentials() {
    return this.allowCredentials;
  }

  /**
   * Configure how long, as a duration, the response from a pre-flight request can
   * be cached by clients.
   *
   * @see #setMaxAge(Long)
   */
  public void setMaxAge(Duration maxAge) {
    this.maxAge = maxAge.getSeconds();
  }

  /**
   * Configure how long, in seconds, the response from a pre-flight request can be
   * cached by clients.
   * <p>
   * By default this is not set.
   */
  public void setMaxAge(Long maxAge) {
    this.maxAge = maxAge;
  }

  /**
   * Return the configured {@code maxAge} value, or {@code null} if none.
   *
   * @see #setMaxAge(Long)
   */
  public Long getMaxAge() {
    return this.maxAge;
  }

  /**
   * By default a newly created {@code CorsConfiguration} does not permit any
   * cross-origin requests and must be configured explicitly to indicate what
   * should be allowed.
   * <p>
   * Use this method to flip the initialization model to start with open defaults
   * that permit all cross-origin requests for GET, HEAD, and POST requests. Note
   * however that this method will not override any existing values already set.
   * <p>
   * The following defaults are applied if not already set:
   * <ul>
   * <li>Allow all origins.</li>
   * <li>Allow "simple" methods {@code GET}, {@code HEAD} and {@code POST}.</li>
   * <li>Allow all headers.</li>
   * <li>Set max age to 1800 seconds (30 minutes).</li>
   * </ul>
   */
  public CorsConfiguration applyPermitDefaultValues() {
    if (this.allowedOrigins == null && this.allowedOriginPatterns == null) {
      this.allowedOrigins = DEFAULT_PERMIT_ALL;
    }
    if (this.allowedMethods == null) {
      this.allowedMethods = DEFAULT_PERMIT_METHODS;
      this.resolvedMethods = DEFAULT_PERMIT_METHODS;
    }
    if (this.allowedHeaders == null) {
      this.allowedHeaders = DEFAULT_PERMIT_ALL;
    }
    if (this.maxAge == null) {
      this.maxAge = 1800L;
    }
    return this;
  }

  /**
   * Combine the non-null properties of the supplied {@code CorsConfiguration}
   * with this one.
   * <p>
   * When combining single values like {@code allowCredentials} or {@code maxAge},
   * {@code this} properties are overridden by non-null {@code other} properties
   * if any.
   * <p>
   * Combining lists like {@code allowedOrigins}, {@code allowedMethods},
   * {@code allowedHeaders} or {@code exposedHeaders} is done in an additive way.
   * For example, combining {@code ["GET", "POST"]} with {@code ["PATCH"]} results
   * in {@code ["GET", "POST", "PATCH"]}, but keep in mind that combining
   * {@code ["GET", "POST"]} with {@code ["*"]} results in {@code ["*"]}.
   * <p>
   * Notice that default permit values set by
   * {@link CorsConfiguration#applyPermitDefaultValues()} are overridden by any
   * value explicitly defined.
   *
   * @return the combined {@code CorsConfiguration}, or {@code this} configuration
   * if the supplied configuration is {@code null}
   */
  public CorsConfiguration combine(CorsConfiguration other) {
    if (other == null) {
      return this;
    }
    CorsConfiguration config = new CorsConfiguration(this);

    List<String> origins = combine(getAllowedOrigins(), other.getAllowedOrigins());
    List<OriginPattern> patterns = combinePatterns(this.allowedOriginPatterns, other.allowedOriginPatterns);
    config.allowedOrigins = (origins == DEFAULT_PERMIT_ALL && !CollectionUtils.isEmpty(patterns) ? null : origins);
    config.allowedOriginPatterns = patterns;

    config.setAllowedMethods(combine(getAllowedMethods(), other.getAllowedMethods()));
    config.setAllowedHeaders(combine(getAllowedHeaders(), other.getAllowedHeaders()));
    config.setExposedHeaders(combine(getExposedHeaders(), other.getExposedHeaders()));

    Boolean allowCredentials = other.getAllowCredentials();
    if (allowCredentials != null) {
      config.setAllowCredentials(allowCredentials);
    }
    Long maxAge = other.getMaxAge();
    if (maxAge != null) {
      config.setMaxAge(maxAge);
    }
    return config;
  }

  private List<String> combine(List<String> source, List<String> other) {
    if (other == null) {
      return (source != null ? source : Collections.emptyList());
    }
    if (source == null) {
      return other;
    }
    if (source == DEFAULT_PERMIT_ALL || source == DEFAULT_PERMIT_METHODS) {
      return other;
    }
    if (other == DEFAULT_PERMIT_ALL || other == DEFAULT_PERMIT_METHODS) {
      return source;
    }
    if (source.contains(ALL) || other.contains(ALL)) {
      return ALL_LIST;
    }
    LinkedHashSet<String> combined = new LinkedHashSet<>(source);
    combined.addAll(other);
    return new ArrayList<>(combined);
  }

  private List<OriginPattern> combinePatterns(
          List<OriginPattern> source, List<OriginPattern> other) {

    if (other == null) {
      return (source != null ? source : Collections.emptyList());
    }
    if (source == null) {
      return other;
    }
    if (source.contains(ALL_PATTERN) || other.contains(ALL_PATTERN)) {
      return ALL_PATTERN_LIST;
    }
    Set<OriginPattern> combined = new LinkedHashSet<>(source.size() + other.size());
    combined.addAll(source);
    combined.addAll(other);
    return new ArrayList<>(combined);
  }

  /**
   * Check the origin of the request against the configured allowed origins.
   *
   * @param requestOrigin
   *         the origin to check
   *
   * @return the origin to use for the response, or {@code null} which means the
   * request origin is not allowed
   */
  public String checkOrigin(final String requestOrigin) {
    if (StringUtils.isEmpty(requestOrigin)) {
      return null;
    }
    final List<String> allowedOrigins = this.allowedOrigins;
    if (ObjectUtils.isNotEmpty(allowedOrigins)) {
      if (allowedOrigins.contains(ALL)) {
        validateAllowCredentials();
        return ALL;
      }
      for (final String allowedOrigin : allowedOrigins) {
        if (requestOrigin.equalsIgnoreCase(allowedOrigin)) {
          return requestOrigin;
        }
      }
    }

    if (ObjectUtils.isNotEmpty(this.allowedOriginPatterns)) {
      for (OriginPattern p : this.allowedOriginPatterns) {
        if (p.getDeclaredPattern().equals(ALL) || p.getPattern().matcher(requestOrigin).matches()) {
          return requestOrigin;
        }
      }
    }

    return null;
  }

  /**
   * Validate that when {@link #setAllowCredentials allowCredentials} is true,
   * {@link #setAllowedOrigins allowedOrigins} does not contain the special
   * value {@code "*"} since in that case the "Access-Control-Allow-Origin"
   * cannot be set to {@code "*"}.
   *
   * @throws IllegalArgumentException
   *         if the validation fails
   * @since 3.0
   */
  public void validateAllowCredentials() {
    if (this.allowCredentials == Boolean.TRUE &&
            this.allowedOrigins != null && this.allowedOrigins.contains(ALL)) {

      throw new IllegalArgumentException(
              "When allowCredentials is true, allowedOrigins cannot contain the special value \"*\" " +
                      "since that cannot be set on the \"Access-Control-Allow-Origin\" response header. " +
                      "To allow credentials to a set of origins, list them explicitly " +
                      "or consider using \"allowedOriginPatterns\" instead.");
    }
  }

  /**
   * Check the supplied request headers (or the headers listed in the
   * {@code Access-Control-Request-Headers} of a pre-flight request) against the
   * configured allowed headers.
   *
   * @param requestHeaders
   *         the request headers to check
   *
   * @return the list of allowed headers to list in the response of a pre-flight
   * request, or {@code null} if none of the supplied request headers is
   * allowed
   */
  public List<String> checkHeaders(final List<String> requestHeaders) {

    if (requestHeaders == null) {
      return null;
    }
    if (requestHeaders.isEmpty()) {
      return Collections.emptyList();
    }
    final List<String> allowedHeaders = this.allowedHeaders;
    if (ObjectUtils.isEmpty(allowedHeaders)) {
      return null;
    }
    boolean allowAnyHeader = allowedHeaders.contains(ALL);
    List<String> result = new ArrayList<>(requestHeaders.size());
    for (String requestHeader : requestHeaders) {
      if (StringUtils.isNotEmpty(requestHeader)) {
        requestHeader = requestHeader.trim();
        if (allowAnyHeader) {
          result.add(requestHeader);
        }
        else {
          for (final String allowedHeader : allowedHeaders) {
            if (requestHeader.equalsIgnoreCase(allowedHeader)) {
              result.add(requestHeader);
              break;
            }
          }
        }
      }
    }
    return (result.isEmpty() ? null : result);
  }

  /**
   * Check the HTTP request method (or the method from the
   * {@code Access-Control-Request-Method} header on a pre-flight request) against
   * the configured allowed methods.
   *
   * @param method
   *         the HTTP request method to check
   *
   * @return the list of HTTP methods to list in the response of a pre-flight
   * request, or {@code null} if the supplied {@code requestMethod} is not
   * allowed
   */
  public List<String> checkHttpMethod(String method) {
    if (method == null) {
      return null;
    }
    if (this.resolvedMethods == null) {
      return Collections.singletonList(method);
    }
    return (this.resolvedMethods.contains(method) ? this.resolvedMethods : null);
  }

  public void updateCorsConfig(CrossOrigin annotation) {
    if (annotation == null) {
      return;
    }
    for (String origin : annotation.value()) {
      addAllowedOrigin(resolveCorsValue(origin));
    }
    for (RequestMethod method : annotation.methods()) {
      addAllowedMethod(method.name());
    }
    for (String header : annotation.allowedHeaders()) {
      addAllowedHeader(resolveCorsValue(header));
    }
    for (String header : annotation.exposedHeaders()) {
      addExposedHeader(resolveCorsValue(header));
    }

    String allowCredentials = resolveCorsValue(annotation.allowCredentials());
    if ("true" .equalsIgnoreCase(allowCredentials)) {
      setAllowCredentials(true);
    }
    else if ("false" .equalsIgnoreCase(allowCredentials)) {
      setAllowCredentials(false);
    }
    else if (!allowCredentials.isEmpty()) {
      throw new IllegalStateException("@CrossOrigin's allowCredentials value must be \"true\", \"false\", " +
                                              "or an empty string (\"\"): current value is [" + allowCredentials + "]");
    }

    if (annotation.maxAge() >= 0 && getMaxAge() == null) {
      setMaxAge(annotation.maxAge());
    }
  }

  protected String resolveCorsValue(String value) {
    return ContextUtils.getExpressionEvaluator().evaluate(value, String.class);
  }

  /**
   * Alternative to {@link #setAllowedOrigins} that supports origins declared
   * via wildcard patterns. In contrast to {@link #setAllowedOrigins allowedOrigins}
   * which does support the special value {@code "*"}, this property allows
   * more flexible patterns, e.g. {@code "https://*.domain1.com"}. Furthermore
   * it always sets the {@code Access-Control-Allow-Origin} response header to
   * the matched origin and never to {@code "*"}, nor to any other pattern, and
   * therefore can be used in combination with {@link #setAllowCredentials}
   * set to {@code true}.
   * <p>By default this is not set.
   *
   * @since 3.0
   */
  public CorsConfiguration setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
    if (allowedOriginPatterns == null) {
      this.allowedOriginPatterns = null;
    }
    else {
      this.allowedOriginPatterns = new ArrayList<>(allowedOriginPatterns.size());
      for (String patternValue : allowedOriginPatterns) {
        addAllowedOriginPattern(patternValue);
      }
    }
    return this;
  }

  /**
   * Return the configured origins patterns to allow, or {@code null} if none.
   *
   * @since 3.0
   */
  public List<String> getAllowedOriginPatterns() {
    if (this.allowedOriginPatterns == null) {
      return null;
    }
    return this.allowedOriginPatterns.stream()
            .map(OriginPattern::getDeclaredPattern)
            .collect(Collectors.toList());
  }

  /**
   * Variant of {@link #setAllowedOriginPatterns} for adding one origin at a time.
   *
   * @since 3.0
   */
  public void addAllowedOriginPattern(String originPattern) {
    if (this.allowedOriginPatterns == null) {
      this.allowedOriginPatterns = new ArrayList<>(4);
    }
    this.allowedOriginPatterns.add(new OriginPattern(originPattern));
    if (this.allowedOrigins == DEFAULT_PERMIT_ALL) {
      this.allowedOrigins = null;
    }
  }

  /**
   * Contains both the user-declared pattern (e.g. "https://*.domain.com") and
   * the regex {@link Pattern} derived from it.
   */
  private static class OriginPattern {
    private final Pattern pattern;
    private final String declaredPattern;

    OriginPattern(String declaredPattern) {
      this.declaredPattern = declaredPattern;
      this.pattern = toPattern(declaredPattern);
    }

    private static Pattern toPattern(String patternValue) {
      patternValue = "\\Q" + patternValue + "\\E";
      patternValue = patternValue.replace("*", "\\E.*\\Q");
      return Pattern.compile(patternValue);
    }

    public String getDeclaredPattern() {
      return this.declaredPattern;
    }

    public Pattern getPattern() {
      return this.pattern;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (other == null || !getClass().equals(other.getClass())) {
        return false;
      }
      return Objects.equals(this.declaredPattern, ((OriginPattern) other).declaredPattern);
    }

    @Override
    public int hashCode() {
      return this.declaredPattern.hashCode();
    }

    @Override
    public String toString() {
      return this.declaredPattern;
    }
  }

}
