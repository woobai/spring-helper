package fun.gangwan.data.redis.command;

/**
 *
 * Redis 命令类型枚举
 *
 */
public enum RedisCommandType {

    /**********************  通用命令 ******************/

    /**
     * 给key设置有效期
     * @see <a href="https://redis.io/commands/expire/">Redis Documentation: EXPIRE</a>
     */
    EXPIRE,

    /**
     * 查看key的剩余有效期
     * @see <a href="https://redis.io/commands/ttl">Redis Documentation: TTL</a>
     */
    TTL,

    /**
     * 判断key是否存在
     * @see <a href="https://redis.io/commands/exists">Redis Documentation: EXISTS</a>
     */
    EXISTS,

    /**
     * 删除key
     * @see <a href="https://redis.io/commands/del">Redis Documentation: DEL</a>
     */
    DEL,

    /**********************  String命令 ******************/

    /**
     * 新增键值
     * @see <a href="https://redis.io/commands/set">Redis Documentation: SET</a>
     * @see <a href="https://redis.io/commands/setex">Redis Documentation: SETEX</a>
     */
    SET,

    /**
     * 获取键值
     * @see <a href="https://redis.io/commands/get">Redis Documentation: GET</a>
     */
    GET,

    /**
     * 只有在 key 不存在时设置 key 的值
     * @see <a href="https://redis.io/commands/setnx">Redis Documentation: SETNX</a>
     */
    SETNX,

    /**
     * 将 key 中储存的数字值+1
     * @see <a href="https://redis.io/commands/incr">Redis Documentation: INCR</a>
     */
    INCR,

    /**
     * 将 key 所储存的值加上给定的步长（delta）
     * @see <a href="https://redis.io/commands/incrby">Redis Documentation: INCRBY</a>
     */
    INCRBY,

    /**
     * 将 key 中储存的数字值-1
     * @see <a href="https://redis.io/commands/decr">Redis Documentation: DECR</a>
     */
    DECR,

    /**
     * 将 key 所储存的值减去给定的步长（delta）
     * @see <a href="https://redis.io/commands/decrby">Redis Documentation: DECRBY</a>
     */
    DECRBY,

    /**********************  List命令 ******************/

    /**
     * 向列表左侧插入一个或多个元素
     * @see <a href="https://redis.io/commands/lpush">Redis Documentation: LPUSH</a>
     */
    LPUSH,

    /**
     * 弹出最左边的元素，弹出之后该值在列表中将被移除
     * @see <a href="https://redis.io/commands/lpop">Redis Documentation: LPOP</a>
     */
    LPOP,

    /**
     * 向列表右侧插入一个或多个元素
     * @see <a href="https://redis.io/commands/rpush">Redis Documentation: RPUSH</a>
     */
    RPUSH,

    /**
     * 弹出最右边的元素，弹出之后该值在列表中将被移除
     * @see <a href="https://redis.io/commands/rpop">Redis Documentation: RPOP</a>
     */
    RPOP,

    /**
     * 返回一段角标范围内的所有元素
     * @see <a href="https://redis.io/commands/lrange">Redis Documentation: LRANGE</a>
     */
    LRANGE,

    /**
     * 从列表头部开始删除值等于value的元素count次,相当于for循环执行i次remove操作
     * <a href="https://redis.io/commands/lrem">Redis Documentation: LREM</a>
     */
    LREM,

    /**
     * 保留指定范围内的元素
     * @see <a href="https://redis.io/commands/ltrim">Redis Documentation: LTRIM</a>
     */
    LTRIM;

    /**********************  TODO Hash命令 ******************/

    /**********************  TODO Set命令 ******************/

    /**********************  TODO SortedSet命令 ******************/

}
