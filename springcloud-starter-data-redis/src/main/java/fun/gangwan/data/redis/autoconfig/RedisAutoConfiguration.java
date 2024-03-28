package fun.gangwan.data.redis.autoconfig;

import fun.gangwan.data.redis.support.RedisCounter;
import fun.gangwan.data.redis.template.RedisPipelineTemplate;
import fun.gangwan.data.redis.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

/**
 *
 * <br> RedisAutoConfiguration </br>
 * <span> Redis自动配置组件</span>
 *
 * @author ZhouYi
 * @since 2021/08/26
 * @version 1.0.0
 *
 */
@Slf4j
@Configuration
@Import({RedisPipelineTemplate.class, RedisCounter.class})
@ConditionalOnProperty(prefix = RedisAutoConfiguration.PREFIX, value = "prefix", matchIfMissing = false)
public class RedisAutoConfiguration {

    public static final String PREFIX = "spring.redis";

    @Value("${fc.boot.dependency.version.redis:UNKNOWN}")
    private String pomVer;

    @Resource
    LettuceConnectionFactory lettuceConnectionFactory;

    /**
     *
     * @param redis
     * @param redisPrefix
     * @param validateConnection
     * @return
     */
    @Bean
    public RedisUtils initRedis(StringRedisTemplate redis,
                                @Value("${spring.redis.prefix}") String redisPrefix,
                                @Value("${spring.redis.lettuce.validateConnection:true}") boolean validateConnection) {
        lettuceConnectionFactory.setValidateConnection(validateConnection);
        log.info("###### RedisAutoConfiguration register success with dependency {} ######", pomVer);
        return new RedisUtils().getInstance(redis, redisPrefix);
    }

}
