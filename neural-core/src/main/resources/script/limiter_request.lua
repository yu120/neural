--[[
Request Limiter Script v1.0
@author：lry(echo)
@since：2019-06-25
--]]


-- 加流量
-- @param key                  唯一标识
-- @param permit               每次获取许可数
-- @param max_permit           最大许可数
-- @param request_interval     请求时间窗大小,单位milliseconds
-- @return                     0=获取失败,1=获取成功
local function tryAcquireRequest(key, permit, max_permit, request_interval)
    local org_permit = tonumber(permit)
    local org_max_permit = tonumber(max_permit)
    local org_request_interval = tonumber(request_interval)

    -- 获取当前的许可数
    local current_permit = tonumber(redis.call('GET', key) or 0)
    local next_permit = current_permit + org_permit

    -- 如果超过了最大并发数，返回false
    if next_permit > org_max_permit then
        return {0, org_max_permit}
    else
        -- 增加并发计数
        redis.call('INCRBY', key, org_permit)
        -- 如果key中保存的并发计数为0，说明当前是一个新的时间窗口，它的过期时间设置为窗口的过期时间
        if current_permit == 0 then
            redis.call('PEXPIRE', key, org_request_interval)
        end

        return {1, next_permit}
    end
end


-- 主流程
return tryAcquireRequest(KEYS[1], KEYS[2], KEYS[3])
