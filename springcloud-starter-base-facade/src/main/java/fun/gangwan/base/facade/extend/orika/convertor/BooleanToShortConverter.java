package fun.gangwan.base.facade.extend.orika.convertor;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;

/**
 * Boolean转换为Short的方法
 *
 * @author ZhouYi
 * @since 2021/08/06
 * @version 1.0.0
 */
public class BooleanToShortConverter extends BidirectionalConverter<Boolean, Short> {

    @Override
    public Short convertTo(Boolean source, Type<Short> destinationType, MappingContext mappingContext) {
        return (source != null && source) ? (short)1 : (short)0;
    }

    @Override
    public Boolean convertFrom(Short source, Type<Boolean> destinationType, MappingContext mappingContext) {
        if(source > 0) {
            return true;
        } else if (source == 0){
            return false;
        } else {
            throw new IllegalArgumentException(String.format("无法转换为Boolean类型[1: true, 0: false]: %s", source));
        }
    }
}