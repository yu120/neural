--[[
Idempotent Script v1.0
@author：lry(echo)
@since：2019-07-14
--]]


-- 幂等校验并记录
-- @param key              幂等KEY
-- @return                 0=表示成功
local function mightContain(key)
    local current_permit = tonumber(redis.call('GET', key) or 0)
    if (next_curr_permits >= 0) then
        redis.pcall('HSET', key, 'curr_permits', next_curr_permits)
        return {1, next_curr_permits}
    end
end


-- 主流程
return mightContain(KEYS[1])
