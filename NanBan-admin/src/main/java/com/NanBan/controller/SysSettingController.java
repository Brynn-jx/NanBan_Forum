package com.NanBan.controller;

import com.NanBan.annotation.VerifyParam;
import com.NanBan.controller.base.ABaseController;
import com.NanBan.entity.config.AdminConfig;
import com.NanBan.entity.dto.*;
import com.NanBan.entity.vo.ResponseVO;
import com.NanBan.exception.BusinessException;
import com.NanBan.service.SysSettingService;
import com.NanBan.utils.JsonUtils;
import com.NanBan.utils.OKHttpUtils;
import com.NanBan.utils.StringTools;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/setting")
public class SysSettingController extends ABaseController {
    @Resource
    private SysSettingService sysSettingService;

    @Resource
    private AdminConfig adminConfig;

    @RequestMapping("/getSetting")
    public ResponseVO getSetting() {
        return getSuccessResponseVO(sysSettingService.refreshCache());
    }

    @RequestMapping("/saveSetting")
    public ResponseVO saveSetting(@VerifyParam SysSetting4AuditDto auditDto,
                                  @VerifyParam SysSetting4CommentDto commentDto,
                                  @VerifyParam SysSetting4PostDto postDto,
                                  @VerifyParam SysSetting4LikeDto likeDto,
                                  @VerifyParam SysSetting4RegisterDto registerDto,
                                  @VerifyParam SysSetting4EmailDto emailDto) {
        SysSettingDto sysSettingDto = new SysSettingDto();
        sysSettingDto.setAuditSetting(auditDto);
        sysSettingDto.setCommentSetting(commentDto);
        sysSettingDto.setPostSetting(postDto);
        sysSettingDto.setLikeSetting(likeDto);
        sysSettingDto.setRegisterSetting(registerDto);
        sysSettingDto.setEmailSetting(emailDto);
        sysSettingService.saveSetting(sysSettingDto);
        sendWebRequest();
        return getSuccessResponseVO(null);
    }

    /**
     * 请求访客端的内部接口
     */
    private void sendWebRequest() {
        String appKey = adminConfig.getInnerApiAppKey();
        String appSecret = adminConfig.getInnerApiAppSecret();
        Long timestamp = System.currentTimeMillis();
        String sign = StringTools.encodeMd5(appKey + timestamp + appSecret);
        String url = adminConfig.getWebApiUrl() + "?appKey=" + appKey + "&timestamp=" + timestamp + "&sign=" + sign;
        String responseJson = OKHttpUtils.getRequest(url);
        ResponseVO responseVO = JsonUtils.convertJson2Obj(responseJson, ResponseVO.class);
        if (!STATUC_SUCCESS.equals(responseVO.getStatus())) {
            throw new BusinessException("刷新访客端缓存失败");
        }
    }
}
