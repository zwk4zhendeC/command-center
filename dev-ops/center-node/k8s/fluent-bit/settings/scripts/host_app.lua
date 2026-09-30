-- syslog字段映射脚本
-- 基于日志分类文档为syslog日志添加二进制应用相关字段

function map_app_fields(tag, timestamp, record)
    -- 获取主机名，优先从环境变量或系统，否则从tag中提取
    -- host_ip: 宿主机IP（从环境变量获取）
    record["host_ip"] = os.getenv("HOST_IP") or "unknown"
    -- 时间字段标准化：_time重命名为time
    record["_time"] = timestamp
    -- log_type: 日志类型，区分不同类型的日志
    record["log_type"] = "host_app"
    -- _msg: 日志内容
    record["_msg"] = record["_msg"] or ""
    return 1, timestamp, record
end
