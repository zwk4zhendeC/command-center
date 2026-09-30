function all_end(tag, timestamp, record)
    -- host_ip: 宿主机IP（从环境变量获取）
    record["host_ip"] = os.getenv("HOST_IP") or "unknown"
    -- 时间字段标准化：_time重命名为time
    -- 业务标识，我们只采集gdzd的日志
    record["area_group"] = 'gdzd'
    -- 网络标识，只采集内网
    record["local_area"] = '1'
    return 1, timestamp, record
end
