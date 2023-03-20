--[[
刷新orderbook快照;

Keys:
    1. 本次的快照key
Values:
    1. 本次的sequenceId
    2. 本次的快照数据
]]--
-- 获取上次的的sequenceId
local key_lastSeqId = '_lastSeqId_'
local lastSeqId = redis.call('get', key_lastSeqId)
local key = KEYS[1]
local seqId = ARGV[1]
local data = ARGV[2]

-- 如果检查到seqId是最新的，则存入
if not lastSeqId or seqId > lastSeqId then
    -- 更新最新seqId
    redis.call('set', key_lastSeqId, seqId);
    -- 存入orderbook的json数据
    redis.call('set', key, data);
    -- 发送消息
    redis.call('publish', 'notification', '{"type":"orderbook","data":' .. data .. '}')
    return true;
end
return false;

