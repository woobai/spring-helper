package fun.gangwan.data.redis.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.gangwan.data.redis.callback.LoadCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *
 * Redis工具类
 * 为避免业务方代码太大改动，从老框架迁移
 *
 */
@Slf4j
public class RedisUtils {

    /**
     * redis操作类
     */
    private StringRedisTemplate        redis;

    private String                     redisPrefix;

    /**
     * get or set 方法分布式锁的过期时间ms
     */
    private static final long          GET_OR_SET_EXPIRED_TIME    = 10000;

    /**
     * get or set 方法休眠时间ms
     */
    private static final long          GET_OR_SET_SLEEP_TIME      = 50;

    /**
     * 默认获取锁等待时间5秒
     */
    private static final int           DEFAULT_WAIT_TIME          = 5;

    /**
     * 获取锁的随机休眠时间
     */
    private static final int           GET_LOCK_RANDOM_SLEEP_TIME = 100;

    /**
     * SCAN limit size
     */
    private static final int           SCAN_LIMIT_SIZE = 10000;

    /**
     * 工具类实体
     */
    private static volatile RedisUtils instance;

    public RedisUtils(StringRedisTemplate redis, String redisPrefix) {
        this.redis = redis;
        this.redisPrefix = redisPrefix;
    }

    public RedisUtils() {
    }

    public RedisUtils getInstance(StringRedisTemplate redis, String redisPrefix) {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    instance = new RedisUtils(redis, redisPrefix);
                }
            }
        }
        return instance;
    }

    public static RedisUtils initInstance() {
        return instance;
    }

    /**
     * 获取存储的信息
     *
     * @param key key
     * @param clazz clazz
     * @param <T> clazz
     * @return T class
     */
    public <T> T redisGetWithInstance(String key, Class<? extends T> clazz) {

        if (log.isDebugEnabled()) {
            log.debug("开始从redis取得数据并封成对象返回:key={}", key);
        }

        if (StringUtils.isBlank(key)) {
            return null;
        }

        T result = null;

        try {
            String resultStr = redis.opsForValue().get(key);
            if (StringUtils.isBlank(resultStr)) {
                return null;
            }
            result = JsonConvertUtils.json2Object(resultStr, clazz);
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("从redis取得数据失败:key={}", key, e);
            }
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("从redis取得数据并封成对象返回结束:key={},result={}", key, result);
        }

        return result;
    }

    /**
     * 获取存储的信息
     *
     * @param key key
     * @return string s
     */
    public String redisGet(String key) {

        if (log.isDebugEnabled()) {
            log.debug("开始从redis取得数据并封成对象返回:key={}", key);
        }

        String result = null;

        try {
            result = redis.opsForValue().get(key);
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("从redis取得数据失败:key={}", key, e);
            }

            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("开始从redis取得数据并封成对象返回:key={},result={}", key, result);
        }

        return result;
    }

    /**
     * <p>
     * get or set 防缓存击穿,如果释放锁时恰好redis异常 导致锁释放失败,可能引起StackOverFlow
     * </p>
     *
     * @param key redis key
     * @param callback 加载数据
     * @param expiredTime key过期时间ms
     * @param clazz aaa
     * @param <T> aaa
     * @return T AAA
     */
    public <T> T redisGetOrSet(String key, LoadCallback<T> callback, long expiredTime, Class<? extends T> clazz) {
        return redisGetOrSet(key, callback, expiredTime, GET_OR_SET_EXPIRED_TIME, clazz);
    }

    /**
     * <p>
     * get or set 防缓存击穿,如果释放锁时恰好redis异常 导致锁释放失败,可能引起StackOverFlow
     * </p>
     *
     * @param key redis key
     * @param callback 加载数据
     * @param expiredTime key过期时间ms
     * @param waitLockTime 分布式锁的超时时间ms
     * @param clazz T
     * @param <T> aaa
     * @return T
     */
    public <T> T redisGetOrSet(String key, LoadCallback<T> callback, long expiredTime, long waitLockTime,
                               Class<? extends T> clazz) {
        T value = redisGetWithInstance(key, clazz);
        if (null != value) {
            return value;
        }
        String keyMutex = key + "_mutex";
        Object lockValue = getAtomLock(keyMutex, waitLockTime / 1000);
        if (!StringUtils.isBlank(lockValue)) {
            //获取锁成功
            T loadValue = null;
            try {
                T checkValue = redisGetWithInstance(key, clazz);
                if (null != checkValue) {
                    return checkValue;
                }
                loadValue = callback.load();
                if (null == loadValue) {
                    loadValue = clazz.newInstance();
                }
                redisSet(key, loadValue, expiredTime, TimeUnit.MILLISECONDS);
            } catch (InstantiationException | IllegalAccessException e) {
                log.error("redisGetOrSet异常: ", e);
            } finally {
                //可能失败
                releaseLock(keyMutex, String.valueOf(lockValue));
            }
            return loadValue;
        } else {
            //其他线程休息50毫秒后重试
            try {
                Thread.sleep(GET_OR_SET_SLEEP_TIME);
            } catch (InterruptedException e) {
                log.info("redisGetOrSet等待中断", e);
            }
            return redisGetOrSet(key, callback, expiredTime, waitLockTime, clazz);
        }
    }

    /**
     * 存储信息
     *
     * @param key key
     * @param json json
     */
    public void redisSet(String key, String json) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("开始向redis存储数据:key={},value={}", key, json);
            }

            redis.opsForValue().set(key, json);

        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("向redis存储数据失败:key={},value={}", key, json, e);
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("向redis存储数据结束:key={},value={}", key, json);
        }

    }

    /**
     * 存储信息
     *
     * @param key key
     * @param json json
     * @param timeout timeout
     * @param unit unit
     */
    public void redisSet(String key, String json, long timeout, TimeUnit unit) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("开始向redis存储数据:key={},value={},timeout={},unit={}", key, json, timeout, unit);
            }

            redis.opsForValue().set(key, json, timeout, unit);
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("向redis存储数据失败:key={},value={},timeout={},unit={}", key, json, timeout, unit, e);
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("向redis存储数据结束:key={},value={},timeout={},unit={}", key, json, timeout, unit);
        }

    }

    /**
     * 存储信息
     *
     * @param key key
     * @param json json
     */
    public void redisSet(String key, Object json) {

        try {
            if (log.isDebugEnabled()) {
                log.debug("开始向redis存储数据:key={},value={}", key, json);
            }

            redis.opsForValue().set(key, JsonConvertUtils.objectToJson(json));

        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("向redis存储数据失败:key={},value={}", key, json, e);
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("向redis存储数据结束:key={},value={}", key, json);
        }
    }

    /**
     * 存储信息
     *
     * @param key key
     * @param json json
     * @param timeout timeout
     * @param unit unit
     */
    public void redisSet(String key, Object json, long timeout, TimeUnit unit) {

        try {
            if (log.isDebugEnabled()) {
                log.debug("开始向redis存储数据:key={},value={},timeout={},unit={}", key, json, timeout, unit);
            }

            redis.opsForValue().set(key, JsonConvertUtils.objectToJson(json), timeout, unit);

        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("向redis存储数据失败:key={},value={},timeout={},unit={}", key, json, timeout, unit, e);
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("向redis存储数据结束:key={},value={},timeout={},unit={}", key, json, timeout, unit);
        }

    }

    /**
     * hashSet
     *
     * @param key key
     * @param hashKey hashKey
     * @param json json
     */
    public void redisHashSet(String key, String hashKey, String json) {

        try {
            if (log.isDebugEnabled()) {
                log.debug("开始向redis存储数据:key={},hashKey={},value={}", key, hashKey, json);
            }

            redis.opsForHash().put(key, hashKey, json);

        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("向redis存储数据失败:key={},hashKey={},value={}", key, hashKey, json, e);
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("向redis存储数据结束:key={},hashKey={},value={}", key, hashKey, json);
        }

    }

    /**
     * hashSet
     *
     * @param key key
     * @param hashKey hashKey
     * @param json json
     */
    public void redisHashSet(String key, String hashKey, Object json) {

        try {
            if (log.isDebugEnabled()) {
                log.debug("开始向redis存储数据:key={},hashKey={},value={}", key, hashKey, json);
            }

            redis.opsForHash().put(key, hashKey, JsonConvertUtils.objectToJson(json));

        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("向redis存储数据失败:key={},hashKey={},value={}", key, hashKey, json, e);
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("向redis存储数据结束:key={},hashKey={},value={}", key, hashKey, json);
        }

    }

    /**
     * <p>
     * 批量put hash
     * </p>
     *
     * @param key key
     * @param map hash键值对
     */
    public void redisHashPutAll(String key, Map<String, String> map) {

        if (log.isDebugEnabled()) {
            log.debug("开始从redis取得数据:key={},map={}", key, map);
        }

        if (CollectionUtil.isNullOrEmpty(map)) {
            return;
        }

        try {
            BoundHashOperations<String, String, String> operations = redis.boundHashOps(key);
            operations.putAll(map);
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("从redis取得数据异常:key={},map={}", key, map, e);
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("从redis取得数据并封成对象结束:key={},map={}", key, map);
        }

    }

    /**
     * <p>
     * 批量put hash
     * </p>
     *
     * @param key key
     * @param map hash键值对
     */
    public void redisHashPutAllObj(String key, Map<String, Object> map) {

        if (log.isDebugEnabled()) {
            log.debug("开始从redis取得数据:key={},map={}", key, map);
        }

        if (CollectionUtil.isNullOrEmpty(map)) {
            return;
        }

        try {
            BoundHashOperations<String, String, String> operations = redis.boundHashOps(key);
            Map<String, String> strMap = new HashMap<>(map.size());
            map.forEach((s, o) -> {
                if (String.class.isAssignableFrom(o.getClass())) {
                    strMap.put(s, String.valueOf(o));
                } else {
                    strMap.put(s, JsonConvertUtils.objectToJson(o));
                }
            });
            operations.putAll(strMap);
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("从redis取得数据异常:key={},map={}", key, map, e);
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("从redis取得数据并封成对象结束:key={},map={}", key, map);
        }

    }

    /**
     * redisHashGet
     *
     * @param key key
     * @param hashKey hashKey
     * @return  a
     */
    public Object redisHashGet(String key, String hashKey) {

        if (log.isDebugEnabled()) {
            log.debug("开始从redis取得数据:key={},hashKey={}", key, hashKey);
        }

        Object result = null;

        try {
            result = redis.opsForHash().get(key, hashKey);
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("从redis取得数据异常:key={},hashKey={}", key, hashKey, e);
            }
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("从redis取得数据并封成对象结束:key={},hashKey={},result={}", key, hashKey, result);
        }

        return result;
    }

    /**
     * <p>
     * 批量获取hash
     * </p>
     *
     * @param key key
     * @param hashKeys hashKey集合
     * @return Object集合，数据不存在时返回<code>null</code>
     */
    public List<Object> redisHashMultiGet(String key, List<String> hashKeys) {
        return redisHashMultiGetWithInstance(key, hashKeys, Object.class);
    }

    /**
     * <p>
     * get or set 防缓存击穿 hash,如果释放锁时恰好redis异常 导致锁释放失败,可能引起StackOverFlow
     * </p>
     *
     * @param key redis key
     * @param hashKey redis hashKey
     * @param callback 加载数据
     * @param expiredTime key过期时间ms
     * @param clazz T
     * @return T
     */
//    public <T> T redisHashGetOrSet(String key, String hashKey, LoadCallback<T> callback, long expiredTime,
//                                   Class<? extends T> clazz) {
//        return redisHashGetOrSet(key, hashKey, callback, expiredTime, GET_OR_SET_EXPIRED_TIME, clazz);
//    }

    /**
     * <p>
     * get or set 防缓存击穿 hash,如果释放锁时恰好redis异常 导致锁释放失败,可能引起StackOverFlow
     * </p>
     *
     * @param key redis key
     * @param hashKey redis hashKey
     * @param callback 加载数据
     * @param expiredTime key过期时间ms
     * @param waitLockTime 分布式锁的超时时间ms
     * @param clazz T
     * @param <T> as
     * @return T
     */
    public <T> T redisHashGetOrSet(String key, String hashKey, LoadCallback<T> callback, long expiredTime,
                                   long waitLockTime, Class<? extends T> clazz) {
        T value = redisHashGetWithInstance(key, hashKey, clazz);
        if (null != value) {
            return value;
        }
        String keyMutex = key + hashKey + "_hash_mutex";
        Object lockValue = getAtomLock(keyMutex, waitLockTime / 1000);
        if (!StringUtils.isBlank(lockValue)) {
            //获取锁成功
            T loadValue = null;
            try {
                T checkValue = redisHashGetWithInstance(key, hashKey, clazz);
                if (null != checkValue) {
                    return checkValue;
                }
                loadValue = callback.load();
                if (null == loadValue) {
                    loadValue = clazz.newInstance();
                }
                redisHashSet(key, hashKey, loadValue);
                setExpire(key, expiredTime, TimeUnit.MILLISECONDS);
            } catch (InstantiationException | IllegalAccessException e) {
                log.error("redisHashGetOrSet异常: ", e);
            } finally {
                //可能失败
                releaseLock(keyMutex, String.valueOf(lockValue));
            }

            return loadValue;
        } else {
            //其他线程休息50毫秒后重试
            try {
                Thread.sleep(GET_OR_SET_SLEEP_TIME);
            } catch (InterruptedException e) {
                log.info("redisGetOrSet等待中断", e);
            }
            return redisHashGetOrSet(key, hashKey, callback, expiredTime, waitLockTime, clazz);
        }
    }

    /**
     * redisHashGetWithInstance 获取存储的信息
     *
     * @param key key
     * @param hashKey hashKey
     * @param clazz clazz
     * @param <T> t
     * @return clazz
     */
    public <T> T redisHashGetWithInstance(String key, String hashKey, Class<? extends T> clazz) {

        if (log.isDebugEnabled()) {
            log.debug("开始从redis取得数据:key={},hashKey={}", key, hashKey);
        }

        if (StringUtils.isBlank(hashKey)) {
            return null;
        }

        T result = null;

        try {
            Object resultStr = redis.opsForHash().get(key, hashKey);
            //如果为空,直接返回null
            if (resultStr == null || StringUtils.isBlank(resultStr + "")) {
                return null;
            }
            result = JsonConvertUtils.json2Object(resultStr, clazz);
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("从redis取得数据失败:key={},hashKey={}", key, hashKey, e);
            }
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("从redis取得数据并封成对象结束:key={},hashKey={},result={}", key, hashKey, result);
        }

        return result;
    }

    /**
     * <p>
     * 批量获取hash
     * </p>
     *
     * @param key key
     * @param hashKeys hashKey集合,不能为空
     * @param clazz T
     * @param <T> t
     * @return T集合，数据不存在时返回<code>null</code>
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> redisHashMultiGetWithInstance(String key, List<String> hashKeys, Class<? extends T> clazz) {

        if (log.isDebugEnabled()) {
            log.debug("开始从redis取得数据:key={},hashKeys={}", key, hashKeys);
        }

        if (CollectionUtil.isNullOrEmpty(hashKeys)) {
            return null;
        }

        List<T> result;

        try {
            BoundHashOperations<String, String, String> operations = redis.boundHashOps(key);
            List<String> ts = operations.multiGet(hashKeys);
            //如果为空,直接返回null
            if (CollectionUtil.isNullOrEmpty(ts)) {
                return null;
            }
            //只过滤null
            List<String> nonNullResult = ts.stream().filter(Objects::nonNull).collect(Collectors.toList());
            if (CollectionUtil.isNullOrEmpty(nonNullResult)) {
                return null;
            }
            result = (List<T>) JsonConvertUtils.jsonToList(nonNullResult.toString(), clazz);
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("从redis取得数据异常:key={},hashKeys={}", key, hashKeys, e);
            }
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("从redis取得数据并封成对象结束:key={},result={}", key, result);
        }

        return result;
    }

    /**
     * http://doc.redisfans.com/key/scan.html
     * 通过scan命令获取匹配的key集合
     * @param pattern 匹配规则
     * @return asd
     */
    public Set<String> getKeysByScan(String pattern){
        return redis.execute(new RedisCallback<Set<String>>() {
            @Override
            public Set<String> doInRedis(RedisConnection connection) throws DataAccessException {

                Set<String> keys = new HashSet<>();

                Cursor<byte[]> cursor = connection.scan(
                        new ScanOptions.ScanOptionsBuilder()
                                .match(pattern)
                                .count(SCAN_LIMIT_SIZE)
                                .build()
                );
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next()));
                }
                //无需cursor.close(); execute finally会执行RedisConnectionUtils.releaseConnection
                return keys;
            }
        }, true);
    }

    /**
     * http://doc.redisfans.com/key/scan.html
     * 通过HSCAN命令迭代哈希键中的keys
     * @param hashKey hashKey
     * @param matchPattern matchPattern
     * @return ad
     */
    public Set<String> getKeysByHScan(String hashKey, String matchPattern){
        Set<String> keys = new HashSet<>();
        try {
            Cursor<Map.Entry<Object, Object>> cursor = redis.opsForHash()
                    .scan(hashKey, ScanOptions.scanOptions()
                                    .match(matchPattern)
                                    .count(SCAN_LIMIT_SIZE)
                                    .build()
            );
            while (cursor.hasNext()) {
                Map.Entry<Object, Object> entry = cursor.next();
                keys.add((String) entry.getKey());
            }
            //关闭cursor
            cursor.close();
        } catch (IOException e) {
            e.printStackTrace();
            return keys;
        }
        return keys;
    }

    /**
     * 获取key下所有hash
     *
     * @param key key
     * @param clazz T
     * @param <T> T
     * @return T Map，数据不存在时返回<code>null</code>
     */
    public <T> Map<String, T> redisHashGetAllWithInstance(String key, Class<? extends T> clazz) {
        if (log.isDebugEnabled()) {
            log.debug("开始从redis取得数据:key={}", key);
        }
        if (StringUtils.isNullOrEmpty(key)) {
            return null;
        }
        Map<String, T> all;
        try {
            RedisSerializer keySerializer = redis.getKeySerializer();
            all = redis.execute((RedisCallback<Map<String, T>>) con -> {
                Map<byte[], byte[]> result = con.hGetAll(keySerializer.serialize(key));
                if (CollectionUtil.isNullOrEmpty(result)) {
                    return null;
                }

                Map<String, T> ans = new HashMap<>(result.size());
                for (Map.Entry<byte[], byte[]> entry : result.entrySet()) {
                    T t = JsonConvertUtils.jsonToObject(new String(entry.getValue()), clazz);
                    ans.put(new String(entry.getKey()), t);
                }
                return ans;
            });
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("从redis取得数据异常:key={}", key, e);
            }
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("从redis取得数据并封成对象结束:key={},result={}", key, all);
        }
        return all;
    }

    /**
     * redisHashGet
     *
     * @param key key
     * @param hashKey hashKey
     */
    public void redisHashDelete(String key, String hashKey) {

        if (log.isDebugEnabled()) {
            log.debug("开始删除redis缓存数据:key={},hashKey={}", key, hashKey);
        }

        boolean flag = false;
        try {
            flag = redis.opsForHash().get(key, hashKey) != null ? true : false;
            if (flag) {
                redis.opsForHash().delete(key, hashKey);
            }
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("删除redis缓存数据失败:key={},hashKey={}", key, hashKey, e);
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("删除redis缓存数据结束:key={},hashKey={}", key, hashKey);
        }

        return;
    }

    /**
     * 追加信息
     *
     * @param key key
     * @param json json
     */
    public void redisAppend(String key, String json) {

        if (log.isDebugEnabled()) {
            log.debug("开始向redis中追加数据:key={}", key);
        }

        try {
            redis.opsForValue().append(key, json);

        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("向redis中追加数据失败:key={}", key, e);
            }
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("向redis中追加数据结束:key={}", key);
        }
    }

    /**
     * 删除信息
     *
     * @param key key
     */
    public void redisDelete(String key) {

        if (log.isDebugEnabled()) {
            log.debug("开始从redis删除数据:key={}", key);
        }

        boolean flag = false;
        try {
            flag = redis.opsForValue().get(key) != null ? true : false;
            if (flag) {
                redis.delete(key);
            }
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("从redis删除数据异常:key={}", key, e);
            }

            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("从redis删除数据结束:key={}", key);
        }
    }

    /**
     * 删除，删除前未执行查询操作，请谨慎使用，建议使用{RedisUtils#redisDelete}
     *
     * @param key key
     */
    public void redisDel(String key) {

        if (log.isDebugEnabled()) {
            log.debug("开始从redis删除数据:key={}", key);
        }

        try {
            redis.delete(key);
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("从redis删除数据异常:key={}", key, e);
            }

            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("从redis删除数据结束:key={}", key);
        }
    }

    /**
     * 获取存储的信息转化为list
     *
     * @param key key
     * @param clazz clazz
     * @param <T> t
     * @return aa as
     */
    public <T> List<T> redisGetToList(String key, Class<T> clazz) {

        if (log.isDebugEnabled()) {
            log.debug("开始从redis取得数据并封成list返回:key={}", key);
        }

        List<T> result = null;

        try {
            String resultstr = redis.opsForValue().get(key);
            if (StringUtils.isBlank(resultstr)) {
                return null;
            }
            result = JsonConvertUtils.jsonToList(resultstr, clazz);
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("从redis取得数据失败:key={}", key, e);
            }

            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("开始从redis取得数据并封成对象返回结束:key={},result={}", key, result);
        }

        return result;
    }

    /**
     * <p>
     * get or set 防缓存击穿,如果释放锁时恰好redis异常 导致锁释放失败,可能引起StackOverFlow
     * </p>
     *
     * @param key redis key
     * @param callback 加载数据
     * @param expiredTime key过期时间ms
     * @param clazz T
     * @param <T> s
     * @return T
     */
    public <T> List<T> redisGetOrSetList(String key, LoadCallback<List<T>> callback, long expiredTime,
                                         Class<? extends T> clazz) {
        return redisGetOrSetList(key, callback, expiredTime, GET_OR_SET_EXPIRED_TIME, clazz);
    }

    /**
     * <p>
     * get or set 防缓存击穿,如果释放锁时恰好redis异常 导致锁释放失败,可能引起StackOverFlow
     * </p>
     *
     * @param key redis key
     * @param callback 加载数据
     * @param expiredTime key过期时间ms
     * @param waitLockTime 分布式锁的超时时间ms
     * @param clazz T
     * @param <T> T
     * @return T
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> redisGetOrSetList(String key, LoadCallback<List<T>> callback, long expiredTime,
                                         long waitLockTime, Class<? extends T> clazz) {
        List<T> values = (List<T>) redisGetToList(key, clazz);
        if (null != values) {
            return values;
        }
        String keyMutex = key + "_mutex_list";
        Object lockValue = getAtomLock(keyMutex, waitLockTime / 1000);
        if (!StringUtils.isBlank(lockValue)) {
            //获取锁成功
            List<T> loadValue;
            try {
                List<T> checkValue = (List<T>) redisGetToList(key, clazz);
                if (null != checkValue) {
                    return checkValue;
                }
                loadValue = callback.load();
                if (null == loadValue) {
                    loadValue = Collections.emptyList();
                }
                redisSet(key, loadValue, expiredTime, TimeUnit.MILLISECONDS);
            } finally {
                //可能失败
                releaseLock(keyMutex, String.valueOf(lockValue));
            }
            return loadValue;
        } else {
            //其他线程休息50毫秒后重试
            try {
                Thread.sleep(GET_OR_SET_SLEEP_TIME);
            } catch (InterruptedException e) {
                log.info("redisGetOrSet等待中断", e);
            }
            return redisGetOrSetList(key, callback, expiredTime, waitLockTime, clazz);
        }
    }

    /**
     * 以key -- hashkey的形式获取存储的数据
     *
     * @param key key
     * @param hashKey hashKey
     * @param clazz clazz
     * @param <T> t
     * @return clazz
     */
    public <T> List<T> redisHashGetToList(String key, String hashKey, Class<T> clazz) {

        if (log.isDebugEnabled()) {
            log.debug("开始从redis取得数据:key={},hashKey={}", key, hashKey);
        }

        if (StringUtils.isBlank(key) || StringUtils.isBlank(hashKey)) {
            return null;
        }

        List<T> result = null;

        try {
            Object resultobj = redis.opsForHash().get(key, hashKey);
            //如果为空,直接返回null
            if (StringUtils.isBlank(resultobj)) {
                return null;
            }
            result = JsonConvertUtils.jsonToList(resultobj + "", clazz);
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("从redis取得数据失败:key={},hashKey={}", key, hashKey, e);
            }
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("从redis取得数据结束:key={},hashKey={}", key, hashKey);
        }

        return result;
    }

    /**
     * <p>
     * get or set 防缓存击穿 hash,如果释放锁时恰好redis异常 导致锁释放失败,可能引起StackOverFlow
     * </p>
     *
     * @param key redis key
     * @param hashKey redis hashKey
     * @param callback 加载数据
     * @param expiredTime key过期时间ms
     * @param clazz T
     * @param <T> t
     * @return T
     */
    public <T> List<T> redisHashGetOrSetList(String key, String hashKey, LoadCallback<List<T>> callback,
                                             long expiredTime, Class<? extends T> clazz) {
        return redisHashGetOrSetList(key, hashKey, callback, expiredTime, GET_OR_SET_EXPIRED_TIME, clazz);
    }

    /**
     * <p>
     * get or set 防缓存击穿 hash,如果释放锁时恰好redis异常 导致锁释放失败,可能引起StackOverFlow
     * </p>
     *
     * @param key redis key
     * @param hashKey redis hashKey
     * @param callback 加载数据
     * @param expiredTime key过期时间ms
     * @param waitLockTime 分布式锁的超时时间ms
     * @param clazz T
     * @param <T> t
     * @return T
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> redisHashGetOrSetList(String key, String hashKey, LoadCallback<List<T>> callback,
                                             long expiredTime, long waitLockTime, Class<? extends T> clazz) {
        List<T> values = (List<T>) redisHashGetToList(key, hashKey, clazz);
        if (null != values) {
            return values;
        }
        String keyMutex = key + hashKey + "_hash_mutex_list";
        Object lockValue = getAtomLock(keyMutex, waitLockTime / 1000);
        if (!StringUtils.isBlank(lockValue)) {
            //获取锁成功
            List<T> loadValue;
            try {
                List<T> checkValue = (List<T>) redisHashGetToList(key, hashKey, clazz);
                if (null != checkValue) {
                    return checkValue;
                }
                loadValue = callback.load();
                if (null == loadValue) {
                    loadValue = Collections.emptyList();
                }
                redisHashSet(key, hashKey, loadValue);
                setExpire(key, expiredTime, TimeUnit.MILLISECONDS);
            } finally {
                //可能失败
                releaseLock(keyMutex, String.valueOf(lockValue));
            }
            return loadValue;
        } else {
            //其他线程休息50毫秒后重试
            try {
                //可能失败
                Thread.sleep(GET_OR_SET_SLEEP_TIME);
            } catch (InterruptedException e) {
                log.info("redisGetOrSet等待中断", e);
            }
            return redisHashGetOrSetList(key, hashKey, callback, expiredTime, waitLockTime, clazz);
        }
    }

    /**
     * @return the redisPrefix
     */
    public String getRedisPrefix() {
        return redisPrefix;
    }

    /**
     * @param redisPrefix the redisPrefix to set
     */
    public void setRedisPrefix(String redisPrefix) {
        this.redisPrefix = redisPrefix;
    }

    public void setExpire(String key, long timeout, TimeUnit unit) {
        redis.expire(key, timeout, unit);
    }

    /**
     * 自增长
     * <span>
     *     注意：脚手架3.0.0版本以后禁止使用该方法，请更换RedisCounter计数器工具类
     * </span>
     *
     * @param key key
     * @return 自增后的值
     */
    @Deprecated
    public Long increment(String key) {
        if (log.isDebugEnabled()) {
            log.debug("开始redis自增长:key={}", key);
        }
        Long increment;
        try {
            RedisAtomicLong redisAtomicLong = new RedisAtomicLong(key, redis.getConnectionFactory());
            increment = redisAtomicLong.incrementAndGet();
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("redis自增长失败:key={}", key, e);
            }
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("redis自增长 返回:key={},result={}", key, increment);
        }
        return increment;
    }

    /**
     * 自增长
     * <span>
     *     注意：脚手架3.0.0版本以后禁止使用该方法，请更换RedisCounter计数器工具类
     * </span>
     *
     * @param key key
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 加1后的值
     */
    @Deprecated
    public Long increment(String key, long timeout, TimeUnit unit) {
        if (log.isDebugEnabled()) {
            log.debug("开始redis自增长:key={}", key);
        }
        Long increment;
        try {
            RedisAtomicLong redisAtomicLong = new RedisAtomicLong(key, redis.getConnectionFactory());
            redisAtomicLong.expire(timeout, unit);
            increment = redisAtomicLong.incrementAndGet();
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("redis自增长失败:key={}", key, e);
            }
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("redis自增长 返回:key={},result={}", key, increment);
        }
        return increment;
    }

    /**
     * 自增长
     * <span>
     *     注意：脚手架3.0.0版本以后禁止使用该方法，请更换RedisCounter计数器工具类
     * </span>
     *
     * @param key key
     * @param value 初始值
     * @return asd
     */
    @Deprecated
    public Long increment(String key, Long value) {
        if (log.isDebugEnabled()) {
            log.debug("开始redis自增长:key={},value={}", key, value);
        }
        Long increment;
        try {
            RedisAtomicLong redisAtomicLong = new RedisAtomicLong(key, redis.getConnectionFactory(), value);
            increment = redisAtomicLong.incrementAndGet();
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("redis自增长失败:key={},value={}", key, value, e);
            }
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("redis自增长 返回:key={},value={},result={}", key, value, increment);
        }
        return increment;
    }

    /**
     * 自增长
     * <span>
     *     注意：脚手架3.0.0版本以后禁止使用该方法，请更换RedisCounter计数器工具类
     * </span>
     *
     * @param key key
     * @param value 初始值
     * @param timeout 过期时间
     * @param unit 过期时间单位
     * @return a
     */
    @Deprecated
    public Long increment(String key, Long value, long timeout, TimeUnit unit) {
        if (log.isDebugEnabled()) {
            log.debug("开始redis自增长:key={},value={},timeout={},unit={}", key, value, timeout, unit);
        }
        Long increment;
        try {
            RedisAtomicLong redisAtomicLong = new RedisAtomicLong(key, redis.getConnectionFactory(), value);
            redisAtomicLong.expire(timeout, unit);
            increment = redisAtomicLong.incrementAndGet();
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("redis自增长失败:key={},value={},timeout={},unit={}", key, value, timeout, unit, e);
            }
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("redis自增长 返回:key={},value={},,timeout={},unit={},result={}", key, value, timeout, unit,
                    increment);
        }
        return increment;
    }

    /**
     * 自减
     * <span>
     *     注意：脚手架3.0.0版本以后禁止使用该方法，请更换RedisCounter计数器工具类
     * </span>
     *
     * @param key key
     * @return a
     */
    @Deprecated
    public Long dncrement(String key) {
        if (log.isDebugEnabled()) {
            log.debug("开始redis自减:key={}", key);
        }
        Long dncrement;
        try {
            RedisAtomicLong redisAtomicLong = new RedisAtomicLong(key, redis.getConnectionFactory());
            dncrement = redisAtomicLong.decrementAndGet();
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("redis自减失败:key={}", key, e);
            }
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("redis自减 返回:key={},result={}", key, dncrement);
        }
        return dncrement;
    }

    /**
     * 自减
     * <span>
     *     注意：脚手架3.0.0版本以后禁止使用该方法，请更换RedisCounter计数器工具类
     * </span>
     *
     * @param key key
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 减1后的值
     */
    @Deprecated
    public Long dncrement(String key, long timeout, TimeUnit unit) {
        if (log.isDebugEnabled()) {
            log.debug("开始redis自减:key={}", key);
        }
        Long dncrement;
        try {
            RedisAtomicLong redisAtomicLong = new RedisAtomicLong(key, redis.getConnectionFactory());
            redisAtomicLong.expire(timeout, unit);
            dncrement = redisAtomicLong.decrementAndGet();
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("redis自减失败:key={}", key, e);
            }
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("redis自减 返回:key={},result={}", key, dncrement);
        }
        return dncrement;
    }

    /**
     * 自减
     * <span>
     *     注意：脚手架3.0.0版本以后禁止使用该方法，请更换RedisCounter计数器工具类
     * </span>
     *
     * @param key key
     * @param value 初始值
     * @param timeout 过期时间
     * @param unit 过期时间单位
     * @return aa
     */
    @Deprecated
    public Long dncrement(String key, Long value, long timeout, TimeUnit unit) {
        if (log.isDebugEnabled()) {
            log.debug("开始redis自减:key={},value={}", key, value);
        }
        Long dncrement;
        try {
            RedisAtomicLong redisAtomicLong = new RedisAtomicLong(key, redis.getConnectionFactory(), value);
            redisAtomicLong.expire(timeout, unit);
            dncrement = redisAtomicLong.decrementAndGet();
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("redis自减失败:key={},value={},timeout={},unit={}", key, value, timeout, unit, e);
            }
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug(
                    "redis自增长 返回:key={},value={},,timeout={},unit={},result={}", key, value, timeout, unit, dncrement
            );
        }
        return dncrement;
    }

    /**
     * 获取自增长的值
     * <span>
     *     注意：脚手架3.0.0版本以后禁止使用该方法，请更换RedisCounter计数器工具类
     * </span>
     *
     * @param key key
     * @return a
     */
    @Deprecated
    public Long getForConn(String key) {
        if (log.isDebugEnabled()) {
            log.debug("开始redis获取自增长的值:key={}", key);
        }
        Long num;
        try {
            RedisAtomicLong redisAtomicLong = new RedisAtomicLong(key, redis.getConnectionFactory());
            num = redisAtomicLong.get();
        } catch (Exception e) {
            if (log.isInfoEnabled()) {
                log.info("redis获取自增长的值失败:key={}", key, e);
            }
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("redis获取自增长的值返回:key={},result={}", key, num);
        }
        return num;
    }

    /**
     * redis加锁
     *
     * @param key 锁的key值
     * @param expiredTime 超时时间
     * @return bool
     */
    public Boolean getLock(final String key, final long expiredTime) {
        return redis.execute(new RedisCallback<Boolean>() {

            @Override
            public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
                RedisSerializer keySerializer = redis.getKeySerializer();
                RedisSerializer<String> valueSerializer = redis.getStringSerializer();

                // 返回是否获取到锁
                boolean result = true;
                // 循环获取
                while (true) {
                    // 时间戳
                    long value = System.currentTimeMillis() + expiredTime;
                    // 序列化
                    byte[] byteKeys = keySerializer.serialize(key);
                    byte[] byteValues = valueSerializer.serialize(value + "");
                    // 获取锁 true代表获取到锁，false代表没有获取到
                    boolean isLock = connection.setNX(byteKeys, byteValues);
                    // 如果没有获取到锁
                    if (!isLock) {
                        // 获取锁的过期时间
                        byte[] byteTime = connection.get(byteKeys);
                        String time = valueSerializer.deserialize(byteTime);
                        // 如果锁不存在
                        if (null == time) {
                            result = false;
                            continue;
                        }
                        // 如果锁超时
                        if (Long.parseLong(time) < System.currentTimeMillis()) {
                            // 时间戳
                            value = System.currentTimeMillis() + expiredTime;
                            // 序列化
                            byteValues = valueSerializer.serialize(value + "");
                            // 设置新的过期时间并返回旧的过期时间
                            byte[] oldByteTime = connection.getSet(byteKeys, byteValues);
                            String oldTime = valueSerializer.deserialize(oldByteTime);
                            // 如果锁不存在
                            if (null == oldTime) {
                                result = false;
                                continue;
                            }
                            // 如果time=oldTime，代表获取到时间戳
                            if (Long.parseLong(time) == Long.parseLong(oldTime)) {
                                result = true;
                                break;
                            } else {
                                // 如果time!=oldTime，代表没有获取到时间戳，继续循环
                                try {
                                    // 休眠
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    log.warn("redis锁被中断 ", e);
                                }
                                result = false;
                                continue;
                            }
                        }
                    } else {
                        // 获取到锁
                        result = true;
                        break;
                    }
                }

                return result;
            }
        });
    }

    /**
     * 获取原子锁，获取不到可以重试
     * @param key 锁key
     * @param timeoutSeconds 锁过期时间
     * @param retryTimes 重试次数
     * @param retryInterval 重试间隔
     * @return string
     */
    public String getAtomLock(String key, long timeoutSeconds, int retryTimes, long retryInterval) {
        try {
            String value = UUID.randomUUID().toString();
            RedisSerializer keySerializer = redis.getKeySerializer();
            RedisSerializer<String> valueSerializer = redis.getStringSerializer();

            // 序列化
            byte[] byteKeys = keySerializer.serialize(key);
            byte[] byteValues = valueSerializer.serialize(value);

            boolean lockStat = Boolean.TRUE.equals(redis.execute((RedisCallback<Boolean>) (connection) ->
                    {
                        assert byteKeys != null;
                        assert byteValues != null;
                        return connection.set(
                                byteKeys, byteValues,
                                Expiration.milliseconds(timeoutSeconds * 1000L),
                                RedisStringCommands.SetOption.SET_IF_ABSENT
                        );
                    }
            ));
            if (lockStat) {
                return value;
            } else {
                if (--retryTimes < 0) {
                    return null;
                }
                //指定间隔后重试
                try {
                    TimeUnit.MILLISECONDS.sleep(retryInterval);
                } catch (InterruptedException exception) {
                    log.error("获取锁重试失败:key={}", key, exception);
                }
                return getAtomLock(key, timeoutSeconds, retryTimes, retryInterval);
            }
        } catch (Exception exception) {
            log.error("获取锁失败:key={}", key, exception);
        }
        return null;
    }

    /**
     * redis加锁
     *
     * @param key 锁的key值
     * @param expiredTime 超时时间
     * @return str
     */
    public String getLockSimple(final String key, final long expiredTime) {
        return redis.execute((RedisCallback<String>) connection -> {

            RedisSerializer keySerializer = redis.getKeySerializer();
            RedisSerializer<String> valueSerializer = redis.getStringSerializer();

            // 时间戳
            long value = System.currentTimeMillis() + expiredTime;
            // 序列化
            byte[] byteKeys = keySerializer.serialize(key);
            byte[] byteValues = valueSerializer.serialize(value + "");
            // 获取锁 true代表获取到锁，false代表没有获取到
            Boolean isLock = connection.setNX(byteKeys, byteValues);
            log.debug("key={} setNX结果:{}", key, isLock);
            if (null == isLock || !isLock) {
                return null;
            }
            connection.pExpire(byteKeys, expiredTime);
            //把value返回，释放锁时用于判断是否为当前线程的锁
            String val = valueSerializer.deserialize(byteValues);
            log.debug("redis加锁getLockSimple 获取锁成功:key={},expiredTime={},value={}", key, expiredTime, val);
            return val;
        });
    }

    /**
     * 释放锁
     *
     * @param key key
     * @param value setNX的值
     * @return 成功返回true,失败返回false
     */
    public Boolean unLock(final String key, final String value) {

        boolean flag = false;
        try {
            flag = value.equals(redis.opsForValue().get(key));
            if (!flag) {
                return false;
            }
            redis.delete(key);
        } catch (Exception e) {
            log.info("从redis释放锁异常:key={},value={}", key, value, e);
            return false;
        }

        return true;
    }

    /**
     * 获取计数信号量，用于处理限制并发访问同一资源的客户端数量
     *
     * @param key key
     * @param limit 允许并发访问总量
     * @return boolean
     */
    public boolean getSemaphore(final String key, final int limit) {
        return getSemaphore(key, limit, 2000);
    }

    /**
     * 获取计数信号量，用于处理限制并发访问同一资源的客户端数量
     *
     * @param key key
     * @param limit 允许并发访问总量
     * @param expiredTime expiredTime
     * @return boolean
     */
    public Boolean getSemaphore(final String key, final int limit, final long expiredTime) {
        return redis.execute((RedisCallback<Boolean>) connection -> {
            RedisSerializer keySerializer = redis.getKeySerializer();
            RedisSerializer<String> valueSerializer = redis.getStringSerializer();
            // 返回是否获取到计数量
            boolean result = false;

            // 获取锁
            String lock = "semaphore_lock";
            if (getLock(lock, expiredTime)) {
                // 序列化
                byte[] byteKeys = keySerializer.serialize(key);
                byte[] byteValues = valueSerializer.serialize(UUID.randomUUID().toString());

                // 计时器
                byte[] countByteKeys = keySerializer.serialize(key + "_count");
                long count = connection.incr(countByteKeys);

                log.debug(System.currentTimeMillis() + "_" + count);
                // 添加有序集合元素
                connection.zAdd(byteKeys, count, byteValues);
                long rank = connection.zRank(byteKeys, byteValues);
                // zset排名是从0开始
                if (rank <= limit - 1) {
                    result = true;
                } else {
                    // 如果排名超过总计数量，删除添加元素
                    connection.zRem(byteKeys, byteValues);
                }
                log.debug(result + "_" + count);

                // 释放锁
                redisDelete(lock);
            }

            return result;
        });
    }

    /**
     * 添加对象并同时设置失效时间
     * @param key key
     * @param expires expires
     * @param value value
     * @return asd
     */
    public boolean redisSetWithExpires(final String key, final Long expires, final String value) {
        redis.execute(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                RedisSerializer keySerializer = redis.getKeySerializer();
                connection.setEx(keySerializer.serialize(key), expires, redis.getStringSerializer().serialize(value));
                return true;
            }
        });
        return false;
    }

    /**
     * 发布消息
     *
     * @param channel channel
     * @param message message
     * @return bool
     */
    public boolean publisher(String channel, String message) {

        try {
            redis.convertAndSend(channel, message);
            return true;
        } catch (Exception e) {
            log.info("消息发布失败,channel={},message={}", channel, message, e);
        }

        return false;
    }

    /**
     * redisFireWall
     *
     * @param key key
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return bool
     */
    public boolean redisFireWall(String key, long timeout, TimeUnit unit) {
        String value = redisGet(key);
        if (StringUtils.isNotBlank(value)) {
            return true;
        } else {
            redisSet(key, "1", timeout, unit);
        }
        return false;
    }

    /**
     * 分布式锁
     *
     * @param key 锁key
     * @param timeoutSeconds 超时时间，单位s
     * @return 如果获取锁成功，返回设置到该key的值(用于释放锁)，获取锁失败返回<code>null</code>
     */
    public Object getAtomLock(String key, long timeoutSeconds) {

        try {
            String value = UUID.randomUUID().toString();
            RedisSerializer serializer = redis.getKeySerializer();
            boolean lockStat = redis.execute((RedisCallback<Boolean>) connection -> {
                return connection.set(serializer.serialize(key), value.getBytes(Charset.forName("UTF-8")),
                        Expiration.milliseconds(timeoutSeconds * 1000), RedisStringCommands.SetOption.SET_IF_ABSENT);
            });

            if (lockStat) {
                return value;
            }

        } catch (Exception ex) {
            log.error("获取锁失败:key={}", key, ex);
        }
        return null;
    }

    /**
     * 分布式锁，可能引起cpu升高
     *
     * @param key 锁key
     * @param timeoutSeconds 超时时间，单位s
     * @param waitTime 等待获取锁时间，单位s，默认5s
     * @return 如果获取锁成功，返回设置到该key的值(用于释放锁)，获取锁失败返回<code>null</code>
     */
    public String getAtomLock(String key, long timeoutSeconds, long waitTime) {
        if (waitTime < 1) {
            waitTime = DEFAULT_WAIT_TIME;
        }
        long waitTimeSecond = waitTime * 1000;
        Random random = new Random();
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < waitTimeSecond) {
            Object lockValue = getAtomLock(key, timeoutSeconds);
            if (null != lockValue) {
                // 上锁成功结束请求
                return lockValue.toString();
            }
            /* 随机延迟 */
            try {
                Thread.sleep(random.nextInt(GET_LOCK_RANDOM_SLEEP_TIME));
            } catch (InterruptedException e) {
                log.warn("获取锁等待中断:key={}", key, e);
            }
        }
        return null;
    }

    /**
     * 释放锁
     *
     * @param key 锁key
     * @param value 锁value 来自getAtomLock()
     */
    public void releaseLock(String key, String value) {
        // 在releaseLock前，存在由于锁自动过期，而被其他线程获取同一个锁的可能
        try {
            RedisSerializer serializer = redis.getKeySerializer();
            //结果为1释放成功
            boolean unLockStat = Boolean.TRUE.equals(redis.execute((RedisCallback<Boolean>) connection -> {
                //结果为1释放成功
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                return connection.eval(script.getBytes(), ReturnType.BOOLEAN, 1, serializer.serialize(key),
                        value.getBytes(StandardCharsets.UTF_8));
            }));

            if (!unLockStat) {
                log.error("释放锁失败,msg={}", key + "已自动超时,可能已被其他线程重新获取锁");
            }

        } catch (Exception ex) {
            log.error("释放锁失败,key={}", key, ex);
        }
    }

    /**
     * 续约锁
     * @param key 锁key
     * @param value 锁value
     * @param lockTimeMilliseconds 续约时间
     * @return aaa
     */
    public boolean renewalLock(String key, String value, long lockTimeMilliseconds) {
        try {
            RedisSerializer serializer = redis.getKeySerializer();
            boolean renewalResult = Boolean.TRUE.equals(redis.execute((RedisCallback<Boolean>) (connection) -> {
                String script = "if (redis.call('get', KEYS[1]) == ARGV[1]) then redis.call('pexpire', KEYS[1], ARGV[2]); return 1; end; return 0;";
                return connection.eval(
                        script.getBytes(), ReturnType.BOOLEAN, 1,
                        serializer.serialize(key), value.getBytes(StandardCharsets.UTF_8),
                        String.valueOf(lockTimeMilliseconds).getBytes(StandardCharsets.UTF_8)
                );
            }));
            if (!renewalResult) {
                log.info("续约锁失败:key={}, value={}", key, value);
            }
            return renewalResult;
        } catch (Exception exception) {
            log.error("续约锁发生异常,key={}", key, exception);
        }
        return false;
    }

    static class StringUtils {

        public static boolean isNotBlank(String str) {
            return !isBlank(str);
        }

        public static boolean isNullOrEmpty(String str) {
            return str == null || "".equals(str.trim());
        }

        public static boolean isBlank(Object str) {
            return str == null || "".equals((str + "").trim());
        }
    }

    static class CollectionUtil{

        public static boolean isNullOrEmpty(Map<? extends Object, ? extends Object> map) {
            return map == null || map.isEmpty();
        }

        public static boolean isNullOrEmpty(Collection<? extends Object> collection) {
            return collection == null || collection.isEmpty();
        }
    }

    static class JsonConvertUtils {

        public static final ObjectMapper mapper = new ObjectMapper();
        public static final ObjectMapper mapperIgnoreNull = new ObjectMapper();

        static {
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapperIgnoreNull.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }

        public static String objectToJson(Object obj) {
            try {
                return mapper.writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                return null;
            }
        }

        public static <T> T jsonToObject(String json, Class<? extends T> clas) {
            try {
                return mapper.readValue(json, clas);
            } catch (IOException e) {
                return null;
            }
        }

        /**
         * 将json数据转换成pojo对象list
         * <p>Title: jsonToList</p>
         * <p>Description: </p>
         *
         * @param jsonData
         * @param beanType
         * @return
         */
        public static <T> List<T> jsonToList(String jsonData, Class<T> beanType) {
            JavaType javaType = mapper.getTypeFactory().constructParametricType(List.class, beanType);
            try {
                List<T> list = mapper.readValue(jsonData, javaType);
                return list;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        public static <T> T json2Object(Object json, Class<? extends T> clas) {
            try {
                return mapper.readValue(json + "", clas);
            } catch (IOException e) {
                return null;
            }
        }
    }

}
