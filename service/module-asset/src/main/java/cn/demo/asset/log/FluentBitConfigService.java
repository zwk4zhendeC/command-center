package cn.demo.asset.log;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class FluentBitConfigService {
    //    YAMLMapper mapper = new YAMLMapper();
    Yaml yaml = new Yaml();

    @Resource
    Map<String, FluentBitHandler> handlerMap = new HashMap<>();

    public void addAppLog() {

    }

    /**
     * 基于现有yaml，插入一个输入
     * @param yamlText
     * @param vo
     * @return
     */
    public FluentBitBo saveInput(String yamlText, FluentBitVo vo) {
        FluentBitBo bitBo = yaml.loadAs(yamlText, FluentBitBo.class);
        FluentBitHandler bitHandler = handlerMap.get(vo.getType());
        bitHandler.process(bitBo, vo);
        return bitBo;
    }
}
