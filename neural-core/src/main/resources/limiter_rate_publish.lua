--- 发布令牌桶配置
--- @param key 令牌的唯一标识
--- @param max_permits 桶大小
--- @param rate  向桶里添加令牌的速率
--- @param apps  可以使用令牌桶的应用列表，应用之前用逗号分隔
local function tryPublish(key, max_permits, rate , apps)
    local redis_rate_limit_info = redis.pcall('HMGET' , key , 'last_mill_second' , 'curr_permits' , 'max_permits' , 'rate' , 'apps')
    local org_max_permits = tonumber(redis_rate_limit_info[3])
    local org_rate = redis_rate_limit_info[4]
    local org_apps = redis_rate_limit_info[5]
    if (org_max_permits == nil) or (apps ~= org_apps or rate ~= org_rate or max_permits ~= org_max_permits) then
        redis.pcall('HMSET' , key , 'max_permits' , max_permits , 'rate' , rate , 'curr_permits' , max_permits , 'apps' , apps)
    end
    return 1
end

--- 主流程
return tryPublish(KEYS[1] , KEYS[2] , KEYS[3] , KEYS[4])
