function k8s_fields_mapping(tag, timestamp, record)
    -- 早期返回：如果不是kubernetes日志，直接退出
    if not record["kubernetes"] then return -1, 0, 0 end
    -- labels字段不存在
    if not record["kubernetes"]["labels"] then return -1, 0,0 end
    
    local k8s = record["kubernetes"]
    local labels = k8s["labels"]

    -- 通用字段标准化（所有日志类型都需要）
    -- 时间字段标准化：使用timestamp参数
    record["_time"] = timestamp
    -- log_type: 日志类型，区分不同类型的日志
    record["log_type"] = "k8s_pod"
    -- node_ip: 宿主机IP（从环境变量获取）
    record["host_ip"] = os.getenv("HOST_IP") or "unknown"
    -- host_name: 使用kubernetes.node_name而不是宿主机主机名（为了与指标统一）
    record["host_name"] = k8s["host"] or "unknown"
    -- app_name: 应用名称（取自客户提供的app标签）
    record["app_name"] = labels["app"] or "unknown"
    -- asset_type: 日志对象的类型（使用app标签）
    record["asset_type"] = labels["asset_type"] or "unknown"

    -- K8S专有字段
    -- namespace: 命名空间
    record["namespace"] = k8s["namespace_name"] or "unknown"
    -- pod_name: Pod名称
    record["pod_name"] = k8s["pod_name"] or "unknown"
    -- container_name: 容器名称
    record["container_name"] = k8s["container_name"] or "unknown"

    -- 从labels中提取字段
    -- service_name: 服务名称（取自客户提供的serviceName标签）
    record["service_name"] = labels["serviceName"] or "unknown"
    -- language: 语言标签（取自客户提供的language标签）
    record["language"] = labels["language"]
    -- service_type: 服务类型（取自客户提供的type标签）
    -- record["service_type"] = labels["asset_type"]
    record["object_name"] = labels["serviceName"] or "unknown"

    -- level: 日志级别（基于日志内容提取）
    -- record["level"] = "info"
    
    -- log_id: 不同类型日志该字段含义不同（k8s.{namespace}.{host_name}.{pod_name}.{container_name}）
    local namespace = record["namespace"] or "unknown"
    local host_name = record["host_name"] or "unknown"
    local pod_name = record["pod_name"] or "unknown"
    local container_name = record["container_name"] or "unknown"
    record["log_id"] = "k8s." .. namespace .. "." .. host_name .. "." .. pod_name .. "." .. container_name
    
    return 1, timestamp, record
end