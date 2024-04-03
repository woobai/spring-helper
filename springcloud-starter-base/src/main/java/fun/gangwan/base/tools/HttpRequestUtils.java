package fun.gangwan.base.tools;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Optional;

/**
 * <br>HttpRequestUtils </br>
 * <span>HttpRequest工具类</span>
 *
 */
@SuppressWarnings("unused")
@Slf4j
public class HttpRequestUtils {

    /**
     * 判断请求是否json格式
     *
     * @param request
     * @return
     */
    public static boolean isJsonRequest(HttpServletRequest request) {
        if (StringUtils.isBlank(request.getContentType())) {
            return false;
        }
        return StringUtils.containsIgnoreCase(request.getContentType(), MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * 获取当前请求的token
     *
     * @return 请求头的值
     */
    public static String getCurrentHttpRequestAuthorization() {
        return getCurrentHttpRequestHeader(HttpHeaders.AUTHORIZATION);
    }

    /**
     * 获取当前请求头的第一个值
     *
     * @param header 请求头
     * @return 请求头的值
     */
    public static String getCurrentHttpRequestHeader(String header) {
        return Optional.ofNullable(getCurrentHttpServletRequest())
                .map(request -> request.getHeaders(header))
                .filter(Enumeration::hasMoreElements)
                .map(Enumeration::nextElement)
                .orElse(null);
    }

    /**
     * 获取当前线程绑定的HttpServletRequest
     *
     * @return HttpServletRequest
     */
    @Nullable
    public static HttpServletRequest getCurrentHttpServletRequest() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .orElse(null);
    }
}
