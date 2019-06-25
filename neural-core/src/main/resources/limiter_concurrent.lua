---[[
Concurrent Limiter Script v1.0
@author：lry(echo)
@since：2019-06-25
@TODO 死信问题怎么解决？
获取限流KEY(LUA下标从1开始)
---]]


--- 并发流量整形
--- @param key      唯一标识
--- @param permits  大于0表示加流量,小于0表示减流量
--- @param limit    获取限流大小参数
--- @return 0=表示取令牌失败(也就是桶里没有令牌),1=表示获取令牌成功
local function trafficConcurrent(key , permits, limit)
    -- 获取当前流量大小,没有则默认为0
    local currentConcurrent = tonumber(redis.call("GET", key) or "0")
    if permits > 0 then
        --- 加流量
        if currentConcurrent + 1 > limit then
            -- 达到限流大小,返回状态码和并发数
            return 0
        else
            redis.call("INCRBY", key, 1)
            return currentConcurrent + 1
        end
    elseif permits < 0 then
        --- 减流量
        if currentConcurrent - 1 <= 0 then
            return 0
        else
            redis.call("DECRBY", key, 1)
            return currentConcurrent - 1
        end
    end
end


--- 主流程
return trafficConcurrent(KEYS[1], (KEYS[2], KEYS[3])
