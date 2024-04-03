package fun.gangwan.base.facade.extend.orika.convertor;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;

import java.util.Date;

/**
 * 整型转换为时间的方法
 *
 * @author ZhouYi
 * @since 2021/08/06
 * @version 1.0.0
 */
public class IntegerToDateConverter extends BidirectionalConverter<Integer, Date> {
    @Override
    public Date convertTo(Integer source, Type<Date> destinationType, MappingContext mappingContext) {
        return new Date(source * 1000L);
    }

    @Override
    public Integer convertFrom(Date source, Type<Integer> destinationType, MappingContext mappingContext) {
        return (int)(source.getTime() / 1000);
    }
}
