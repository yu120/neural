--[[================================================================================================
基于Redis+LUA实现分布式限流控制
作者：李茹钰(echo)
时间：2017-10-17
====================================================================================================
数据量预估：50个应用 X 每个应用20个限流资源 X

1.限流数据结构设计：(预计：50 X 20 X 3 = 0.3W条记录)
a.限流配置信息(HashMap)：Hash<limiter:config:${appName}:${资源ID}, Map<Key, Value>>
b.限流并发计数器(INCR)：String<limiter:concurrent:${appName}:${资源ID}, Number>
c.限流速率计数器(INCR)：String<limiter:rate:${appName}:${资源ID}, Number>

2.运维数据结构设计：(预计：50 X 20 X 3 X (60分钟+24小时+30天) = 34.2W条记录,收纳最近：60分/24小时/30天的记录)
Time = MINUTE(yyyyMMddHHmm)、HOUR(yyyyMMddHH)、DAY(yyyyMMdd)
a.统计并发计数器(INCR)：String<limiter:statistics:${MINUTE/HOUR/DAY}:concurrent:${appName}:${资源ID}:${Time}, Number>
b.统计速率计数器(INCR)：String<limiter:statistics:rate:${appName}:${资源ID}:${Time}, Number>
c.统计耗时计数器(INCR)：String<limiter:statistics:elapsed:${appName}:${资源ID}:${Time}, Number>
====================================================================================================
测试命令：redis-cli --eval limiter.lua , biz_order cn.ms.biz.order.IOrderService.queryOrder true
--]]

-- 第一步：常数定义与参数获取
-- 时间精度（毫秒级,1秒内的粒度控制建议使用并发量控制）
local PEXPIRE_UNIT = {MILLISECOND=1, SECOND=1000, MINUTE=60000, HOUR=3600000, DAY=86400000}
-- 获取参数值（应用NAME、资源ID、调试日志开关）
local appName, limiterId, logEnable = ARGV[1], ARGV[2], ARGV[3]

-- 第二步：读取配置、解析配置
local configKey = string.format('limiter:config:%s:%s', appName, limiterId)
-- 读取当前资源相关配置信息：源数据结构为[key1, value1, key2, value2, ……]
local configArray = redis.call('HGETALL', configKey)
if table.getn(configArray) == 0 then -- 没有发现限流配置
  return {'NOTFOUND'}
end
-- 解读配置
local configMap = {} -- 目标数据结构为{key1=value1, key2=value2, ……}
for index = 1, table.getn(configArray), 2 do
  configMap[configArray[index]] = configArray[index+1]
end
local enable = configMap['enable'] -- 限流资源开关
local time = configMap['time'] -- 限流粒度时间
local timeUnit  = configMap['timeUnit'] -- 限流粒度时间单位
local strategy = configMap['strategy'] -- 超额策略
local maxConcurrent = tonumber(configMap['maxConcurrent']) -- 最大并发限制
local maxRate = tonumber(configMap['maxRate']) -- 最大速率限制

-- 第三步：限流配置校验
if enable == nil or time == nil or timeUnit == nil or strategy == nil then
  return {'DEFECT'} -- 配置缺失
end
if 'true' ~= enable then
  return {'DISABLE'} -- 校验限流资源开关：已禁用
end

-- 第四步：并发控制（并发不需要过期）：TODO 如何处理并发未释放的数据？
local concurrentKey = string.format('limiter:concurrent:%s:%s', appName, limiterId)
if maxConcurrent ~= nil then -- 若并发参数设置,则校验
  local concurrentNum = tonumber(redis.call('GET', concurrentKey))
  if concurrentNum == nil or maxConcurrent > concurrentNum then -- KEY不存在或并发未超额
    redis.call('INCR', concurrentKey) -- 并发+1
  else -- 并发已超额
    return {'CONCURRENT_FULL', strategy} -- 返回并发超额后的策略
  end
end

-- 第五步：速率控制（分布式速率控制不需要考虑时间窗滑动问题,即不需要考虑令牌桶方式）
local rateKey = string.format('limiter:rate:%s:%s', appName, limiterId)
if maxRate ~= nil then -- 若速率参数设置,则校验
  local rateNum = tonumber(redis.call('GET', rateKey))
  if rateNum == nil then  -- KEY不存在：设置速率为1,且设置其过期时间
    redis.call('INCR', rateKey)
    redis.call('PEXPIRE', rateKey, PEXPIRE_UNIT[timeUnit] * time)
  elseif maxRate > rateNum then -- 速率未超额
    redis.call('INCR', rateKey) -- 速率+1
  else -- 速率已超额
    return {'RATE_FULL', strategy} -- 返回速率超额后的策略
  end
end

-- 第六步：调试日志
if logEnable then
  local concurrent = redis.call('GET', concurrentKey)
  local rate = redis.call('GET', rateKey)
  local pttl = redis.call('PTTL', rateKey)
  print(string.format('Limiter[id=%s, concurrent=%s, rate=%s, pttl=%s ms]', configKey, concurrent, rate, pttl))
end

-- 第七步：流量校验通过
return {'PASS'}
