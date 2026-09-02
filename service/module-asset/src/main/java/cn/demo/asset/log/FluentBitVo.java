package cn.demo.asset.log;

import cn.demo.asset.entity.vo.AssetVo;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FluentBitVo {
    private String type;
    private AssetVo asset;
    /**
     *
     */
    private List<Map<String, String>> args;
}
