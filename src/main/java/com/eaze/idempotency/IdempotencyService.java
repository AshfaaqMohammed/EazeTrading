package com.eaze.idempotency;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Service
public class IdempotencyService {
    private static final String KEY_PREFIX = "idemp:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public IdempotencyService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    private String redisKey(String scopedKey) {
        return KEY_PREFIX + scopedKey;
    }

    public boolean tryBegin(String scopedKey) {
        IdempotencyRecord inProgress = new IdempotencyRecord(IdempotencyRecord.State.IN_PROGRESS, 0,null);
        String json = write(inProgress);
        Boolean won = redis.opsForValue().setIfAbsent(redisKey(scopedKey),json,TTL);
        return Boolean.TRUE.equals(won);
    }

    public void complete(String scopedKey, int httpStatus, String body) {
        IdempotencyRecord done = new IdempotencyRecord(IdempotencyRecord.State.DONE, httpStatus, body);
        redis.opsForValue().set(redisKey(scopedKey), write(done), TTL);
    }

    public void release(String scopedKey) {
        redis.delete(redisKey(scopedKey));
    }

    public IdempotencyRecord find(String scopedKey) {
        String json = redis.opsForValue().get(redisKey(scopedKey));
        return json == null ? null : read(json);
    }

    private String write(IdempotencyRecord record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize idempotency record",e);
        }
    }

    private IdempotencyRecord read(String json) {
        try {
            return objectMapper.readValue(json, IdempotencyRecord.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize idempotency record.",e);
        }
    }
}
