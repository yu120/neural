-- Rate Limiter Script v1.0
-- 获取限流KEY(LUA下标从1开始)
local key = KEYS[1]
-- 获取限流大小参数
local limit = tonumber(ARGV[1])

-- 获取当前流量大小,没有则默认为0
local currentRate = tonumber(redis.call("get", key) or "0")

-- 速率限流校验
local checkRate = currentRate + 1
if checkRate > limit then
    -- 达到限流大小,返回状态码和速率
    return {1, checkRate, "SUCCESS"}
else
    -- 没有达到阈值value+1
    redis.call("INCRBY", key, 1)
    -- EXPIRE后边的单位是秒
    redis.call("EXPIRE", key, 10)
    -- 没有达到限流大小,返回状态码和速率
    return {0, checkRate, "FAILURE"}
end