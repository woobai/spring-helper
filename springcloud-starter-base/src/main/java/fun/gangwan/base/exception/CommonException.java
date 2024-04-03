package fun.gangwan.base.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

/**
 *
 * <p>系统异常定义</p>
 *
 * <p>由代码不严谨造成的逻辑漏洞，或者其他什么原因导致的无法由客户端重新提交来纠正的错误异常</p>
 *
 * <p>向客户端返回http code为500</p>
 *
 * @author ZhouYi
 * @since 2021/08/09
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CommonException extends RuntimeException implements Serializable {

    /**
     * 创建CommonException
     */
    public CommonException() {
        super();
    }

    /**
     * 创建CommonException
     *
     * @param detail 错误信息描述
     */
    public CommonException(String detail) {
        super(detail);
    }

    /**
     * 创建CommonException
     *
     * @param detail 错误信息描述
     * @param args 替换描述信息中的变量
     */
    public CommonException(String detail, Object... args) {
        super(String.format(detail, args));
    }

    /**
     * 创建CommonException
     *
     * @param detail 错误信息描述
     * @param ex 异常信息
     */
    public CommonException(Throwable ex, String detail) {
        super(detail, ex);
    }

    /**
     * 创建CommonException
     * @param ex
     * @param detail
     * @param args
     */
    public CommonException(Throwable ex, String detail, Object... args) {
        super(String.format(detail, args), ex);
    }

}
