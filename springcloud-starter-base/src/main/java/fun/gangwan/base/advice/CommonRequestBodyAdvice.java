package fun.gangwan.base.advice;

import com.fc.base.tools.JacksonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * <br> CommonRequestBodyAdvice </br>
 * <br> 该类前置处理请求入参 </br>
 *
 */
@Slf4j
//@RestControllerAdvice
public class CommonRequestBodyAdvice implements RequestBodyAdvice {

    @PostConstruct
    public void init(){
        log.info("###### Request body log will been print ######");
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        return inputMessage;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        //通过反射拿到mapping
        Method method = parameter.getMethod();
        String paramType = parameter.getGenericParameterType().getTypeName();
        String mapped = parameter.getDeclaringClass().getName() + "#" + method.getName() + "(" + paramType + ")";

        //获取body内容
        String jsonBody;
        if (StringHttpMessageConverter.class.isAssignableFrom(converterType)) {
            jsonBody = body.toString();
        } else {
            jsonBody = JacksonUtils.obj2String(body);
//            jsonBody = LogFormatUtils.formatValue(body, false);
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(attributes == null){
            log.warn("RequestContextHolder cannot get RequestAttributes");
            return body;
        }
        HttpServletRequest request = attributes.getRequest();

        //打印日志
        log.info("请求映射 -> {}[{}] mapped to [{}], ContentType -> {}",
                request.getMethod(), request.getRequestURI(), mapped, request.getContentType()
        );
        log.info("请求参数 -> {}", jsonBody);
        return body;
    }

    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }
}
