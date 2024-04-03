package fun.gangwan.base.facade.extend.orika;

import fun.gangwan.base.facade.extend.orika.convertor.BigDecimalToIntegerConverter;
import fun.gangwan.base.facade.extend.orika.convertor.BooleanToShortConverter;
import fun.gangwan.base.facade.extend.orika.convertor.IntegerToDateConverter;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.converter.ConverterFactory;
import ma.glasnost.orika.converter.DefaultConverterFactory;
import ma.glasnost.orika.impl.ConfigurableMapper;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 自定义Mapper组件配置
 *
 * @author ZhouYi
 * @since 2021/08/06
 * @version 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnClass(ma.glasnost.orika.MapperFactory.class)
@AutoConfigureBefore(value = {WebMvcAutoConfiguration.class, DispatcherServletAutoConfiguration.class})
public class ConfigurableMapperFactory {

    @Autowired(required = false)
    private List<IMapperConfigure> mapperConfigureList;

    private static ConfigurableMapper beanMapper = new ConfigurableMapper(true);

    @Bean
    public ConfigurableMapper initConfigurableMapper() {
        ConfigurableMapper configurableMapper = new ConfigurableMapper() {
            @Override
            protected void configure(MapperFactory factory) {
                if (mapperConfigureList != null) {
                    mapperConfigureList.forEach(mapperConfigure -> {
                        mapperConfigure.configure(factory);
                    });
                }
            }

            /**
             * 配置自定义的转换器
             * @param factoryBuilder fb
             */
            @Override
            protected void configureFactoryBuilder(DefaultMapperFactory.Builder factoryBuilder) {
                ConverterFactory converterFactory = new DefaultConverterFactory();
                converterFactory.registerConverter(new BigDecimalToIntegerConverter());
                converterFactory.registerConverter(new BooleanToShortConverter());
                converterFactory.registerConverter(new IntegerToDateConverter());

                factoryBuilder.converterFactory(converterFactory);
            }
        };

        ConfigurableMapperFactory.beanMapper = configurableMapper;
        log.info("###### Orika mapper init success ######");
        return configurableMapper;
    }

    /**
     *
     * @return 获取spring上下文中的ConfigurableMapper
     */
    public static ConfigurableMapper getBeanMapper() {
        return ConfigurableMapperFactory.beanMapper;
    }
}
