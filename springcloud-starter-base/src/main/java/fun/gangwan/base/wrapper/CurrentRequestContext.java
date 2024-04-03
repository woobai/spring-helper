package fun.gangwan.base.wrapper;

import com.google.common.collect.Maps;
import fun.gangwan.base.facade.constant.MDCConstant;
import fun.gangwan.base.metadata.RequestMetadata;
import fun.gangwan.base.tools.HttpRequestUtils;
import fun.gangwan.base.tools.JacksonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;

/**
 * <br>CurrentRequestContext </br>
 * <span>当前请求上下文</span>
 *
 */
@Slf4j
public class CurrentRequestContext implements Cloneable{

    static {
        // 注册清理线程
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                CurrentRequestContext.init();
            }
        });
    }

    /**
     * 初始化
     */
    public static void init() {
        MDC.clear();
    }

    /**
     * 绑定到上下文
     * @param k
     * @param v
     */
    private static void bind(String k, String v) {
        if(StringUtils.isAnyBlank(k, v)){
            log.debug("CurrentRequestContext bind error -> k:{}, v:{}", k, v);
            return;
        }
        MDC.put(k, v);
    }

    /**
     * 绑定到上下文
     *
     * @param k
     * @param v
     */
    public static void bind(String k, Object v){
        bind(k, JacksonUtils.obj2String(v));
    }

    /**
     * 获取上下文绑定值
     * @param k
     * @return
     */
    public static String pick(String k){
        if(StringUtils.isBlank(k)){
            return null;
        }
        return MDC.get(k);
    }

    /**
     * 生成RequestId
     *
     * @return RequestId
     */
    public static String generateRequestId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 绑定当前上下文信息到MDC
     *
     * @param quirksMode
     */
    public static void bindRequestMetadata(HttpServletRequest request, final RequestMetadata requestMetadata, boolean quirksMode) {
        String params = buildCurrentParams(request);
        String headers = buildCurrentHeaders(request);

        String mdcRequestId = pick(MDCConstant.REQUEST_ID);
        if(!StringUtils.equals(requestMetadata.getRequestId(), mdcRequestId)){
            //如果之前有拦截器或者过滤器绑定过REQUEST_ID，需要判定请求header和当前线程的REQUEST_ID是否一致
            bind(MDCConstant.REQUEST_ID, requestMetadata.getRequestId());
        }

        //请求参数
        bind(MDCConstant.REQUEST_PARAMS, params);
        //请求报头
        bind(MDCConstant.REQUEST_HEADERS, headers);
        //请求元数据
        bind(MDCConstant.METADATA_REQUEST, requestMetadata);

    }

    private static String buildCurrentParams(HttpServletRequest request){
        if(HttpRequestUtils.isJsonRequest(request)){
            String bodyStr = new RequestContextWrapper(request).getBodyString();
            return JacksonUtils.obj2String(bodyStr);
        }else{
            return buildRequestParams(request.getParameterMap());
        }
    }

    private static String buildCurrentHeaders(HttpServletRequest request){
        Enumeration<String> headerNames = request.getHeaderNames();
        Map<String, String> headersMap = Maps.newHashMap();
        String name;
        if (headerNames != null) {
            while(headerNames.hasMoreElements()) {
                name = headerNames.nextElement();
                String value = request.getHeader(name);
                headersMap.put(name.toLowerCase(), value);
            }
        }
        return JacksonUtils.obj2String(headersMap);
    }



    /**
     * 获取当前请求参数json格式串
     *
     * @return 获取当前请求参数json格式串
     */
    public static String getCurrentParamsJson(){
        String params = pick(MDCConstant.REQUEST_PARAMS);
        if(StringUtils.isNotBlank(params)){
            return params;
        }
        return StringUtils.EMPTY;
    }

    /**
     *
     * 获取当前请求报头json格式串
     *
     * @return 获取当前请求报头json格式串
     */
    public static String getCurrentHeadersJson() {
        String headers = pick(MDCConstant.REQUEST_HEADERS);
        if(StringUtils.isNotBlank(headers)){
            return headers;
        }
        return StringUtils.EMPTY;
    }

    /**
     * 获取请求参数集合
     *
     * @param parameterMap
     * @return
     */
    private static String buildRequestParams(Map<String, String[]> parameterMap) {
        StringBuilder builder = new StringBuilder(256);

        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            builder.append(entry.getKey()).append("=").append(StringUtils.join(entry.getValue())).append(",");
        }
        return StringUtils.removeEnd(builder.toString(), ",");
    }

    /**
     * 获取当前请求上下文的链路标识
     * @return
     */
    public static String getRequestId(){
        return pick(MDCConstant.REQUEST_ID);
    }

    /**
     * 获取当前请求URI
     * @return
     */
    public static String getCurrentUri(){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(attributes == null){
            return null;
        }
        return attributes.getRequest().getRequestURI();
    }

}
