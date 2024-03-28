package fun.gangwan.data.redis.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.TimeUnit;

/**
 *
 * Redis 命令基础对象
 *
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RedisCommand {

    /**
     *
     * 命令类型
     */
    private RedisCommandType commandType;

    /**
     * 键
     */
    private String key;

    /**
     * 值
     */
    private Object value;

    /**
     * 步长，仅对INCRBY 和 DECRBY命令有效
     */
    private Long delta;

    /**
     * key过期时间
     */
    private Long expiredTime;

    /**
     * key过期时间单位
     */
    private TimeUnit timeUnit;

    /**
     * List命令构建器
     */
    private RedisListCmdBuilder redisListCmdBuilder = new RedisListCmdBuilder();

    /**
     * Hash命令构建器
     */
    private RedisHashCmdBuilder redisHashCmdBuilder = new RedisHashCmdBuilder();

    /**
     * Set命令构建器
     */
    private RedisSetCmdBuilder redisSetCmdBuilder = new RedisSetCmdBuilder();

    /**
     * SortedSet命令构建器
     */
    private RedisSortedSetCmdBuilder redisSortedSetCmdBuilder = new RedisSortedSetCmdBuilder();

}
