-- 获取调用脚本时传入的第一个key值（用作限流的key）
local identity = KEYS[1]
-- 统计最小单元,默认为1
local countUnit = tonumber(ARGV[1])
-- 最大统计许可数
local maxCount = tonumber(ARGV[2])
-- 统计计数周期的超时时间(单位为毫秒)
local timeout = tonumber(ARGV[3])

-- 获取当前流量大小
local currentCount = tonumber(redis.call('GET', identity) or '0')
local nextCount = currentCount + countUnit

-- 是否超出限流
if nextCount > maxCount then
    -- TODO: 是否需要统计超出流量?
    -- 返回(拒绝)
    return 0, nextCount
else
    -- 没有超出value + 1
    redis.call('INCRBY', identity, countUnit)
    -- 设置过期时间
    redis.call('PEXPIRE', identity, timeout)
    -- 返回(放行)
    return 1, nextCount
end