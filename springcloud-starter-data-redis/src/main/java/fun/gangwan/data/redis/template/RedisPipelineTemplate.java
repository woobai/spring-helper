package fun.gangwan.data.redis.template;

import fun.gangwan.data.redis.command.RedisCommand;
import fun.gangwan.data.redis.command.RedisListCmdBuilder;
import io.lettuce.core.RedisCommandExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 *
 * RedisTemplate的pipeline可以将多个命令打包成一批发送给Redis服务器，减少了网络传输的次数，降低了网络延迟，提高了Redis的性能。
 * RedisTemplate的pipeline可以将多个命令缓存在客户端，等到一定数量后再一次性发送给Redis服务器，减少了Redis服务器的内存消耗。
 * RedisTemplate的pipeline可以保证命令的顺序性，批量返回操作结果，保证了数据的正确性。
 *
 *
 */
@Slf4j
@Component
public class RedisPipelineTemplate {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    /**
     * pipeline 执行批量操作
     * @param cmdList aa
     * @return  aa
     */
    public List<Object> executePipeline(List<RedisCommand> cmdList) {

        return stringRedisTemplate.executePipelined(new SessionCallback<Object>() {

            @Override
            public <String, V> Object execute(RedisOperations<String, V> operations) throws DataAccessException {

                ValueOperations<String, Object> ops = (ValueOperations<String, Object>) operations.opsForValue();
                ListOperations<String, Object> listOps = (ListOperations<String, Object>) operations.opsForList();


                for (RedisCommand command : cmdList) {

                    if(StringUtils.isBlank(command.getKey())){
                        throw new RedisCommandExecutionException("非法命令集，键不能为空");
                    }
                    Object v = command.getValue();
                    String k = (String) command.getKey();
                    RedisListCmdBuilder listCmd = command.getRedisListCmdBuilder();

                    switch (command.getCommandType()) {
                        /**
                         * String命令
                         */
                        case SET:
                            if(command.getTimeUnit() == null || command.getExpiredTime() == null){
                                ops.set(k, v);
                            }else{
                                ops.set(k, v, command.getExpiredTime(), command.getTimeUnit());
                            }
                            log.debug("Redis Pipeline SET {} {}", k, v);
                            break;
                        case GET:
                            ops.get(k);
                            log.debug("Redis Pipeline GET {}", k);
                            break;
                        case SETNX:
                            ops.setIfAbsent(k, v);
                            log.debug("Redis Pipeline SETNX {}", k);
                            break;
                        case INCR:
                            ops.increment(k);
                            log.debug("Redis Pipeline INCR {}", k);
                            break;
                        case INCRBY:
                            ops.increment(k, command.getDelta());
                            log.debug("Redis Pipeline INCRBY {} by {}", k, command.getDelta());
                            break;
                        case DECR:
                            ops.decrement(k);
                            log.debug("Redis Pipeline DECR {}", k);
                            break;
                        case DECRBY:
                            ops.decrement(k, command.getDelta());
                            log.debug("Redis Pipeline DECRBY {} by {}", k, command.getDelta());
                            break;
                        /**
                         * 通用命令
                          */
                        case EXPIRE:
                            if(command.getTimeUnit() == null || command.getExpiredTime() == null){
                                throw new RedisCommandExecutionException("非法EXPIRE命令");
                            }
                            operations.expire(k, command.getExpiredTime(), command.getTimeUnit());
                            log.debug("Redis Pipeline EXPIRE {}", k);
                            break;
                        case TTL:
                            operations.getExpire(k);
                            log.debug("Redis Pipeline TTL {}", k);
                            break;
                        case EXISTS:
                            operations.hasKey(k);
                            log.debug("Redis Pipeline EXISTS {}", k);
                            break;
                        case DEL:
                            operations.delete(k);
                            log.debug("Redis Pipeline DEL {}", k);
                            break;
                        /**
                         * List命令
                         */
                        case LRANGE:
                            if(listCmd.getStart() == null || listCmd.getEnd() == null){
                                throw new RedisCommandExecutionException("非法LRANGE命令");
                            }
                            listOps.range(k, listCmd.getStart(), listCmd.getEnd());
                            log.debug("Redis Pipeline LRANGE {} from {} to {}", k, listCmd.getStart(), listCmd.getEnd());
                            break;
                        case LPUSH:
                            listOps.leftPush(k, v);
                            log.debug("Redis Pipeline LPUSH {} : {}", k, v);
                            break;
                        case LPOP:
                            listOps.leftPop(k);
                            log.debug("Redis Pipeline LPOP {}", k);
                            break;
                        case RPUSH:
                            listOps.rightPush(k, v);
                            log.debug("Redis Pipeline RPUSH {} : {}", k, v);
                            break;
                        case RPOP:
                            listOps.rightPop(k);
                            log.debug("Redis Pipeline RPOP {}", k);
                            break;
                        case LREM:
                            if(listCmd.getRemoveCount() == null || Objects.isNull(v)){
                                throw new RedisCommandExecutionException("非法LREM命令");
                            }
                            listOps.remove(k, listCmd.getRemoveCount(), v);
                            log.debug("Redis Pipeline LREM {} by {} times", k, listCmd.getRemoveCount());
                            break;
                        case LTRIM:
                            if(listCmd.getStart() == null || listCmd.getEnd() == null){
                                throw new RedisCommandExecutionException("非法LTRIM命令");
                            }
                            listOps.trim(k, listCmd.getStart(), listCmd.getEnd());
                            log.debug("Redis Pipeline LTRIM {} from {} to {}", k, listCmd.getStart(), listCmd.getEnd());
                            break;
                        //添加其他命令类型的处理...
                        default:
                            throw new RedisCommandExecutionException("该版本暂不支持该Redis流水线命令: " + command.getCommandType());
                    }
                }

                // 返回值必须为null，否则会抛出异常
                return null;
            }
        });
    }

}
