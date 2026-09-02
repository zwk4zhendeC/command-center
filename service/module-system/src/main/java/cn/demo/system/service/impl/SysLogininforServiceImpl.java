package cn.demo.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.demo.system.domain.SysLogininfor;
import cn.demo.system.mapper.SysLogininforMapper;
import cn.demo.system.service.ISysLogininforService;

/**
 * 系统访问日志情况信息 服务层处理
 *
 * @author ruoyi
 */
@Service
public class SysLogininforServiceImpl implements ISysLogininforService
{

    @Autowired
    private SysLogininforMapper logininforMapper;

    /**
     * 新增系统登录日志
     *
     * @param logininfor 访问日志对象
     */
    @Override
    public void insertLogininfor(SysLogininfor logininfor)
    {
        logininfor.setUserName(limit(logininfor.getUserName(), 50));
        logininfor.setIpaddr(limit(logininfor.getIpaddr(), 128));
        logininfor.setLoginLocation(limit(logininfor.getLoginLocation(), 255));
        logininfor.setBrowser(limit(logininfor.getBrowser(), 50));
        logininfor.setOs(limit(logininfor.getOs(), 50));
        logininfor.setMsg(limit(logininfor.getMsg(), 255));
        logininforMapper.insertLogininfor(logininfor);
    }

    private String limit(String value, int maxLength)
    {
        if (value == null || value.length() <= maxLength)
        {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /**
     * 查询系统登录日志集合
     *
     * @param logininfor 访问日志对象
     * @return 登录记录集合
     */
    @Override
    public List<SysLogininfor> selectLogininforList(SysLogininfor logininfor)
    {
        return logininforMapper.selectLogininforList(logininfor);
    }

    /**
     * 批量删除系统登录日志
     *
     * @param infoIds 需要删除的登录日志ID
     * @return 结果
     */
    @Override
    public int deleteLogininforByIds(Long[] infoIds)
    {
        return logininforMapper.deleteLogininforByIds(infoIds);
    }

    /**
     * 清空系统登录日志
     */
    @Override
    public void cleanLogininfor()
    {
        logininforMapper.cleanLogininfor();
    }
}
