-- 限制时间窗最大请求数 v1.0

-- 获取限流KEY(LUA下标从1开始)
local key = KEYS[1]
--- 时间窗最大请求数
local max_request = tonumber(ARGV[1])
--- 时间窗大小
local window_size = tonumber(ARGV[2])

--- 时间窗内当前请求数
local current_request = tonumber(redis.call("get", key) or 0)
if current_request + 1 > max_request then
    return 0
else
    redis.call("INCRBY", key,1)
    if window_size > 0 then
        redis.call("expire", key, window_size)
    end
    return current_request + 1
end

