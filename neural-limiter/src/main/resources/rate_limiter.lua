-- 获取调用脚本时传入的第一个key值（用作限流的 key）
local key = KEYS[1]
-- 获取调用脚本时传入的第一个参数值（限流大小）
local maxLimit = tonumber(ARGV[1])

-- 获取当前流量大小
local currentLimit = tonumber(redis.call('get', key) or '0')

-- 是否超出限流
if currentLimit + 1 > maxLimit then
    -- 返回(拒绝)
    return 0, currentLimit
else
    -- 没有超出 value + 1
   local nextLimit = redis.call('INCRBY', key, 1)
    -- 设置过期时间
    redis.call('EXPIRE', key, ARGV[2])
    -- 返回(放行)
    return 1, nextLimit
end