---[[
Request Limiter Script v1.0
@author：lry(echo)
@since：2019-06-25
---]]


--- 加流量
--- @param key 唯一标识
--- @param max_permit_request_s  最大许可数
--- @param request_interval_s  请求时间窗大小
--- @return 0=表示获取失败,1=表示获取成功
local function tryAcquireRequest(key , max_permit_request_s, request_interval_s)
    -- 时间窗口内最大并发数
    local max_permit_request = tonumber(max_permit_request_s)
    -- 窗口的间隔时间:milliseconds
    local request_interval = tonumber(request_interval_s)

    -- 获取当前的许可数
    local current_permit = tonumber(redis.call('GET', key) or 0)

    -- 如果超过了最大并发数，返回false
    if current_permit + 1 > max_permit_request then
        return 0
    else
        -- 增加并发计数
        redis.call('INCRBY', key, 1)
        -- 如果key中保存的并发计数为0，说明当前是一个新的时间窗口，它的过期时间设置为窗口的过期时间
        if current_permit == 0 then
            redis.call('PEXPIRE', key, request_interval)
        end

        return current_permit + 1
    end
end


--- 主流程
local method = KEYS[1]
if method == 'tryAcquireRequest' then
    return tryAcquireRequest(ARGV[1], ARGV[2], ARGV[3])
end