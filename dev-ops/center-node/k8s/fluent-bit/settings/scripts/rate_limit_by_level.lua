-- 日志级别限流脚本
local rate_limiters = {}
local window_size = 60  -- 时间窗口大小（秒）

-- 每个级别的限流配置（每分钟最大条数）
local rate_limits = {
    ["error"] = 100,    -- error级别：每分钟最多100条
    ["warn"] = 200,     -- warn级别：每分钟最多200条
    ["info"] = 500,     -- info级别：每分钟最多500条
    ["debug"] = 1000,   -- debug级别：每分钟最多1000条
    ["unknown"] = 50    -- 未知级别：每分钟最多50条
}

function log_level_rate_limit(tag, timestamp, record)
    local current_time = os.time()
    local level = record["level"] or "unknown"
    
    -- 初始化该级别的限流器
    if not rate_limiters[level] then
        rate_limiters[level] = {
            count = 0,
            window_start = current_time,
            dropped_count = 0
        }
    end
    
    local limiter = rate_limiters[level]
    local limit = rate_limits[level] or rate_limits["unknown"]
    
    -- 检查是否需要重置时间窗口
    if current_time - limiter.window_start >= window_size then
        -- 如果有丢弃的日志，记录统计信息
        if limiter.dropped_count > 0 then
            print(string.format("[RATE_LIMIT] Level: %s, Window: %d-%d, Allowed: %d, Dropped: %d", 
                  level, limiter.window_start, current_time, limiter.count, limiter.dropped_count))
        end
        
        -- 重置计数器
        limiter.count = 0
        limiter.dropped_count = 0
        limiter.window_start = current_time
    end
    
    -- 检查是否超过限制
    if limiter.count >= limit then
        limiter.dropped_count = limiter.dropped_count + 1
        
        -- 返回0表示丢弃该记录
        return 0, timestamp, record
    end
    
    -- 增加计数器
    limiter.count = limiter.count + 1
    
    -- 添加限流统计信息到记录中
    record["rate_limit_count"] = limiter.count
    record["rate_limit_max"] = limit
    
    return 1, timestamp, record
end