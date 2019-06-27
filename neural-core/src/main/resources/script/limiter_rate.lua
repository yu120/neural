--[[
Rate Limiter Script v1.0
@author：lry(echo)
@since：2019-06-25
@link https://github.com/ZhuBaker/distributed-current-limiter/blob/master/src/main/resources/lua/rate_limiter.lua
@link https://github.com/herosea/priority-rate-limiter/blob/master/src/main/resources/rate_limiter.lua
--]]


-- 判断source_str中是否包含sub_str
-- @param source_str
-- @param sub_str
-- @return false=不包含,true=包含
local function contains(source_str, sub_str)
    local start_pos,end_pos = string.find(source_str, sub_str)
    if start_pos == nil then
        return false
    end
    local source_str_len = string.len(source_str)
    local s = string.sub(source_str, end_pos, end_pos)
    if source_str_len == end_pos then
        return true
    elseif string.sub(source_str, end_pos + 1, end_pos + 1) == ',' then
        return true
    end
    return false
end


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

    -- 令牌桶刚刚创建，上一次获取令牌的毫秒数为空
    -- 根据和上一次向桶里添加令牌的时间和当前时间差，触发式往桶里添加令牌，并且更新上一次向桶里添加令牌的时间
    -- 如果向桶里添加的令牌数不足一个，则不更新上一次向桶里添加令牌的时间
    if (type(last_mill_second) ~= 'boolean' and last_mill_second ~= nil) then
        local reverse_permits = math.floor(((curr_mill_second - last_mill_second)/1000) * rate)
        local expect_curr_permits = reverse_permits + curr_permits;
        local_curr_permits = math.min(expect_curr_permits, max_permits)
        -- 大于0表示不是第一次获取令牌，也没有向桶里添加令牌
        if (reverse_permits > 0) then
            redis.pcall('HSET', key, 'last_mill_second', curr_mill_second)
        end
    else
        redis.pcall('HSET', key, 'last_mill_second', curr_mill_second)
    end

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


-- 发布令牌桶配置
-- @param key          令牌的唯一标识
-- @param max_permits  桶大小
-- @param rate         向桶里添加令牌的速率
-- @param apps         可以使用令牌桶的应用列表，应用之前用逗号分隔
local function tryPublish(key, max_permits, rate, apps)
    local redis_rate_limit_info = redis.pcall('HMGET', key, 'last_mill_second', 'curr_permits', 'max_permits', 'rate', 'apps')
    local org_max_permits = tonumber(redis_rate_limit_info[3])
    local org_rate = redis_rate_limit_info[4]
    local org_apps = redis_rate_limit_info[5]
    if (org_max_permits == nil) or (apps ~= org_apps or rate ~= org_rate or max_permits ~= org_max_permits) then
        redis.pcall('HMSET', key, 'max_permits', max_permits, 'rate', rate, 'curr_permits', max_permits, 'apps', apps)
    end
    return 1
end


-- 主流程
return tryAcquireRate(KEYS[1], KEYS[2], KEYS[3], KEYS[4])
