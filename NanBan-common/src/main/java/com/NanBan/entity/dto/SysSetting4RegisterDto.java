package com.NanBan.entity.dto;


import com.NanBan.annotation.VerifyParam;

/**
 * 注册设置
 */
public class SysSetting4RegisterDto {
    // 注册欢迎语
    @VerifyParam(required = true)
    private String registerWelcomInfo;

    public String getRegisterWelcomInfo() {
        return registerWelcomInfo;
    }

    public void setRegisterWelcomInfo(String registerWelcomInfo) {
        this.registerWelcomInfo = registerWelcomInfo;
    }
}
