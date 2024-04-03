package fun.gangwan.base.facade.extend.orika;

import ma.glasnost.orika.MapperFactory;

/**
 * 自定义Mapper组件配置
 *
 * @author ZhouYi
 * @since 2021/08/06
 * @version 1.0.0
 */
public interface IMapperConfigure {

    /**
     * 配置特殊的转换
     * @param factory
     */
    void configure(MapperFactory factory);
}
