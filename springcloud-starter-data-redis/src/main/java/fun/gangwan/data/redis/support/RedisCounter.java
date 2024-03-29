package fun.gangwan.data.redis.support;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 *
 * RedisCounter
 * <span>Redis计数器工具类，提供自增、自减计数方法</span>
 *
 *
 */
@Component
public class RedisCounter {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 获取当前计数器数值
     * @param key key
     * @return 计数器值
     */
    public Long getCounter(String key){
        return Long.parseLong(Objects.requireNonNull(stringRedisTemplate.opsForValue().get(key)));
    }

    /**
     * 自增长
     * 如果key不存在，则在执行操作前将其设置为0
     * @param key key
     * @return 自增后的值
     */
    public Long increment(String key){
        return stringRedisTemplate.opsForValue().increment(key);
    }

    /**
     * 自增长
     * 如果key不存在，则在执行操作前将其设置为0
     * @param key key
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return 自增后的值
     */
    public Long increment(String key, long timeout, TimeUnit unit){
        Long result = stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, timeout, unit);
        return result;
    }

    /**
     * 自增长,指定初始值，使用该方法请确保操作原子性
     * 如果key不存在，则在执行操作前将其设置为0
     *
     * @param key key
     * @param initValue 初始值
     * @return 自增后的值
     */
    public Long increment(String key, Long initValue){
        stringRedisTemplate.opsForValue().set(key, String.valueOf(initValue));
        return stringRedisTemplate.opsForValue().increment(key);
    }

    /**
     * 自增长,指定初始值，使用该方法请确保操作原子性
     *
     * @param key key
     * @param initValue 初始值
     * @param timeout 过期时间
     * @param unit 过期时间单位
     * @return long
     */
    public Long increment(String key, Long initValue, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, String.valueOf(initValue), timeout, unit);
        return stringRedisTemplate.opsForValue().increment(key);
    }

    /**
     * 自减
     *
     * @param key key
     * @return 自减后的值，如果键不存在，那么在执行decrement()操作前，会先将其初始化为0
     */
    public Long decrement(String key){
        return stringRedisTemplate.opsForValue().decrement(key);
    }

    /**
     * 自减
     *
     * @param key key
     * @param timeout 过期时间
     * @param unit 过期时间单位
     * @return 自减后的值，如果键不存在，那么在执行decrement()操作前，会先将其初始化为0
     */
    public Long decrement(String key, long timeout, TimeUnit unit) {
        Long result = stringRedisTemplate.opsForValue().decrement(key);
        stringRedisTemplate.expire(key, timeout, unit);
        return result;
    }

    /**
     * 自减，指定初始值，使用该方法请确保操作原子性
     *
     * @param key key
     * @param initValue  初始值
     * @param timeout 过期时间
     * @param unit  过期时间单位
     * @return 自减后的值，如果键不存在，那么在执行decrement()操作前，会先将其初始化为0
     */
    public Long decrement(String key, Long initValue, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, String.valueOf(initValue), timeout, unit);
        return stringRedisTemplate.opsForValue().decrement(key);
    }

}
