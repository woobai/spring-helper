/*
 * Copyright 2019 tuhu.cn All right reserved. This software is the
 * confidential and proprietary information of tuhu.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Tuhu.cn
 */
package fun.gangwan.data.redis.callback;

/**
 * <p>
 * 获取数据，当前用于 {@link fun.gangwan.data.redis.util.RedisUtils} 中。如从db中获取数据
 * 从老框架迁移
 * </p>
 * 
 */
public interface LoadCallback<T> {
    /**
     * <p>
     * 加载数据
     * </p>
     * 
     * @return T
     */
    T load();
}
