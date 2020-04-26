-- 获取调用脚本时传入的第一个key值（用作限流的key）
local identity = KEYS[1]
-- 并发最小单元,默认为1
local permitUnit = tonumber(ARGV[1])
-- 最大并发许可数
local maxPermit = tonumber(ARGV[2])
--并发计数周期的超时时间(单位为毫秒)
local timeout = tonumber(ARGV[3])

-- 获取当前流量大小
local currentConcurrent = tonumber(redis.call('GET', identity) or '0')
local nextConcurrent = currentConcurrent + permitUnit

-- 是否超出限流
if nextConcurrent > maxPermit then
    -- 返回(拒绝)
    return 0, nextConcurrent
else
    -- 没有超出value + 1
    redis.call('INCRBY', identity, permitUnit)
    -- 设置过期时间
    redis.call('PEXPIRE', identity, timeout)
    -- 返回(放行)
    return 1, nextConcurrent
end