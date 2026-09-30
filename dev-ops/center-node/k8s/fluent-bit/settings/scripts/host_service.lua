-- syslog字段映射脚本
-- 基于日志分类文档为syslog日志添加二进制应用相关字段

function map_service_fields(tag, timestamp, record)
    record["host_ip"] = os.getenv("HOST_IP") or "unknown"
    record["service_name"] = extractServiceName('-service',record['filename']);
    record["object_name"] = record["service_name"];
    return 1, timestamp, record
end

function map_audit_fields(tag, timestamp, record)
    record["host_ip"] = os.getenv("HOST_IP") or "unknown"
    record["service_name"] = extractServiceName('-audit',record['filename']);
    record["object_name"] = record["service_name"];
    return 1, timestamp, record
end


function extractServiceName(suffix,path)
    -- 1️⃣ 提取文件名
    local filename = string.match(path, "[^/]+$")
    -- 2️⃣ 找最后一个 -后缀
    local audit_pos = filename:match(".*()%"..suffix)

    -- 4️⃣ 截取关键字前的内容
    if audit_pos then
        return string.sub(filename, 1, audit_pos - 1)
    end
    -- 5️⃣ 没匹配到关键字，返回整个文件名
    return filename
end