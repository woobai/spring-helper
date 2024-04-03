package fun.gangwan.base.facade.extend.orika.convertor;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;

import java.math.BigDecimal;

/**
 * BigDecimal转换为Integer的方法
 *
 * @author ZhouYi
 * @since 2021/08/06
 * @version 1.0.0
 */
public class BigDecimalToIntegerConverter extends BidirectionalConverter<BigDecimal, Integer> {

    @Override
    public Integer convertTo(BigDecimal source, Type<Integer> destinationType, MappingContext mappingContext) {
        return source.intValue();
    }

    @Override
    public BigDecimal convertFrom(Integer source, Type<BigDecimal> destinationType, MappingContext mappingContext) {
        return BigDecimal.valueOf(source);
    }
}
