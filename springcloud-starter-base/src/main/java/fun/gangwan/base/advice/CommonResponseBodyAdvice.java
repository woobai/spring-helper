package fun.gangwan.base.advice;

import com.fc.base.constant.BootConfPrefix;
import com.fc.base.tools.JacksonUtils;
import com.fc.base.tools.PathMatcherUtils;
import com.fc.base.wrapper.CurrentRequestContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.MethodParameter;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.List;

/**
 * <br> CommonResponseBodyAdvice </br>
 * <br> 数据回写后触发 </br>
 *
 */
@Slf4j(topic = LoggerTopic.HTTP_TRACE)
@RestControllerAdvice
@ConditionalOnProperty(prefix = BootConfPrefix.LOG, name = "enable", havingValue = "true", matchIfMissing = true)
public class CommonResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    /**
     * 排除路径，默认值为actuator端点和swagger ui相关路径
     */
    @Value("#{'${fc.boot.mvc.log.excludePatterns:/,/error,/actuator/**}'.split(',')}")
    private List<String> excludePatterns;

    @Value("${fc.boot.mvc.log.response.max-length:3072}")
    private int logMaxLength;

    @PostConstruct
    public void init() {
        log.info("###### Response body log will been print without patterns {} ######", excludePatterns);
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
//      return AbstractJackson2HttpMessageConverter.class.isAssignableFrom(converterType)
        String uri = CurrentRequestContext.getCurrentUri();
        boolean isMatched = PathMatcherUtils.isAnyMatch(uri, excludePatterns);
        if(isMatched){
            return false;
        }

        Method method = returnType.getMethod();
        if(method == null){
            log.warn("method is null");
            return false;
        }
        return method.isAnnotationPresent(RequestMapping.class)
                || method.isAnnotationPresent(GetMapping.class)
                || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(PatchMapping.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        final String uri = request.getURI().getPath();
        final String method = request.getMethodValue();
        String result = JacksonUtils.obj2String(body);
        log.info("返回结果 [{}]{} -> {}", method, uri, LogFormatUtils.formatValue(result, logMaxLength, false));
        return body;
    }

}
