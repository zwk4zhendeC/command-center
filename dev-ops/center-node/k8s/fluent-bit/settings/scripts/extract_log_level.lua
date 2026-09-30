-- 基于消息内容提取日志级别
function level_by_content(tag, timestamp, record)
    if record["level"] ~= nil then return 1, timestamp, record end

    -- 定义各级别的关键字集合
    local error_set = { "fatal", "error", "panic", "fail", "exception","失败","错误"}
    local warn_set  = { "warn", "alert", "caution", "注意","警告"}
    local info_set  = { "info", "notice", "消息", "信息" }
    local debug_set = { "debug", "trace", "调试", "追踪"}
    local msg = record["_msg"]
    --  OFF > FATAL > ERROR > WARN > INFO > DEBUG > TRACE > ALL 
    record["level"] = "info"
    if msg == nil or type(msg) ~= "string" then
        local raw = record["log"] or record["message"] or ""
        print(string.format("[extract_log_level] skip level detection tag=%s msg=%s raw=%s", tostring(tag), tostring(msg), tostring(raw)))
        return 1, timestamp, record
    end
    local lmsg = string.lower(msg)
    -- 错误级别
    for _, kw in ipairs(error_set) do
        if string.find(lmsg, kw) then
            record["level"] = "error"
            return 1, timestamp, record
        end
    end
    -- 警告级别
    for _, kw in ipairs(warn_set) do
        if string.find(lmsg, kw) then
            record["level"] = "warn"
            return 1, timestamp, record
        end
    end
    -- debug级别
    for _, kw in ipairs(debug_set) do
        if string.find(lmsg, kw) then
            record["level"] = "debug"
            return 1, timestamp, record
        end
    end
    return 1, timestamp, record
end

-- 基于systemd的PRIORITY字段映射日志级别
function level_by_priority(tag, timestamp, record)
  local priority_map = {
      ["0"] = "error",   -- Emergency: system is unusable
      ["1"] = "error",   -- Alert: action must be taken immediately  
      ["2"] = "error",   -- Critical: critical conditions
      ["3"] = "error",   -- Error: error conditions
      ["4"] = "warn",    -- Warning: warning conditions
      ["5"] = "info",  -- Notice: normal but significant condition
      ["6"] = "info",    -- Informational: informational messages
      ["7"] = "debug"    -- Debug: debug-level messages
  }

    if record["PRIORITY"] then
        record["level"] = priority_map[record["PRIORITY"]] or "unknown"
    else
        record["level"] = "unknown"
    end
    
    return 1, timestamp, record
end