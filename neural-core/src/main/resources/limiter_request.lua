-- 限制时间窗最大请求数 v1.0

-- 获取限流KEY(LUA下标从1开始)
local key = KEYS[1]
-- 时间窗口内最大并发数
local max_permits = tonumber(ARGV[1])
-- 窗口的间隔时间
local interval_milliseconds = tonumber(ARGV[2])

-- 获取当前的许可数
local current_permits = tonumber(redis.call("GET", key) or 0)

-- 如果超过了最大并发数，返回false
if current_permits + 1 > max_permits then
    return 0
else
    -- 增加并发计数
    redis.call("INCRBY", key, 1)
    -- 如果key中保存的并发计数为0，说明当前是一个新的时间窗口，它的过期时间设置为窗口的过期时间
    if current_permits == 0 then
        redis.call("PEXPIRE", key, interval_milliseconds)
    end
    return current_permits + 1
end
