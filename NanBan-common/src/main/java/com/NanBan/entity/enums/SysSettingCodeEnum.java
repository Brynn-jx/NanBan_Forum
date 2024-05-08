package com.NanBan.entity.enums;

public enum SysSettingCodeEnum {
    AUDIT("audit", "com.NanBan.entity.dto.SysSetting4AuditDto", "auditSetting", "审核设置"),
    COMMENT("comment", "com.NanBan.entity.dto.SysSetting4CommentDto", "commentSetting", "评论设置"),
    POST("post", "com.NanBan.entity.dto.SysSetting4PostDto", "postSetting", "帖子设置"),
    LIKE("like", "com.NanBan.entity.dto.SysSetting4LikeDto", "likeSetting", "点赞设置"),
    REGISTER("register", "com.NanBan.entity.dto.SysSetting4RegisterDto", "registerSetting", "注册设置"),
    EMAIL("email", "com.NanBan.entity.dto.SysSetting4EmailDto", "emailSetting", "邮件设置"),
    ;

    public static SysSettingCodeEnum getByCode(String code){
        for(SysSettingCodeEnum item:SysSettingCodeEnum.values()){
            if(item.getCode().equals(code)){
                return item;
            }
        }
        return null;
    }
    private String code;
    private String clazz;
    private String propName;
    private String desc;

    SysSettingCodeEnum(String code, String clazz, String propName, String desc){
        this.code = code;
        this.clazz = clazz;
        this.propName = propName;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getClazz() {
        return clazz;
    }

    public String getPropName() {
        return propName;
    }

    public String getDesc() {
        return desc;
    }
}
