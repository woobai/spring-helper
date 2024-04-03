package fun.gangwan.base.facade.response;

import com.github.pagehelper.PageInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * API通用返回结构 PageInfoResponse
 *
 * @author ZhouYi
 * @since 2021/08/06
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PageInfoResponse<T> extends BaseResponse<PageInfo<T>> {

}

