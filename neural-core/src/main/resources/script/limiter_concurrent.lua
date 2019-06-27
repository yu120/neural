--[[
Concurrent Limiter Script v1.0
@author：lry(echo)
@since：2019-06-25
@TODO 死信问题怎么解决？
--]]


-- 并发流量整形
-- @param key          唯一标识
-- @param permits      每次获取许可数量,大于0表示加流量,小于0表示减流量
-- @param max_permit   最大许可数
-- @return             0=获取失败,1=获取成功
local function trafficConcurrent(key, permits, max_permit)
    local org_permits = tonumber(permits)
    local org_max_permit = tonumber(max_permit)

    -- 获取当前流量大小,没有则默认为0
    local current_permit = tonumber(redis.call('GET', key) or '0')
    local next_permit = current_permit + org_permits

    if org_permits > 0 then
        -- 加流量
        if next_permit > org_max_permit then
            -- 达到限流大小,返回状态码和并发数
            return {0, org_max_permit}
        else
            redis.call('INCRBY', key, org_permits)
            return {1, next_permit}
        end
    elseif org_permits < 0 then
        -- 减流量
        if next_permit < 0 then
            return {0, 0}
        else
            redis.call('DECRBY', key, org_permits)
            return {1, next_permit}
        end
    end
end


-- 主流程
return trafficConcurrent(KEYS[1], KEYS[2], KEYS[3])
