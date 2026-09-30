function map_systemd_fields(tag, timestamp, record)
    -- 创建新的record
    local new_record = record
    
    -- 公共字段配置
    -- host_ip: 宿主机IP（从环境变量获取）
    new_record["host_ip"] = os.getenv("HOST_IP") or "unknown"
    -- 时间字段标准化：_time重命名为time
    new_record["_time"] = timestamp
    -- log_type: 日志类型，区分不同类型的日志
    new_record["log_type"] = "systemd"
    -- _msg: 日志内容
    new_record["_msg"] = record["_msg"] or ""
    -- host_name: 主机名称（取自宿主机的主机名）
    new_record["host_name"] = record["_HOSTNAME"] or "unknown"
    
    -- app_name: 应用名称（优先取进程的简短名称SYSLOG_IDENTIFIER，没有则取_COMM）
    new_record["app_name"] = record["SYSLOG_IDENTIFIER"] or record["_COMM"] or "unknown"
    
    -- -- asset_type: 日志对象的类型（取自syslog标识符SYSLOG_IDENTIFIER）
    -- new_record["asset_type"] = new_record["app_name"]
    
    -- SystemD专有字段
    -- service_name: 服务名称（优先取进程的简短名称SYSLOG_IDENTIFIER，没有提供则取进程名_COMM）
    new_record["service_name"] = new_record["app_name"]
    
    -- pid: 进程ID（优先基于服务上报的SYSLOG_PID，没有则取journal采集的_PID）
    new_record["pid"] = record["SYSLOG_PID"] or record["_PID"] or "unknown"
    
    -- _EXE: 可执行文件路径
    new_record["_EXE"] = record["_EXE"] or "unknown"
    -- _COMM: 进程名称
    new_record["_COMM"] = record["_COMM"] or "unknown"
    -- _CMDLINE: 完整的命令行参数
    new_record["_CMDLINE"] = record["_CMDLINE"] or "unknown"
    -- PRIORITY: 日志优先级数值
    new_record["PRIORITY"] = record["PRIORITY"] or 6
    -- _GID: 用户组ID
    new_record["_GID"] = record["_GID"]
    -- _SOURCE_REALTIME_TIMESTAMP: 日志的原始时间戳
    new_record["_SOURCE_REALTIME_TIMESTAMP"] = record["_SOURCE_REALTIME_TIMESTAMP"]
    -- SYSLOG_FACILITY: 日志来源分类
    new_record["SYSLOG_FACILITY"] = record["SYSLOG_FACILITY"]

    -- level: 日志级别（PRIORITY数值转换为文本级别）
    local priority = tonumber(new_record["PRIORITY"])
    if priority then
        if priority <= 3 then
            new_record["level"] = "error"
        elseif priority == 4 then
            new_record["level"] = "warn"
        elseif priority <= 6 then
            new_record["level"] = "info"
        else
            new_record["level"] = "debug"
        end
    end
    
    -- log_id: 不同类型日志该字段含义不同（systemd.{host_name}.{service_name}.{pid}）
    local host_name = new_record["host_name"] or "unknown"
    local service_name = new_record["service_name"] or "unknown"
    local pid = new_record["pid"] or "unknown"
    new_record["log_id"] = "systemd." .. host_name .. "." .. service_name .. "." .. pid
    return 1, timestamp, new_record
end