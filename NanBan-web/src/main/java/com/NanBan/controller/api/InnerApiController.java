package com.NanBan.controller.api;

import com.NanBan.annotation.GlobalInterceptor;
import com.NanBan.annotation.VerifyParam;
import com.NanBan.controller.base.ABaseController;
import com.NanBan.entity.config.WebConfig;
import com.NanBan.entity.enums.ResponseCodeEnum;
import com.NanBan.entity.vo.ResponseVO;
import com.NanBan.exception.BusinessException;
import com.NanBan.service.SysSettingService;
import com.NanBan.utils.StringTools;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController("innerApiController")
@RequestMapping("/innerApi")
public class InnerApiController extends ABaseController{

    @Resource
    private WebConfig webConfig;

    @Resource
    private SysSettingService sysSettingService;

    @RequestMapping("/refresSysSetting")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO refresSysSetting(@VerifyParam(required = true) String appKey,
                                       @VerifyParam(required = true) Long timestamp,
                                       @VerifyParam(required = true) String sign){
        if(!webConfig.getInnerApiAppKey().equals(appKey)){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        if(System.currentTimeMillis() - timestamp > 1000 * 10){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        String mySign = StringTools.encodeMd5(appKey + timestamp + webConfig.getInnerApiAppSecret());
        if(!mySign.equals(sign)){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        // 将修改后的系统设置写入CACHE_DATA
        sysSettingService.refreshCache();
        return getSuccessResponseVO(null);
    }
}
