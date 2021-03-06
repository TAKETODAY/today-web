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
package cn.taketoday.web.http;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cn.taketoday.context.io.Resource;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.MediaType;
import cn.taketoday.context.utils.MultiValueMap;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestMethod;
import cn.taketoday.web.resource.CacheControl;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Locale.US;

/**
 * A data structure representing HTTP request or response headers, mapping String header names
 * to a list of String values, also offering accessors for common application-level data types.
 *
 * <p>In addition to the regular methods defined by {@link Map}, this class offers many common
 * convenience methods, for example:
 * <ul>
 * <li>{@link #getFirst(String)} returns the first value associated with a given header name</li>
 * <li>{@link #add(String, String)} adds a header value to the list of values for a header name</li>
 * <li>{@link #set(String, String)} sets the header value to a single string value</li>
 * </ul>
 *
 * <p>Note that {@code HttpHeaders} generally treats header names in a case-insensitive manner.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Josh Long
 * @author Sam Brannen
 * @author TODAY 2020-01-28 17:15
 * @since 3.0
 */
public abstract class HttpHeaders
        implements Constant, /*Iterable<String>,*/ MultiValueMap<String, String> {

  /**
   * Pattern matching ETag multiple field values in headers such as "If-Match",
   * "If-None-Match".
   *
   * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">Section 2.3 of
   * RFC 7232</a>
   */
  public static final Pattern ETAG_HEADER_VALUE_PATTERN = Pattern.compile("\\*|\\s*((W\\/)?(\"[^\"]*\"))\\s*,?");
  public static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS = new DecimalFormatSymbols(Locale.ENGLISH);
  public static final ZoneId GMT = ZoneId.of("GMT");

  /**
   * Date formats with time zone as specified in the HTTP RFC to use for
   * formatting.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section
   * 7.1.1.1 of RFC 7231</a>
   */
  public static final DateTimeFormatter DATE_FORMATTER = ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", US).withZone(GMT);

  /**
   * Date formats with time zone as specified in the HTTP RFC to use for parsing.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section
   * 7.1.1.1 of RFC 7231</a>
   */
  public static final DateTimeFormatter[] DATE_PARSERS = new DateTimeFormatter[] { //
          DateTimeFormatter.RFC_1123_DATE_TIME, //
          ofPattern("EEEE, dd-MMM-yy HH:mm:ss zzz", US), //
          ofPattern("EEE MMM dd HH:mm:ss yyyy", US).withZone(GMT)
  };

  /**
   * Get the list of header values for the given header name, if any.
   *
   * @param headerName
   *         the header name
   *
   * @return the list of header values, or an empty list
   */
  public List<String> getOrEmpty(String headerName) {
    List<String> values = get(headerName);
    return (values != null ? values : Collections.emptyList());
  }

  /**
   * Set the list of acceptable {@linkplain MediaType media types}, as specified
   * by the {@code Accept} header.
   */
  public void setAccept(List<MediaType> acceptableMediaTypes) {
    set(ACCEPT, MediaType.toString(acceptableMediaTypes));
  }

  /**
   * Return the list of acceptable {@linkplain MediaType media types}, as
   * specified by the {@code Accept} header.
   * <p>
   * Returns an empty list when the acceptable media types are unspecified.
   */
  public List<MediaType> getAccept() {
    return MediaType.parseMediaTypes(get(ACCEPT));
  }

  /**
   * Set the acceptable language ranges, as specified by the
   * {@literal Accept-Language} header.
   */
  public void setAcceptLanguage(List<Locale.LanguageRange> languages) {
    Assert.notNull(languages, "LanguageRange List must not be null");
    final class Function0 implements Function<Locale.LanguageRange, String> {
      @Override
      public String apply(Locale.LanguageRange range) {
        if (range.getWeight() == Locale.LanguageRange.MAX_WEIGHT) {
          return range.getRange();
        }
        DecimalFormat decimal = new DecimalFormat("0.0", DECIMAL_FORMAT_SYMBOLS);
        return range.getRange() + ";q=" + decimal.format(range.getWeight());
      }
    }

    List<String> values = languages.stream().map(new Function0()).collect(Collectors.toList());
    set(ACCEPT_LANGUAGE, collectionToString(values));
  }

  /**
   * Return the language ranges from the {@literal "Accept-Language"} header.
   * <p>
   * If you only need sorted, preferred locales only use
   * {@link #getAcceptLanguageAsLocales()} or if you need to filter based on a
   * list of supported locales you can pass the returned list to
   * {@link Locale#filter(List, Collection)}.
   *
   * @throws IllegalArgumentException
   *         if the value cannot be converted to a language range
   */
  public List<Locale.LanguageRange> getAcceptLanguage() {
    String value = getFirst(ACCEPT_LANGUAGE);
    return (StringUtils.isNotEmpty(value) ? Locale.LanguageRange.parse(value) : Collections.emptyList());
  }

  /**
   * Variant of {@link #setAcceptLanguage(List)} using {@link Locale}'s.
   */
  public void setAcceptLanguageAsLocales(List<Locale> locales) {
    setAcceptLanguage(locales.stream()
                              .map(locale -> new Locale.LanguageRange(locale.toLanguageTag()))
                              .collect(Collectors.toList()));
  }

  /**
   * A variant of {@link #getAcceptLanguage()} that converts each
   * {@link java.util.Locale.LanguageRange} to a {@link Locale}.
   *
   * @return the locales or an empty list
   *
   * @throws IllegalArgumentException
   *         if the value cannot be converted to a locale
   */
  public List<Locale> getAcceptLanguageAsLocales() {
    List<Locale.LanguageRange> ranges = getAcceptLanguage();
    if (ranges.isEmpty()) {
      return Collections.emptyList();
    }
    return ranges.stream()
            .map(range -> Locale.forLanguageTag(range.getRange()))
            .filter(locale -> StringUtils.isNotEmpty(locale.getDisplayName()))
            .collect(Collectors.toList());
  }

  /**
   * Set the (new) value of the {@code Access-Control-Allow-Credentials} response
   * header.
   */
  public void setAccessControlAllowCredentials(boolean allowCredentials) {
    set(ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.toString(allowCredentials));
  }

  /**
   * Return the value of the {@code Access-Control-Allow-Credentials} response
   * header.
   */
  public boolean getAccessControlAllowCredentials() {
    return Boolean.parseBoolean(getFirst(ACCESS_CONTROL_ALLOW_CREDENTIALS));
  }

  /**
   * Set the (new) value of the {@code Access-Control-Allow-Headers} response
   * header.
   */
  public void setAccessControlAllowHeaders(List<String> allowedHeaders) {
    set(ACCESS_CONTROL_ALLOW_HEADERS, collectionToString(allowedHeaders));
  }

  /**
   * Return the value of the {@code Access-Control-Allow-Headers} response header.
   */
  public List<String> getAccessControlAllowHeaders() {
    return getValuesAsList(ACCESS_CONTROL_ALLOW_HEADERS);
  }

  /**
   * Set the (new) value of the {@code Access-Control-Allow-Methods} response
   * header.
   */
  public void setAccessControlAllowMethods(List<?> allowedMethods) {
    set(ACCESS_CONTROL_ALLOW_METHODS, collectionToString(allowedMethods));
  }

  /**
   * Return the value of the {@code Access-Control-Allow-Methods} response header.
   */
  public List<RequestMethod> getAccessControlAllowMethods() {
    List<RequestMethod> result = new ArrayList<>();
    String value = getFirst(ACCESS_CONTROL_ALLOW_METHODS);
    if (value != null) {
      String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
      for (String token : tokens) {
        result.add(RequestMethod.valueOf(token));
      }
    }
    return result;
  }

  /**
   * Set the (new) value of the {@code Access-Control-Allow-Origin} response
   * header.
   */
  public void setAccessControlAllowOrigin(String allowedOrigin) {
    setOrRemove(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
  }

  /**
   * Return the value of the {@code Access-Control-Allow-Origin} response header.
   */
  public String getAccessControlAllowOrigin() {
    return getFieldValues(ACCESS_CONTROL_ALLOW_ORIGIN);
  }

  /**
   * Set the (new) value of the {@code Access-Control-Expose-Headers} response
   * header.
   */
  public void setAccessControlExposeHeaders(List<String> exposedHeaders) {
    set(ACCESS_CONTROL_EXPOSE_HEADERS, collectionToString(exposedHeaders));
  }

  /**
   * Return the value of the {@code Access-Control-Expose-Headers} response
   * header.
   */
  public List<String> getAccessControlExposeHeaders() {
    return getValuesAsList(ACCESS_CONTROL_EXPOSE_HEADERS);
  }

  /**
   * Set the (new) value of the {@code Access-Control-Max-Age} response header.
   */
  public void setAccessControlMaxAge(Duration maxAge) {
    set(ACCESS_CONTROL_MAX_AGE, Long.toString(maxAge.getSeconds()));
  }

  /**
   * Set the (new) value of the {@code Access-Control-Max-Age} response header.
   */
  public void setAccessControlMaxAge(long maxAge) {
    set(ACCESS_CONTROL_MAX_AGE, Long.toString(maxAge));
  }

  /**
   * Return the value of the {@code Access-Control-Max-Age} response header.
   * <p>
   * Returns -1 when the max age is unknown.
   */
  public long getAccessControlMaxAge() {
    String value = getFirst(ACCESS_CONTROL_MAX_AGE);
    return (value != null ? Long.parseLong(value) : -1);
  }

  /**
   * Set the (new) value of the {@code Access-Control-Request-Headers} request
   * header.
   */
  public void setAccessControlRequestHeaders(List<String> requestHeaders) {
    set(ACCESS_CONTROL_REQUEST_HEADERS, collectionToString(requestHeaders));
  }

  /**
   * Return the value of the {@code Access-Control-Request-Headers} request
   * header.
   */
  public List<String> getAccessControlRequestHeaders() {
    return getValuesAsList(ACCESS_CONTROL_REQUEST_HEADERS);
  }

  /**
   * Set the (new) value of the {@code Access-Control-Request-Method} request
   * header.
   */
  public void setAccessControlRequestMethod(RequestMethod requestMethod) {
    setOrRemove(ACCESS_CONTROL_REQUEST_METHOD, (requestMethod != null ? requestMethod.name() : null));
  }

  /**
   * Return the value of the {@code Access-Control-Request-Method} request header.
   */
  public RequestMethod getAccessControlRequestMethod() {
    final String first = getFirst(ACCESS_CONTROL_REQUEST_METHOD);
    if (StringUtils.isEmpty(first)) {
      return null;
    }
    return RequestMethod.valueOf(first);
  }

  /**
   * Set the list of acceptable {@linkplain Charset charsets}, as specified by the
   * {@code Accept-Charset} header.
   */
  public void setAcceptCharset(List<Charset> acceptableCharsets) {
    StringJoiner joiner = new StringJoiner(", ");
    for (Charset charset : acceptableCharsets) {
      joiner.add(charset.name().toLowerCase(Locale.ENGLISH));
    }
    set(ACCEPT_CHARSET, joiner.toString());
  }

  /**
   * Return the list of acceptable {@linkplain Charset charsets}, as specified by
   * the {@code Accept-Charset} header.
   */
  public List<Charset> getAcceptCharset() {
    String value = getFirst(ACCEPT_CHARSET);
    if (value != null) {
      String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
      List<Charset> result = new ArrayList<>(tokens.length);
      for (String token : tokens) {
        int paramIdx = token.indexOf(';');
        String charsetName;
        if (paramIdx == -1) {
          charsetName = token;
        }
        else {
          charsetName = token.substring(0, paramIdx);
        }
        if (!charsetName.equals("*")) {
          result.add(Charset.forName(charsetName));
        }
      }
      return result;
    }
    else {
      return Collections.emptyList();
    }
  }

  /**
   * Set the set of allowed {@link RequestMethod HTTP methods}, as specified by
   * the {@code Allow} header.
   */
  public void setAllow(Set<RequestMethod> allowedMethods) {
    set(ALLOW, StringUtils.collectionToString(allowedMethods)); // special case
  }

  /**
   * Return the set of allowed {@link RequestMethod HTTP methods}, as specified by
   * the {@code Allow} header.
   * <p>
   * Returns an empty set when the allowed methods are unspecified.
   */
  public Set<RequestMethod> getAllow() {
    String value = getFirst(ALLOW);
    if (StringUtils.isNotEmpty(value)) {
      String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
      List<RequestMethod> result = new ArrayList<>(tokens.length);
      for (String token : tokens) {
        result.add(RequestMethod.valueOf(token));
      }
      return EnumSet.copyOf(result);
    }
    else {
      return EnumSet.noneOf(RequestMethod.class);
    }
  }

  /**
   * Set the value of the {@linkplain #AUTHORIZATION Authorization} header to
   * Basic Authentication
   *
   * @param encodedCredentials
   *         the encoded credentials
   *
   * @throws IllegalArgumentException
   *         if supplied credentials string is {@code null} or blank
   * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
   */
  public void setBasicAuth(String encodedCredentials) {
    if (StringUtils.isEmpty(encodedCredentials)) {
      throw new IllegalArgumentException("'encodedCredentials' must not be null or blank");
    }
    set(AUTHORIZATION, "Basic " .concat(encodedCredentials));
  }

  /**
   * Set the value of the {@linkplain #AUTHORIZATION Authorization} header to
   * Basic Authentication based on the given username and password.
   * <p>Note that this method only supports characters in the
   * {@link StandardCharsets#ISO_8859_1 ISO-8859-1} character set.
   *
   * @param username
   *         the username
   * @param password
   *         the password
   *
   * @throws IllegalArgumentException
   *         if either {@code user} or
   *         {@code password} contain characters that cannot be encoded to ISO-8859-1
   * @see #setBasicAuth(String)
   * @see #setBasicAuth(String, String, Charset)
   * @see #encodeBasicAuth(String, String, Charset)
   * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
   */
  public void setBasicAuth(String username, String password) {
    setBasicAuth(username, password, null);
  }

  /**
   * Set the value of the {@linkplain #AUTHORIZATION Authorization} header to
   * Basic Authentication based on the given username and password.
   *
   * @param username
   *         the username
   * @param password
   *         the password
   * @param charset
   *         the charset to use to convert the credentials into an octet
   *         sequence. Defaults to {@linkplain StandardCharsets#ISO_8859_1 ISO-8859-1}.
   *
   * @throws IllegalArgumentException
   *         if {@code username} or {@code password}
   *         contains characters that cannot be encoded to the given charset
   * @see #setBasicAuth(String)
   * @see #setBasicAuth(String, String)
   * @see #encodeBasicAuth(String, String, Charset)
   * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
   */
  public void setBasicAuth(String username, String password, Charset charset) {
    setBasicAuth(encodeBasicAuth(username, password, charset));
  }

  /**
   * Set the value of the {@linkplain #AUTHORIZATION Authorization} header to the
   * given Bearer token.
   *
   * @param token
   *         the Base64 encoded token
   *
   * @see <a href="https://tools.ietf.org/html/rfc6750">RFC 6750</a>
   */
  public void setBearerAuth(String token) {
    Assert.notNull(token, "The base64 encoded token must not be null");
    set(AUTHORIZATION, "Bearer " .concat(token));
  }

  /**
   * Set a configured {@link CacheControl} instance as the new value of the
   * {@code Cache-Control} header.
   */
  public void setCacheControl(CacheControl cacheControl) {
    setOrRemove(CACHE_CONTROL, cacheControl.getHeaderValue());
  }

  /**
   * Set the (new) value of the {@code Cache-Control} header.
   */
  public void setCacheControl(String cacheControl) {
    setOrRemove(CACHE_CONTROL, cacheControl);
  }

  /**
   * Return the value of the {@code Cache-Control} header.
   */
  public String getCacheControl() {
    return getFieldValues(CACHE_CONTROL);
  }

  /**
   * Set the (new) value of the {@code Connection} header.
   */
  public void setConnection(String connection) {
    set(CONNECTION, connection);
  }

  /**
   * Set the (new) value of the {@code Connection} header.
   */
  public void setConnection(List<String> connection) {
    set(CONNECTION, collectionToString(connection));
  }

  /**
   * Return the value of the {@code Connection} header.
   */
  public List<String> getConnection() {
    return getValuesAsList(CONNECTION);
  }

  /**
   * Set the {@code Content-Disposition} header when creating a
   * {@code "multipart/form-data"} request.
   * <p>
   * Applications typically would not set this header directly but rather prepare
   * a {@code MultiValueMap<String, Object>}, containing an Object or a
   * {@link Resource} for each part, and then pass that to the
   * {@code RestTemplate} or {@code WebClient}.
   *
   * @param name
   *         the control name
   * @param filename
   *         the filename (may be {@code null})
   *
   * @see #getContentDisposition()
   */
  public void setContentDispositionFormData(String name, String filename) {
    Assert.notNull(name, "Name must not be null");
    ContentDisposition.Builder disposition = ContentDisposition.builder("form-data").name(name);
    if (filename != null) {
      disposition.filename(filename);
    }
    setContentDisposition(disposition.build());
  }

  /**
   * Set the {@literal Content-Disposition} header.
   * <p>
   * This could be used on a response to indicate if the content is expected to be
   * displayed inline in the browser or as an attachment to be saved locally.
   * <p>
   * It can also be used for a {@code "multipart/form-data"} request. For more
   * details see notes on {@link #setContentDispositionFormData}.
   *
   * @see #getContentDisposition()
   */
  public void setContentDisposition(ContentDisposition contentDisposition) {
    set(CONTENT_DISPOSITION, contentDisposition.toString());
  }

  public void setContentDisposition(String contentDisposition) {
    set(CONTENT_DISPOSITION, contentDisposition);
  }

  /**
   * Return a parsed representation of the {@literal Content-Disposition} header.
   *
   * @see #setContentDisposition(ContentDisposition)
   */
  public ContentDisposition getContentDisposition() {
    String contentDisposition = getFirst(CONTENT_DISPOSITION);
    return contentDisposition != null
           ? ContentDisposition.parse(contentDisposition)
           : ContentDisposition.empty();
  }

  /**
   * Set the {@link Locale} of the content language, as specified by the
   * {@literal Content-Language} header.
   * <p>
   * Use {@code put(CONTENT_LANGUAGE, list)} if you need to set multiple content
   * languages.
   * </p>
   */
  public void setContentLanguage(Locale locale) {
    setOrRemove(CONTENT_LANGUAGE, (locale != null ? locale.toLanguageTag() : null));
  }

  /**
   * Return the first {@link Locale} of the content languages, as specified by the
   * {@literal Content-Language} header.
   * <p>
   * Returns {@code null} when the content language is unknown.
   * <p>
   * Use {@code getValuesAsList(CONTENT_LANGUAGE)} if you need to get multiple
   * content languages.
   * </p>
   */
  public Locale getContentLanguage() {
    return getValuesAsList(CONTENT_LANGUAGE)
            .stream()
            .findFirst()
            .map(Locale::forLanguageTag)
            .orElse(null);
  }

  /**
   * Set the length of the body in bytes, as specified by the
   * {@code Content-Length} header.
   */
  public void setContentLength(long contentLength) {
    set(CONTENT_LENGTH, Long.toString(contentLength));
  }

  /**
   * Return the length of the body in bytes, as specified by the
   * {@code Content-Length} header.
   * <p>
   * Returns -1 when the content-length is unknown.
   */
  public long getContentLength() {
    String value = getFirst(CONTENT_LENGTH);
    return (value != null ? Long.parseLong(value) : -1);
  }

  /**
   * Set the {@linkplain MediaType media type} of the body, as specified by the
   * {@code Content-Type} header.
   */
  public void setContentType(MediaType mediaType) {
    if (mediaType != null) {
      Assert.isTrue(!mediaType.isWildcardType(), "Content-Type cannot contain wildcard type '*'");
      Assert.isTrue(!mediaType.isWildcardSubtype(), "Content-Type cannot contain wildcard subtype '*'");
      set(CONTENT_TYPE, mediaType.toString());
    }
    else {
      remove(CONTENT_TYPE);
    }
  }

  /**
   * Return the {@linkplain MediaType media type} of the body, as specified by the
   * {@code Content-Type} header.
   * <p>
   * Returns {@code null} when the content-type is unknown.
   */
  public MediaType getContentType() {
    String value = getFirst(CONTENT_TYPE);
    return (StringUtils.isNotEmpty(value) ? MediaType.parseMediaType(value) : null);
  }

  /**
   * Set the date and time at which the message was created, as specified by the
   * {@code Date} header.
   */
  public void setDate(ZonedDateTime date) {
    setZonedDateTime(DATE, date);
  }

  /**
   * Set the date and time at which the message was created, as specified by the
   * {@code Date} header.
   */
  public void setDate(Instant date) {
    setInstant(DATE, date);
  }

  /**
   * Set the date and time at which the message was created, as specified by the
   * {@code Date} header.
   * <p>
   * The date should be specified as the number of milliseconds since January 1,
   * 1970 GMT.
   */
  public void setDate(long date) {
    setDate(DATE, date);
  }

  /**
   * Return the date and time at which the message was created, as specified by
   * the {@code Date} header.
   * <p>
   * The date is returned as the number of milliseconds since January 1, 1970 GMT.
   * Returns -1 when the date is unknown.
   *
   * @throws IllegalArgumentException
   *         if the value cannot be converted to a date
   */
  public long getDate() {
    return getFirstDate(DATE);
  }

  /**
   * Set the (new) entity tag of the body, as specified by the {@code ETag}
   * header.
   */
  public void setETag(String etag) {
    if (etag != null) {
      Assert.isTrue(etag.startsWith("\"") || etag.startsWith("W/"), "Invalid ETag: does not start with W/ or \"");
      Assert.isTrue(etag.endsWith("\""), "Invalid ETag: does not end with \"");
      set(ETAG, etag);
    }
    else {
      remove(ETAG);
    }
  }

  /**
   * Return the entity tag of the body, as specified by the {@code ETag} header.
   */
  public String getETag() {
    return getFirst(ETAG);
  }

  /**
   * Set the duration after which the message is no longer valid, as specified by
   * the {@code Expires} header.
   */
  public void setExpires(ZonedDateTime expires) {
    setZonedDateTime(EXPIRES, expires);
  }

  /**
   * Set the date and time at which the message is no longer valid, as specified
   * by the {@code Expires} header.
   */
  public void setExpires(Instant expires) {
    setInstant(EXPIRES, expires);
  }

  /**
   * Set the date and time at which the message is no longer valid, as specified
   * by the {@code Expires} header.
   * <p>
   * The date should be specified as the number of milliseconds since January 1,
   * 1970 GMT.
   */
  public void setExpires(long expires) {
    setDate(EXPIRES, expires);
  }

  /**
   * Return the date and time at which the message is no longer valid, as
   * specified by the {@code Expires} header.
   * <p>
   * The date is returned as the number of milliseconds since January 1, 1970 GMT.
   * Returns -1 when the date is unknown.
   *
   * @see #getFirstZonedDateTime(String)
   */
  public long getExpires() {
    return getFirstDate(EXPIRES, false);
  }

  /**
   * Set the (new) value of the {@code Host} header.
   * <p>
   * If the given {@linkplain InetSocketAddress#getPort() port} is {@code 0}, the
   * host header will only contain the
   * {@linkplain InetSocketAddress#getHostString() host name}.
   */
  public void setHost(InetSocketAddress host) {
    if (host != null) {
      String value = host.getHostString();
      int port = host.getPort();
      if (port != 0) {
        value = value + ':' + port;
      }
      set(HOST, value);
    }
    else {
      remove(HOST);
    }
  }

  /**
   * Return the value of the {@code Host} header, if available.
   * <p>
   * If the header value does not contain a port, the
   * {@linkplain InetSocketAddress#getPort() port} in the returned address will be
   * {@code 0}.
   */
  public InetSocketAddress getHost() {
    String value = getFirst(HOST);
    if (value == null) {
      return null;
    }

    String host = null;
    int port = 0;
    int separator = (value.startsWith("[") ? value.indexOf(':', value.indexOf(']')) : value.lastIndexOf(':'));
    if (separator != -1) {
      host = value.substring(0, separator);
      String portString = value.substring(separator + 1);
      try {
        port = Integer.parseInt(portString);
      }
      catch (NumberFormatException ex) {
        // ignore
      }
    }

    if (host == null) {
      host = value;
    }
    return InetSocketAddress.createUnresolved(host, port);
  }

  /**
   * Set the (new) value of the {@code If-Match} header.
   */
  public void setIfMatch(String ifMatch) {
    set(IF_MATCH, ifMatch);
  }

  /**
   * Set the (new) value of the {@code If-Match} header.
   */
  public void setIfMatch(List<String> ifMatchList) {
    set(IF_MATCH, collectionToString(ifMatchList));
  }

  /**
   * Return the value of the {@code If-Match} header.
   */
  public List<String> getIfMatch() {
    return getETagValuesAsList(IF_MATCH);
  }

  /**
   * Set the time the resource was last changed, as specified by the
   * {@code Last-Modified} header.
   */
  public void setIfModifiedSince(ZonedDateTime ifModifiedSince) {
    setZonedDateTime(IF_MODIFIED_SINCE, ifModifiedSince.withZoneSameInstant(GMT));
  }

  /**
   * Set the time the resource was last changed, as specified by the
   * {@code Last-Modified} header.
   */
  public void setIfModifiedSince(Instant ifModifiedSince) {
    setInstant(IF_MODIFIED_SINCE, ifModifiedSince);
  }

  /**
   * Set the (new) value of the {@code If-Modified-Since} header.
   * <p>
   * The date should be specified as the number of milliseconds since January 1,
   * 1970 GMT.
   */
  public void setIfModifiedSince(long ifModifiedSince) {
    setDate(IF_MODIFIED_SINCE, ifModifiedSince);
  }

  /**
   * Return the value of the {@code If-Modified-Since} header.
   * <p>
   * The date is returned as the number of milliseconds since January 1, 1970 GMT.
   * Returns -1 when the date is unknown.
   *
   * @see #getFirstZonedDateTime(String)
   */
  public long getIfModifiedSince() {
    return getFirstDate(IF_MODIFIED_SINCE, false);
  }

  /**
   * Set the (new) value of the {@code If-None-Match} header.
   */
  public void setIfNoneMatch(String ifNoneMatch) {
    set(IF_NONE_MATCH, ifNoneMatch);
  }

  /**
   * Set the (new) values of the {@code If-None-Match} header.
   */
  public void setIfNoneMatch(List<String> ifNoneMatchList) {
    set(IF_NONE_MATCH, collectionToString(ifNoneMatchList));
  }

  /**
   * Return the value of the {@code If-None-Match} header.
   */
  public List<String> getIfNoneMatch() {
    return getETagValuesAsList(IF_NONE_MATCH);
  }

  /**
   * Set the time the resource was last changed, as specified by the
   * {@code Last-Modified} header.
   */
  public void setIfUnmodifiedSince(ZonedDateTime ifUnmodifiedSince) {
    setZonedDateTime(IF_UNMODIFIED_SINCE, ifUnmodifiedSince.withZoneSameInstant(GMT));
  }

  /**
   * Set the time the resource was last changed, as specified by the
   * {@code Last-Modified} header.
   */
  public void setIfUnmodifiedSince(Instant ifUnmodifiedSince) {
    setInstant(IF_UNMODIFIED_SINCE, ifUnmodifiedSince);
  }

  /**
   * Set the (new) value of the {@code If-Unmodified-Since} header.
   * <p>
   * The date should be specified as the number of milliseconds since January 1,
   * 1970 GMT.
   */
  public void setIfUnmodifiedSince(long ifUnmodifiedSince) {
    setDate(IF_UNMODIFIED_SINCE, ifUnmodifiedSince);
  }

  /**
   * Return the value of the {@code If-Unmodified-Since} header.
   * <p>
   * The date is returned as the number of milliseconds since January 1, 1970 GMT.
   * Returns -1 when the date is unknown.
   *
   * @see #getFirstZonedDateTime(String)
   */
  public long getIfUnmodifiedSince() {
    return getFirstDate(IF_UNMODIFIED_SINCE, false);
  }

  /**
   * Set the time the resource was last changed, as specified by the
   * {@code Last-Modified} header.
   */
  public void setLastModified(ZonedDateTime lastModified) {
    setZonedDateTime(LAST_MODIFIED, lastModified.withZoneSameInstant(GMT));
  }

  /**
   * Set the time the resource was last changed, as specified by the
   * {@code Last-Modified} header.
   */
  public void setLastModified(Instant lastModified) {
    setInstant(LAST_MODIFIED, lastModified);
  }

  /**
   * Set the time the resource was last changed, as specified by the
   * {@code Last-Modified} header.
   * <p>
   * The date should be specified as the number of milliseconds since January 1,
   * 1970 GMT.
   */
  public void setLastModified(long lastModified) {
    setDate(LAST_MODIFIED, lastModified);
  }

  /**
   * Return the time the resource was last changed, as specified by the
   * {@code Last-Modified} header.
   * <p>
   * The date is returned as the number of milliseconds since January 1, 1970 GMT.
   * Returns -1 when the date is unknown.
   *
   * @see #getFirstZonedDateTime(String)
   */
  public long getLastModified() {
    return getFirstDate(LAST_MODIFIED, false);
  }

  /**
   * Set the (new) location of a resource, as specified by the {@code Location}
   * header.
   */
  public void setLocation(URI location) {
    setOrRemove(LOCATION, (location != null ? location.toASCIIString() : null));
  }

  /**
   * Return the (new) location of a resource as specified by the {@code Location}
   * header.
   * <p>
   * Returns {@code null} when the location is unknown.
   */
  public URI getLocation() {
    String value = getFirst(LOCATION);
    return (value != null ? URI.create(value) : null);
  }

  /**
   * Set the (new) value of the {@code Origin} header.
   */
  public void setOrigin(String origin) {
    setOrRemove(ORIGIN, origin);
  }

  /**
   * Return the value of the {@code Origin} header.
   */
  public String getOrigin() {
    return getFirst(ORIGIN);
  }

  /**
   * Set the (new) value of the {@code Pragma} header.
   */
  public void setPragma(String pragma) {
    setOrRemove(PRAGMA, pragma);
  }

  /**
   * Return the value of the {@code Pragma} header.
   */
  public String getPragma() {
    return getFirst(PRAGMA);
  }

  /**
   * Set the (new) value of the {@code Upgrade} header.
   */
  public void setUpgrade(String upgrade) {
    setOrRemove(UPGRADE, upgrade);
  }

  /**
   * Return the value of the {@code Upgrade} header.
   */
  public String getUpgrade() {
    return getFirst(UPGRADE);
  }

  /**
   * Set the request header names (e.g. "Accept-Language") for which the response
   * is subject to content negotiation and variances based on the value of those
   * request headers.
   *
   * @param requestHeaders
   *         the request header names
   */
  public void setVary(List<String> requestHeaders) {
    set(VARY, StringUtils.collectionToString(requestHeaders, ", "));
  }

  /**
   * Return the request header names subject to content negotiation.
   */
  public List<String> getVary() {
    return getValuesAsList(VARY);
  }

  /**
   * Set the given date under the given header name after formatting it as a
   * string using the RFC-1123 date-time formatter. The equivalent of
   * {@link #set(String, String)} but for date headers.
   */
  public void setZonedDateTime(String headerName, ZonedDateTime date) {
    set(headerName, DATE_FORMATTER.format(date));
  }

  /**
   * Set the given date under the given header name after formatting it as a
   * string using the RFC-1123 date-time formatter. The equivalent of
   * {@link #set(String, String)} but for date headers.
   */
  public void setInstant(String headerName, Instant date) {
    setZonedDateTime(headerName, ZonedDateTime.ofInstant(date, GMT));
  }

  /**
   * Set the given date under the given header name after formatting it as a
   * string using the RFC-1123 date-time formatter. The equivalent of
   * {@link #set(String, String)} but for date headers.
   *
   * @see #setZonedDateTime(String, ZonedDateTime)
   */
  public void setDate(String headerName, long date) {
    setInstant(headerName, Instant.ofEpochMilli(date));
  }

  /**
   * Parse the first header value for the given header name as a date, return -1
   * if there is no value, or raise {@link IllegalArgumentException} if the value
   * cannot be parsed as a date.
   *
   * @param headerName
   *         the header name
   *
   * @return the parsed date header, or -1 if none
   *
   * @see #getFirstZonedDateTime(String)
   */
  public long getFirstDate(String headerName) {
    return getFirstDate(headerName, true);
  }

  /**
   * Parse the first header value for the given header name as a date, return -1
   * if there is no value or also in case of an invalid value (if
   * {@code rejectInvalid=false}), or raise {@link IllegalArgumentException} if
   * the value cannot be parsed as a date.
   *
   * @param headerName
   *         the header name
   * @param rejectInvalid
   *         whether to reject invalid values with an
   *         {@link IllegalArgumentException} ({@code true}) or rather return
   *         -1 in that case ({@code false})
   *
   * @return the parsed date header, or -1 if none (or invalid)
   *
   * @see #getFirstZonedDateTime(String, boolean)
   */
  public long getFirstDate(String headerName, boolean rejectInvalid) {
    ZonedDateTime zonedDateTime = getFirstZonedDateTime(headerName, rejectInvalid);
    return (zonedDateTime != null ? zonedDateTime.toInstant().toEpochMilli() : -1);
  }

  /**
   * Parse the first header value for the given header name as a date, return
   * {@code null} if there is no value, or raise {@link IllegalArgumentException}
   * if the value cannot be parsed as a date.
   *
   * @param headerName
   *         the header name
   *
   * @return the parsed date header, or {@code null} if none
   */
  public ZonedDateTime getFirstZonedDateTime(String headerName) {
    return getFirstZonedDateTime(headerName, true);
  }

  /**
   * Parse the first header value for the given header name as a date, return
   * {@code null} if there is no value or also in case of an invalid value (if
   * {@code rejectInvalid=false}), or raise {@link IllegalArgumentException} if
   * the value cannot be parsed as a date.
   *
   * @param headerName
   *         the header name
   * @param rejectInvalid
   *         whether to reject invalid values with an
   *         {@link IllegalArgumentException} ({@code true}) or rather return
   *         {@code null} in that case ({@code false})
   *
   * @return the parsed date header, or {@code null} if none (or invalid)
   */
  public ZonedDateTime getFirstZonedDateTime(String headerName, boolean rejectInvalid) {
    String headerValue = getFirst(headerName);
    if (headerValue == null) {
      // No header value sent at all
      return null;
    }
    if (headerValue.length() >= 3) {
      // Short "0" or "-1" like values are never valid HTTP date headers...
      // Let's only bother with DateTimeFormatter parsing for long enough values.

      // See https://stackoverflow.com/questions/12626699/if-modified-since-http-header-passed-by-ie9-includes-length
      int parametersIndex = headerValue.indexOf(';');
      if (parametersIndex != -1) {
        headerValue = headerValue.substring(0, parametersIndex);
      }

      for (DateTimeFormatter dateFormatter : DATE_PARSERS) {
        try {
          return ZonedDateTime.parse(headerValue, dateFormatter);
        }
        catch (DateTimeParseException ex) {
          // ignore
        }
      }
    }
    if (rejectInvalid) {
      throw new IllegalArgumentException(
              "Cannot parse date value \"" + headerValue + "\" for \"" + headerName + "\" header");
    }
    return null;
  }

  /**
   * Return all values of a given header name, even if this header is set multiple
   * times.
   *
   * @param headerName
   *         the header name
   *
   * @return all associated values
   */
  public List<String> getValuesAsList(String headerName) {
    List<String> values = get(headerName);
    if (values != null) {
      ArrayList<String> result = new ArrayList<>();
      for (String value : values) {
        if (value != null) {
          Collections.addAll(result, StringUtils.tokenizeToStringArray(value, ","));
        }
      }
      return result;
    }
    return Collections.emptyList();
  }

  /**
   * Remove the well-known {@code "Content-*"} HTTP headers.
   * <p>
   * Such headers should be cleared from the response if the intended body can't
   * be written due to errors.
   */
  public void clearContentHeaders() {
    remove(CONTENT_TYPE);
    remove(CONTENT_RANGE);
    remove(CONTENT_LENGTH);
    remove(CONTENT_LANGUAGE);
    remove(CONTENT_LOCATION);
    remove(CONTENT_ENCODING);
    remove(CONTENT_DISPOSITION);
  }

  /**
   * Retrieve a combined result from the field values of the ETag header.
   *
   * @param headerName
   *         the header name
   *
   * @return the combined result
   */
  public List<String> getETagValuesAsList(String headerName) {
    List<String> values = get(headerName);
    if (values != null) {
      ArrayList<String> result = new ArrayList<>();
      for (String value : values) {
        if (value != null) {
          Matcher matcher = ETAG_HEADER_VALUE_PATTERN.matcher(value);
          while (matcher.find()) {
            if ("*" .equals(matcher.group())) {
              result.add(matcher.group());
            }
            else {
              result.add(matcher.group(1));
            }
          }
          if (result.isEmpty()) {
            throw new IllegalArgumentException("Could not parse header '" + headerName + "' with value '" + value + "'");
          }
        }
      }
      return result;
    }
    return Collections.emptyList();
  }

  /**
   * Retrieve a combined result from the field values of multi-valued headers.
   *
   * @param headerName
   *         the header name
   *
   * @return the combined result
   */
  public String getFieldValues(String headerName) {
    List<String> headerValues = get(headerName);
    return (headerValues != null ? collectionToString(headerValues) : null);
  }

  protected String collectionToString(Collection<?> headerValues) {
    return StringUtils.collectionToString(headerValues, ", ");
  }

  /**
   * Set the given header value, or remove the header if {@code null}.
   *
   * @param headerName
   *         the header name
   * @param headerValue
   *         the header value, or {@code null} for none
   */
  public void setOrRemove(String headerName, String headerValue) {
    if (headerValue != null) {
      set(headerName, headerValue);
    }
    else {
      remove(headerName);
    }
  }

  // abstract for subclasses

  /**
   * Return the first header value for the given header name, if any.
   *
   * @param headerName
   *         the header name
   *
   * @return the first header value, or {@code null} if none
   */
  @Override
  public abstract String getFirst(String headerName);

  /**
   * Add the given, single header value under the given name.
   *
   * @param headerName
   *         the header name
   * @param headerValue
   *         the header value
   *
   * @throws UnsupportedOperationException
   *         if adding headers is not supported
   * @see #addAll(String, List)
   * @see #set(String, String)
   */
  @Override
  public abstract void add(String headerName, String headerValue);

  @Override
  public void addAll(String key, List<? extends String> values) {
    for (final String value : values) {
      add(key, value);
    }
  }

  @Override
  public void addAll(MultiValueMap<String, String> values) {
    values.forEach(this::addAll);
  }

  @Override
  public abstract void set(String headerName, String headerValue);

  @Override
  public void setAll(Map<String, String> values) {
    values.forEach(this::set);
  }

  @Override
  public abstract List<String> get(Object headerName);

  @Override
  public abstract List<String> remove(Object headerName);

  @Override
  public String toString() {
    return formatHeaders(this);
  }

  // static

  /**
   * Helps to format HTTP header values, as HTTP header values themselves can
   * contain comma-separated values, can become confusing with regular
   * {@link Map} formatting that also uses commas between entries.
   *
   * @param headers
   *         the headers to format
   *
   * @return the headers to a String
   */
  public static String formatHeaders(MultiValueMap<String, String> headers) {
    return headers.entrySet().stream()
            .map(entry -> {
              List<String> values = entry.getValue();
              return entry.getKey() + ":" + (values.size() == 1 ?
                                             "\"" + values.get(0) + "\"" :
                                             values.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")));
            })
            .collect(Collectors.joining(", ", "[", "]"));
  }

  /**
   * Encode the given username and password into Basic Authentication credentials.
   * <p>The encoded credentials returned by this method can be supplied to
   * {@link #setBasicAuth(String)} to set the Basic Authentication header.
   *
   * @param username
   *         the username
   * @param password
   *         the password
   * @param charset
   *         the charset to use to convert the credentials into an octet
   *         sequence. Defaults to {@linkplain StandardCharsets#ISO_8859_1 ISO-8859-1}.
   *
   * @throws IllegalArgumentException
   *         if {@code username} or {@code password}
   *         contains characters that cannot be encoded to the given charset
   * @see #setBasicAuth(String)
   * @see #setBasicAuth(String, String)
   * @see #setBasicAuth(String, String, Charset)
   * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
   */
  public static String encodeBasicAuth(String username, String password, Charset charset) {
    Assert.notNull(username, "Username must not be null");
    Assert.doesNotContain(username, ":", "Username must not contain a colon");
    Assert.notNull(password, "Password must not be null");
    if (charset == null) {
      charset = StandardCharsets.ISO_8859_1;
    }

    CharsetEncoder encoder = charset.newEncoder();
    if (!encoder.canEncode(username) || !encoder.canEncode(password)) {
      throw new IllegalArgumentException(
              "Username or password contains characters that cannot be encoded to " + charset.displayName());
    }

    String credentialsString = username + ":" + password;
    byte[] encodedBytes = Base64.getEncoder().encode(credentialsString.getBytes(charset));
    return new String(encodedBytes, charset);
  }

  public static DefaultHttpHeaders of() {
    return new DefaultHttpHeaders();
  }

}
