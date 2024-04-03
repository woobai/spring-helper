package fun.gangwan.base.facade.response;

import fun.gangwan.base.facade.enums.BizErrorCodeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;


/**
 * API通用返回结构 BizBaseResponse
 *
 */
@ApiModel(value = "通用响应数据结构")
public class BaseResponse<T> implements Serializable {

    @ApiModelProperty(value = "响应编码")
    private int code;

    @ApiModelProperty(value = "提示消息，用于向用户展示")
    private String message;

    @ApiModelProperty(value = "数据")
    private T data;

    @ApiModelProperty(value = "detail信息，用于输出异常信息")
    private String detail;

    @ApiModelProperty(value = "debug信息，用于异常追踪")
    private String trace;

    /**
     *
     * @return 判断是否成功消息
     */
    public boolean isSuccess(){
        return this.code == BizErrorCodeEnum.SUCCESS.getCode();
    }

    public BaseResponse() {
    }

    public BaseResponse(int code, String message, T data, String detail, String trace) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.detail = detail;
        this.trace = trace;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getTrace() {
        return trace;
    }

    public void setTrace(String trace) {
        this.trace = trace;
    }
}
