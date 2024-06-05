package com.becoze.biback.manager;


import com.becoze.biback.common.ErrorCode;
import com.becoze.biback.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * General RedisLimiter, rate limiting only - not rely on business logic
 */
@Service
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;

    /**
     * Rate limiting
     * @param key String, Differentiate between various rate limiters, such as by different user IDs.
     */
    public void doRateLimit(String key) {
        // Create a rate limiter named User_limiter that allows a maximum of 2 accesses per second.
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL, 2, 1, RateIntervalUnit.SECONDS);

        // Request a token each time an operation comes in.
        boolean canOp = rateLimiter.tryAcquire(1);

        if(!canOp){
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }
}
