package fun.gangwan.base.facade.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

/**
 * API通用返回结构-StringResponse
 *
 * @author ZhouYi
 * @since 2021/08/06
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StringResponse extends BaseResponse<String> implements Serializable {
}