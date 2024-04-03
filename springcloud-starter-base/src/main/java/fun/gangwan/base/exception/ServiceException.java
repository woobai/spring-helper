package fun.gangwan.base.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

/**
 * <p>业务异常定义</p>
 *
 * <p>两种情况下使用此异常：</p>
 * <ul>
 *     <li>由用户的异常操作触发，客户端修正参数后再提交即可恢复</li>
 *     <li>需要明确提示用户出现了什么</li>
 * </ul>
 *
 * <p>向客户端返回http code为200</p>
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ServiceException extends RuntimeException implements Serializable {

    /**
     * 错误码
     */
    private int code;

    /**
     * 错误信息，用于给用户查看的信息（默认中文）
     */
    private String msg;

    /**
     * 错误的补充信息，用于调试错误
     */
    private String detail;

    /**
     * 创建ServiceException
     */
    public ServiceException() {
        super();
    }

    /**
     * 创建ServiceException
     *
     * @param code 错误码
     * @param msg 错误信息，用于给用户查看的信息（默认中文）
     */
    public ServiceException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    /**
     * 创建ServiceException
     *
     * @param code 错误码
     * @param msg 错误信息，用于给用户查看的信息（默认中文）
     * @param detail 错误的补充信息，用于调试错误
     */
    public ServiceException(int code, String msg, String detail) {
        super(msg);
        this.code = code;
        this.msg = msg;
        this.detail = detail;
    }

    /**
     * 创建ServiceException
     *
     * @param code 错误码
     * @param msg 错误信息，用于给用户查看的信息（默认中文）
     * @param cause 关联的异常信息
     */
    public ServiceException(int code, String msg, Throwable cause) {
        super(msg, cause);
        this.code = code;
        this.msg = msg;
    }

    /**
     * 创建ServiceException
     *
     * @param code 错误码
     * @param msg 错误信息，用于给用户查看的信息（默认中文）
     * @param detail 错误的补充信息，用于调试错误
     * @param cause 关联的异常信息
     */
    public ServiceException(int code, String msg, String detail, Throwable cause) {
        super(msg, cause);
        this.code = code;
        this.msg = msg;
        this.detail = detail;
    }

}
