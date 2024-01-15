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
 * 专门提供RedisLimiter限流基础服务的，不受限于项目和业务，提供通用的Redis功能
 */
@Service
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 限流操作
     * @param key 区分不同的限流器，比如不同用户的id
     */
    public void doRateLimit(String key) {
        //创建一个名称为User_limiter的限流器，每秒最多访问2次
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL, 2, 1, RateIntervalUnit.SECONDS);

        // 每当一个操作来了后，请求一个令牌
        boolean canOp = rateLimiter.tryAcquire(1);

        if(!canOp){
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }
}
