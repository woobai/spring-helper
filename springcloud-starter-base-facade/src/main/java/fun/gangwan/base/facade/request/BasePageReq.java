package fun.gangwan.base.facade.request;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * @author : bryan
 * @date : 2024/4/1 15:45
 */
@Data
@ApiModel(value = "通用分页查询数据结构")
public class BasePageReq {

    private Integer current = 1;

    private Integer size = 10;

}
