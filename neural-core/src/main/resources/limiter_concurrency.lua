-- Concurrency Limiter Script v1.0
-- 获取限流KEY(LUA下标从1开始)
local key = KEYS[1]
-- 获取限流大小参数
local limit = tonumber(ARGV[1])

-- 获取当前流量大小,没有则默认为0
local currentConcurrency = tonumber(redis.call("get", key) or "0")

-- 并发限流校验
if currentConcurrency + 1 > limit then
    -- 达到限流大小,返回状态码和并发数
    return 0
else
    -- 没有达到阈值value+1
    redis.call("INCRBY", key, 1)
    -- 没有达到限流大小,返回状态码和并发数
    return currentConcurrency + 1
end