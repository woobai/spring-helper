package fun.gangwan.base.serialization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.PackageVersion;
import com.fasterxml.jackson.datatype.jsr310.deser.*;
import com.fasterxml.jackson.datatype.jsr310.ser.*;
import fun.gangwan.base.tools.JacksonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.TimeZone;

/**
 * JacksonCustomizerConfiguration
 * <span> Jackson自定义配置 </span>
 * <br> 配置一些自定义序列化和反序列化规则，以及一些时间格式处理 </br>
 *
 *
 */
@Slf4j
@Configuration
@ConditionalOnClass(ObjectMapper.class)
@AutoConfigureBefore(JacksonAutoConfiguration.class)
@ComponentScan(basePackageClasses = {JacksonUtils.class})
public class JacksonCustomizerConfiguration {

    @Value("${gw.boot.jackson.standard.datetime-format:yyyy-MM-dd HH:mm:ss}")
    private String standardFormat;

    @Value("${gw.boot.jackson.java8.date-format:yyyy-MM-dd}")
    private String dateFormat;

    @Value("${gw.boot.jackson.java8.year-month-format:yyyy-MM}")
    private String yearMonthFormat;

    @Value("${gw.boot.jackson.java8.time-format:HH:mm:ss}")
    private String timeFormat;

    @Value("${gw.boot.jackson.serialization.include-null:true}")
    private boolean includeNull;

    @Value("${gw.boot.jackson.serialization.include-empty:true}")
    private boolean includeEmpty;

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("${gw.boot.jackson.customizer.enable:true}")
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> {
            //本机时区
            builder.locale(Locale.getDefault());
            //全局时区
            builder.timeZone(TimeZone.getTimeZone(ZoneId.systemDefault()));
            /**
             * 配置java.util.Date的格式化
             */
            builder.simpleDateFormat(standardFormat);
            /**
             * 配置java8 时间配置,解决序列化时带有T的问题，自定义格式化字符串
             */
            builder.modules(new Java8TimeModule());

            if(!includeNull){
                //若POJO对象的属性值为null，序列化时不进行显示
                builder.serializationInclusion(JsonInclude.Include.NON_NULL);
            }

            if(!includeEmpty){
                //若POJO对象的属性值为""，序列化时不进行显示
                builder.serializationInclusion(JsonInclude.Include.NON_EMPTY);
            }

            /**
             * 需要关闭的属性
             *
             * Springboot高版本 JacksonAutoConfiguration 已经关掉以下属性
             * featureDefaults.put(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
             * featureDefaults.put(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
             *
             */
            builder.featuresToDisable(
                    //序列化日期时以timestamps输出，默认true
                    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                    SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS,
                    //如果一个类没有public的方法或属性时，会导致序列化失败。关闭后，会得到一个空JSON串。默认true
                    SerializationFeature.FAIL_ON_EMPTY_BEANS,
                    //反序列化时json串包含了pojo不存在属性时是否抛出JsonMappingException,默认true
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
            );

            log.info("###### Jackson2ObjectMapperBuilderCustomizer config success ######");
        };
    }

    public class Java8TimeModule extends SimpleModule {

        public Java8TimeModule() {
            super(PackageVersion.VERSION);

            //序列化
            this.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(standardFormat)));
            this.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(dateFormat)));
            this.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(timeFormat)));
            this.addSerializer(YearMonth.class, new YearMonthSerializer(DateTimeFormatter.ofPattern(yearMonthFormat)));
            this.addSerializer(Year.class, YearSerializer.INSTANCE);

            //反序列化
            this.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(standardFormat)));
            this.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(dateFormat)));
            this.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(timeFormat)));
            this.addDeserializer(YearMonth.class, new YearMonthDeserializer(DateTimeFormatter.ofPattern(yearMonthFormat)));
            this.addDeserializer(Year.class, YearDeserializer.INSTANCE);
        }
    }

}
