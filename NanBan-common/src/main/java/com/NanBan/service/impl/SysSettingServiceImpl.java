package com.NanBan.service.impl;

import com.NanBan.entity.dto.SysSettingDto;
import com.NanBan.entity.enums.PageSize;
import com.NanBan.entity.enums.SysSettingCodeEnum;
import com.NanBan.entity.po.SysSetting;
import com.NanBan.entity.query.SimplePage;
import com.NanBan.entity.query.SysSettingQuery;
import com.NanBan.entity.vo.PaginationResultVO;
import com.NanBan.exception.BusinessException;
import com.NanBan.mappers.SysSettingMapper;
import com.NanBan.service.SysSettingService;
import com.NanBan.utils.JsonUtils;
import com.NanBan.utils.StringTools;
import com.NanBan.utils.SysCacheUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.List;


/**
 * 系统设置信息 业务接口实现
 */
@Service("sysSettingService")
public class SysSettingServiceImpl implements SysSettingService {

    private static final Logger logger = LoggerFactory.getLogger(SysSettingServiceImpl.class);
    @Resource
    private SysSettingMapper<SysSetting, SysSettingQuery> sysSettingMapper;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<SysSetting> findListByParam(SysSettingQuery param) {
        return this.sysSettingMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(SysSettingQuery param) {
        return this.sysSettingMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<SysSetting> findListByPage(SysSettingQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<SysSetting> list = this.findListByParam(param);
        PaginationResultVO<SysSetting> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(SysSetting bean) {
        return this.sysSettingMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<SysSetting> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.sysSettingMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<SysSetting> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.sysSettingMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 多条件更新
     */
    @Override
    public Integer updateByParam(SysSetting bean, SysSettingQuery param) {
        StringTools.checkParam(param);
        return this.sysSettingMapper.updateByParam(bean, param);
    }

    /**
     * 多条件删除
     */
    @Override
    public Integer deleteByParam(SysSettingQuery param) {
        StringTools.checkParam(param);
        return this.sysSettingMapper.deleteByParam(param);
    }

    /**
     * 根据Code获取对象
     */
    @Override
    public SysSetting getSysSettingByCode(String code) {
        return this.sysSettingMapper.selectByCode(code);
    }

    /**
     * 根据Code修改
     */
    @Override
    public Integer updateSysSettingByCode(SysSetting bean, String code) {
        return this.sysSettingMapper.updateByCode(bean, code);
    }

    /**
     * 根据Code删除
     */
    @Override
    public Integer deleteSysSettingByCode(String code) {
        return this.sysSettingMapper.deleteByCode(code);
    }

    /**
     * 刷新缓存
     * 在一进入程序的时候会执行InitRun中的run方法
     */
    @Override
    public SysSettingDto refreshCache() {
        try {
            SysSettingDto sysSettingDto = new SysSettingDto();
            List<SysSetting> list = this.sysSettingMapper.selectList(new SysSettingQuery());
            for (SysSetting sysSetting : list) {
                String jsonContent = sysSetting.getJsonContent();
                if (StringTools.isEmpty(jsonContent)) {
                    continue;
                }
                String code = sysSetting.getCode();
                SysSettingCodeEnum sysSettingCodeEnum = SysSettingCodeEnum.getByCode(code);
                // 反射， 反射的实现机制是记录了类的信息，这里记录的就是SysSettingDto类的信息，
                // 这些信息包括他父类的信息，他的成员变量，成员函数的信息
                // 记录了信息之后就可以通过反射机制获得SysSettingDto其中的信息，这里获得的是他成员变量的信息，例如auditSetting
                PropertyDescriptor pd = new PropertyDescriptor(sysSettingCodeEnum.getPropName(), SysSettingDto.class);

                // 获取写方法， 例如auditSetting的写方法，就相当于set方法
                Method method = pd.getWriteMethod();

                // 通过完整路径创建一个对象
                Class subClazz = Class.forName(sysSettingCodeEnum.getClazz());
                ;
                // 调用包装在Method中的方法，例如auditSetting的写方法， 第一个参数是实例化的对象，第二个参数是要传入的参数
                method.invoke(sysSettingDto, JsonUtils.convertJson2Obj(jsonContent, subClazz));
            }
            // 将其保存
            SysCacheUtils.refresh(sysSettingDto);
            return sysSettingDto;
        } catch (Exception e) {
            logger.error("刷新缓存失败", e);
            throw new BusinessException("刷新缓存失败");
        }
    }

    /**
     * 将系统设置保存到数据库
     * @param sysSettingDto
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSetting(SysSettingDto sysSettingDto) {
        try {
            Class clazz = SysSettingDto.class;
            for (SysSettingCodeEnum codeEnum : SysSettingCodeEnum.values()) {
                PropertyDescriptor pd = new PropertyDescriptor(codeEnum.getPropName(), clazz);
                Method method = pd.getReadMethod();
                Object obj = method.invoke(sysSettingDto);
                SysSetting sysSetting = new SysSetting();
                sysSetting.setCode(codeEnum.getCode());
                sysSetting.setJsonContent(JsonUtils.convertObj2Json(obj));
                this.sysSettingMapper.insertOrUpdate(sysSetting);
            }
        } catch (Exception e) {
            logger.error("保存设置失败", e);
            throw new BusinessException("保存设置失败");
        }
    }
}