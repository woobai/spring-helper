package fun.gangwan.base.advice;


import feign.RetryableException;
import fun.gangwan.base.constant.BootConfPrefix;
import fun.gangwan.base.exception.CommonException;
import fun.gangwan.base.exception.InvalidApiRequestException;
import fun.gangwan.base.exception.ServiceException;
import fun.gangwan.base.facade.enums.BizEnum;
import fun.gangwan.base.facade.enums.BizErrorCodeEnum;
import fun.gangwan.base.facade.response.BaseResponse;
import fun.gangwan.base.facade.response.BaseResponseBuilder;
import fun.gangwan.base.facade.response.StringResponse;
import fun.gangwan.base.tools.JacksonUtils;
import fun.gangwan.base.wrapper.CurrentRequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.net.SocketTimeoutException;


/**
 * <br> CommonWebAdvice </br>
 * <br> API 通用控制器异常通知处理 </br>
 */
@Slf4j
@RestControllerAdvice
@ConditionalOnProperty(prefix = BootConfPrefix.ADVICE, name = "enable", havingValue = "true", matchIfMissing = true)
public class CommonWebAdvice {

    @Resource
    WebAdviceProperties webAdviceProperties;

    @PostConstruct
    public void init() {
        log.info("###### CommonWebAdvice init success ######");
    }

    /**
     * <li>由用户的异常操作触发，客户端修正参数后再提交即可恢复</li>
     * <li>需要明确提示用户出现了什么</li>
     *
     * @param request
     * @param serviceException
     * @return 200
     * @throws Exception
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<BaseResponse<String>> serviceExceptionHandler(HttpServletRequest request, ServiceException serviceException) {
        String body = CurrentRequestContext.getCurrentParamsJson();
        String requestUrl = request.getServletPath();

        String msg = String.format("ServiceException >>> url: %s, code: %s, msg: %s, params: %s",
                requestUrl, serviceException.getCode(), serviceException.getMessage(), body);

        log.error(msg, webAdviceProperties.isServiceExLogWithStack() ? serviceException : null);

        // 处理返回结果
        BaseResponse<String> baseResponse = createExceptionResponse(serviceException.getCode(), serviceException);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(baseResponse);
    }


    /**
     * 代码造成的逻辑漏洞，或者无法通过重试来纠正的错误异常
     *
     * @param request
     * @param commonException
     * @return 500
     */
    @ExceptionHandler(CommonException.class)
    public ResponseEntity<BaseResponse<String>> commonExceptionHandler(HttpServletRequest request, CommonException commonException) {
        String body = CurrentRequestContext.getCurrentParamsJson();
        String requestUrl = request.getServletPath();

        String msg = String.format("CommonException >>> url: %s, msg: %s, params: %s",
                requestUrl, commonException.getMessage(), body);
        log.error(msg, commonException);

        BaseResponse<String> errorInfo = createExceptionResponse(BizErrorCodeEnum.SYSTEM_ERROR, commonException);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorInfo);
    }

    /**
     * 客户端自定义认证错误处理器<br>
     * 处理客户端非法请求
     *
     * @param request
     * @param ie
     * @return 401
     */
    @ExceptionHandler(InvalidApiRequestException.class)
    public ResponseEntity<BaseResponse<String>> invalidApiRequestHandler(HttpServletRequest request, InvalidApiRequestException ie) {

        // 发送预警通知
        String msg = String.format("非法请求[%s] >>> %s", request.getServletPath(), ie.getMessage());
        log.error(msg);

        BaseResponse<String> errorInfo = createExceptionResponse(BizErrorCodeEnum.AUTH_FAILED, ie);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorInfo);
    }

    /**
     * 全局其他异常处理
     *
     * @param ex
     * @param request
     * @return 200
     */
    @ExceptionHandler
    public ResponseEntity<BaseResponse<String>> globalExHandler(Exception ex, HttpServletRequest request) {
        // 处理参数
        String body = CurrentRequestContext.getCurrentParamsJson();
        String requestUrl = request.getServletPath();

        String msg = String.format("Exception >>> url: %s, msg: %s, params: %s", requestUrl, ex.getMessage(), body);
        log.error(msg, ex);

        BaseResponse<String> errorInfo = webAdviceProperties.isShowGlobalExMsg()
                ? createExceptionResponse(BizErrorCodeEnum.SYSTEM_ERROR, ex)
                : createExceptionResponse(BizErrorCodeEnum.SYSTEM_ERROR.getCode(), ex);

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorInfo);
    }

    /**
     * 处理调用接口超时异常
     *
     * @param ste
     * @param request
     * @return 200
     */
    @ExceptionHandler({SocketTimeoutException.class, RetryableException.class})
    public ResponseEntity<BaseResponse<String>> timeoutHandler(Exception ste, HttpServletRequest request) {
        String requestUrl = request.getServletPath();

        String msg = String.format("请求内部调用超时 >>> url: %s, msg: %s", requestUrl, ste.getMessage());
        log.error(msg, ste);

        BaseResponse<String> errorInfo = createExceptionResponse(BizErrorCodeEnum.CALL_SERVICE_ERROR, ste);

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorInfo);
    }

    /**
     * 处理缺失必填参数的异常
     *
     * @param ex
     * @param request
     * @return 200
     */
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<BaseResponse<String>> missParamErrorHandler(Exception ex, HttpServletRequest request) {
        String body = CurrentRequestContext.getCurrentParamsJson();
        String requestUrl = request.getServletPath();

        String msg = String.format("必要参数缺失 >>> url: %s, msg: %s, params: %s", requestUrl, ex.getMessage(), body);
        if (webAdviceProperties.isArgumentMissPrintErrorLog()) {
            log.error(msg);
        } else {
            log.warn(msg);
        }

        BaseResponse<String> errorInfo = createExceptionResponse(BizErrorCodeEnum.PARAM_IS_NULL, ex);

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorInfo);
    }

    /**
     * 参数校验异常
     *
     * @param be
     * @param request
     * @return 200
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<BaseResponse<String>> argumentNotValidHandler(BindException be, HttpServletRequest request) {
        FieldError fieldError = be.getBindingResult().getFieldError();

        return buildNotValidResponse(request, fieldError);
    }

    /**
     * 参数校验异常
     *
     * @param av
     * @param request
     * @return 200
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<String>> argumentNotValidHandler(MethodArgumentNotValidException av, HttpServletRequest request) {
        FieldError fieldError = av.getBindingResult().getFieldErrors().get(0);

        return buildNotValidResponse(request, fieldError);
    }

    /**
     * 非法参数
     *
     * @param ex
     * @param request
     * @return 200
     */
    @ExceptionHandler(HttpMessageConversionException.class)
    public ResponseEntity<BaseResponse<String>> httpMessageConversionHandler(HttpMessageConversionException ex, HttpServletRequest request) {
        String body = CurrentRequestContext.getCurrentParamsJson();
        String requestUrl = request.getServletPath();

        String msg = String.format("非法参数 >>> url: %s, msg: %s, params: %s", requestUrl, ex.getMessage(), body);
        log.error(msg, ex);

        BaseResponse<String> errorInfo = createExceptionResponse(BizErrorCodeEnum.INVALID_PARAMS, ex);

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorInfo);
    }

    /**
     * 文件上传
     *
     * @param ex
     * @param request
     * @return 200
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<BaseResponse<String>> fileUploadHandler(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        String requestUrl = request.getServletPath();

        String msg = String.format("文件上传异常 >>> url: %s, msg: %s", requestUrl, ex.getMessage());
        log.error(msg, ex);

        BaseResponse<String> errorInfo = createExceptionResponse(BizErrorCodeEnum.FILE_SIZE_LIMIT, ex);

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorInfo);
    }

    /**
     * @param request
     * @param fieldError
     * @return 200
     */
    private ResponseEntity<BaseResponse<String>> buildNotValidResponse(HttpServletRequest request, FieldError fieldError) {
        String body = CurrentRequestContext.getCurrentParamsJson();
        String requestUrl = request.getServletPath();

        String errorMsg = fieldError.getDefaultMessage();
        String errorDetail = fieldError.getField() + errorMsg;
        String msg = String.format("参数校验失败 >>> url: %s, msg: %s, params: %s", requestUrl, errorDetail, body);
        if (webAdviceProperties.isArgumentValidPrintErrorLog()) {
            log.error(msg);
        } else {
            log.warn(msg);
        }

        BaseResponse<String> errorInfo = createExceptionResponse(BizErrorCodeEnum.PARAM_ERROR.getCode(), errorMsg, new CommonException(errorDetail));

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorInfo);
    }

    /**
     * 404 Not Found 异常处理<br>
     * 为了避免探测和记录 404 异常，使用这个 Handler 处理
     *
     * @return
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<StringResponse> notFoundHandler() {

        StringResponse notfound = new StringResponse();
        notfound.setCode(HttpStatus.NOT_FOUND.value());
        notfound.setMessage(HttpStatus.NOT_FOUND.getReasonPhrase());
        notfound.setDetail(HttpStatus.NOT_FOUND.getReasonPhrase());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(notfound);
    }

    /**
     * 创建异常返回值
     *
     * @param exceptionCode
     * @param msg
     * @param exception
     * @return
     */
    private BaseResponse<String> createExceptionResponse(int exceptionCode, String msg, Exception exception) {
        // 构建返回结果
        BaseResponse<String> result = BaseResponseBuilder.failed(
                exceptionCode, msg,
                webAdviceProperties.isShowDetail() ? "[" + exceptionCode + "]" + exception.getMessage() : null,
                webAdviceProperties.isShowTrace()
        );
        log.info("返回异常结果 -> {}", JacksonUtils.obj2String(result));
        return result;
    }

    /**
     * 创建异常返回值
     *
     * @param exceptionCode
     * @param exception
     * @return
     */
    private BaseResponse<String> createExceptionResponse(int exceptionCode, Exception exception) {
        return createExceptionResponse(exceptionCode, exception.getMessage(), exception);
    }

    /**
     * 创建异常返回值
     *
     * @param bizEnum
     * @param exception
     * @return
     */
    private BaseResponse<String> createExceptionResponse(BizEnum bizEnum, Exception exception) {
        return createExceptionResponse(bizEnum.getCode(), bizEnum.getDesc(), exception);
    }

}
