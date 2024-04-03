package fun.gangwan.base.facade.enums;

import java.io.Serializable;

/**
 * 业务枚举基类
 * 
 */
public interface BizEnum extends Serializable {

    public int getCode();

    public String getName();

    public String getDesc();

}
