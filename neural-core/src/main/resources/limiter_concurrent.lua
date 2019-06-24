-- Concurrent Limiter Script v1.0
-- 死信问题怎么解决

-- 获取限流KEY(LUA下标从1开始)
local key = KEYS[1]
-- 获取限流大小参数
local limit = tonumber(ARGV[1])
-- 操作类型,0表示加流量,1表示减流量
local category = tonumber(ARGV[2])

-- 获取当前流量大小,没有则默认为0
local currentConcurrent = tonumber(redis.call("GET", key) or "0")

if category == 0 then
    if currentConcurrent + 1 > limit then
        -- 达到限流大小,返回状态码和并发数
        return 0
    else
        redis.call("INCRBY", key, 1)
        return currentConcurrent + 1
    end
elseif category == 1 then
    if currentConcurrent - 1 <= 0 then
        return 0
    else
        redis.call("DECRBY", key, 1)
        return currentConcurrent - 1
    end
end
