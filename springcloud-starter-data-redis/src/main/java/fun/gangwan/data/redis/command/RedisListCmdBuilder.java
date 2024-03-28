package fun.gangwan.data.redis.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * Redis List 命令参数
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisListCmdBuilder {

    /**
     * range和trim命令 -> 开始位置
     * @see <a href="https://redis.io/commands/lrange">Redis Documentation: LRANGE</a>
     * @see <a href="https://redis.io/commands/ltrim">Redis Documentation: LTRIM</a>
     */
    private Long start;

    /**
     * range和trim命令 -> 结束位置
     * @see <a href="https://redis.io/commands/lrange">Redis Documentation: LRANGE</a>
     * @see <a href="https://redis.io/commands/ltrim">Redis Documentation: LTRIM</a>
     */
    private Long end;

    /**
     * 删除次数
     * <a href="https://redis.io/commands/lrem">Redis Documentation: LREM</a>
     */
    private Long removeCount;
}
