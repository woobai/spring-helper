package fun.gangwan.base.facade.response;

import lombok.Data;

import java.util.List;

/**
 * API通用返回结构 ListBean
 *
 * @author ZhouYi
 * @since 2021/08/06
 * @version 1.0.0
 */
@Data
public class ListBean<T> {

    /**
     * 列表型返回结果
     */
    private List<T> list;
}
