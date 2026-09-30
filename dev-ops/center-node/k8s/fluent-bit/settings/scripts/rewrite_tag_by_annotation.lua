function rewrite_tag_by_annotation(tag, timestamp, record)
    -- 检查是否有kubernetes信息和annotations
    if record["kubernetes"] and record["kubernetes"]["annotations"] then
        local annotations = record["kubernetes"]["annotations"]
        local log_language = annotations["log-language"]
        
        if log_language then
            -- 直接用注解值拼接新tag
            local new_tag = string.gsub(tag, "^kube%.", "kube." .. log_language .. ".")
            
            -- 记录tag重写信息用于调试
            print("Tag rewritten from " .. tag .. " to " .. new_tag .. " based on log-language=" .. log_language)
            
            -- 返回新的tag和记录
            return 2, timestamp, record, new_tag
        end
    end
    
    -- 如果没有匹配的注解，保持原样
    return 1, timestamp, record
end