--[[
Rate Limiter Script v1.0
@author：lry(echo)
@since：2019-06-25
@link https://github.com/ZhuBaker/distributed-current-limiter/blob/master/src/main/resources/lua/rate_limiter.lua
@link https://github.com/herosea/priority-rate-limiter/blob/master/src/main/resources/rate_limiter.lua
--]]


-- 获取令牌
-- @param key              令牌的唯一标识
-- @param permits          请求令牌数量
-- @param curr_mill_second 当前毫秒数
-- @param app              使用令牌的应用标识
-- @return                 0=获取失败,1=获取成功 -1=没有令牌桶配置,0=表示取令牌失败(也就是桶里没有令牌),1=表示获取令牌成功
local function tryAcquireRate(key, permits, curr_mill_second, app)
    local redis_rate_limit_info = redis.pcall('HMGET', key, 'last_mill_second', 'curr_permits', 'max_permits', 'rate', 'apps')
    local last_mill_second = redis_rate_limit_info[1]
    local curr_permits = tonumber(redis_rate_limit_info[2])
    local max_permits = tonumber(redis_rate_limit_info[3])
    local rate = redis_rate_limit_info[4]
    local apps = redis_rate_limit_info[5]

    -- 标识没有配置令牌桶
    if type(apps) == 'boolean' or apps == nil or not contains(apps, app) then
        return {2, curr_permits}
    end

    local local_curr_permits = curr_permits

    -- 判断许可数
    local next_curr_permits = local_curr_permits - permits
    if (next_curr_permits >= 0) then
        -- 获取令牌成功
        redis.pcall('HSET', key, 'curr_permits', next_curr_permits)
        return {1, next_curr_permits}
    else
        -- 获取令牌失败
        redis.pcall('HSET', key, 'curr_permits', local_curr_permits)
        return {0, next_curr_permits}
    end
end


-- 主流程
return mightContain(KEYS[1], KEYS[2], KEYS[3], KEYS[4])
