package org.example.common.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class BitmapCounter {

    private final StringRedisTemplate redis;

    /**
     * 确保缓存初始化：key 不存在时，从 DB 加载
     * @param membersKey 关系 Bitmap key
     * @param countKey   计数 String key
     * @param loadFromDb 加载函数 -> (成员 id 列表)
     * @return 是否完成了初始化
     */
    public boolean ensureInit(String membersKey, String countKey, Supplier<List<Long>> loadFromDb) {
        if (Boolean.TRUE.equals(redis.hasKey(countKey))) {
            return false;
        }
        synchronized (("bitmap_init_" + membersKey).intern()) {
            if (Boolean.TRUE.equals(redis.hasKey(countKey))) {
                return false;
            }
            log.info("Bitmap 缓存冷启动: members={}, count={}", membersKey, countKey);
            List<Long> ids = loadFromDb.get();
            for (Long id : ids) {
                redis.opsForValue().setBit(membersKey, id, true);
            }
            redis.opsForValue().set(countKey, String.valueOf(ids.size()));
            return true;
        }
    }

    /**
     * 查询是否存在
     */
    public boolean contains(String membersKey, Long id) {
        return Boolean.TRUE.equals(redis.opsForValue().getBit(membersKey, id));
    }

    /**
     * 添加一个成员
     */
    public void add(String membersKey, String countKey, Long id) {
        redis.opsForValue().setBit(membersKey, id, true);
        redis.opsForValue().increment(countKey);
    }

    /**
     * 删除一个成员
     */
    public void remove(String membersKey, String countKey, Long id) {
        redis.opsForValue().setBit(membersKey, id, false);
        Long count = redis.opsForValue().decrement(countKey);
        if (count != null && count < 0) {
            redis.opsForValue().set(countKey, "0");
        }
    }

    /**
     * 获取当前计数
     */
    public long getCount(String countKey) {
        String val = redis.opsForValue().get(countKey);
        return val == null ? 0L : Long.parseLong(val);
    }

    /**
     * 清空这个缓存（删除帖子时用）
     */
    public void clear(String membersKey, String countKey) {
        redis.delete(membersKey);
        redis.delete(countKey);
    }
}
