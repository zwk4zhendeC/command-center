package cn.demo.asset.log;

public interface FluentBitHandler {
    /**
     * 将前端提供的配置转化为fluent-bit配置
     * @param bo
     * @param vo
     */
    void process(FluentBitBo bo, FluentBitVo vo);



}
