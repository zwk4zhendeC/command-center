package cn.demo.asset.log;

import cn.demo.asset.entity.vo.AssetVo;
import ognl.IntHashMap;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component("file-handler")
public class FileHandler implements FluentBitHandler {

    public static final String KEY = "tail";

    /**
     * @param bo
     * @param vo
     */
    public void process(FluentBitBo bo, FluentBitVo vo) {
        List<Map<String, String>> args = vo.getArgs();
        AssetVo asset = vo.getAsset();

        for (Map<String, String> arg : args) {
            String path = arg.get("path");
            if (path == null) {
                throw new RuntimeException("");
            }
            String tag = asset.getAssetName() + "_" + asset.getId() + "_" + path;
            if (bo.getPipeline().getInputs().containsKey(tag)) {
                continue;
            }
            Map<String, String> input = new LinkedHashMap<>();
            input.put("name", KEY);
            input.put("path", path);  
        }
    }
}
