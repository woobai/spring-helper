package fun.gangwan.base.advice;

import com.fc.base.constant.BootConfPrefix;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * WebAdviceProperties
 * <span>异常处理配置</span>
 *
 */
@Getter
@Setter
@ConfigurationProperties(prefix = BootConfPrefix.ADVICE)
public class WebAdviceProperties {

    /**
     * 异常信息定义
     */
    private Map<Integer, String> codeMsg = Maps.newHashMap();

    /**
     *
     * 是否强制开启全局异常的msg返回值: SYSTEM_ERROR(10025, "SYSTEM_ERROR", "系统异常")
     * 默认false不开启
     *
     */
    private boolean showGlobalExMsg = false;

    /**
     *
     * 是否输出异常类code和message，默认true
     *
     */
    private boolean showDetail = true;

    /**
     *
     * 是否输出链路信息requestId，默认true
     *
     */
    private boolean showTrace = true;

    /**
     *
     * 抛出ServiceException时是否输出异常堆栈信息
     * 默认false不输出堆栈, 注意：2.3.0版本以前的脚手架默认输出
     *
     */
    private boolean serviceExLogWithStack = false;

    /**
     *
     * 参数缺失是否打印error级别日志
     *
     * 当Spring框架绑定参数时抛出的MissingServletRequestParameterException和MethodArgumentTypeMismatchException异常时是否输出error级别
     * 默认true输出error级别, 设置false输出warn级别日志
     *
     */
    private boolean argumentMissPrintErrorLog = true;

    /**
     *
     * 参数验证失败是否打印error级别日志
     *
     * 当Spring框架绑定参数或者@Valid校验参数的情况下抛出的MethodArgumentNotValidException和BindException异常时是否输出error级别
     * 默认true输出error级别, 设置false输出warn级别日志
     *
     */
    private boolean argumentValidPrintErrorLog = true;


}
