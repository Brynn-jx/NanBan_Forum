package com.NanBan.entity.enums;

public enum UserIntegralOperTypeEnum {
    REGISTER(1, "账号注册"),
    USER_DOWNLOAD_ATTACHMENT(2, "下载附件"),
    DOWNLOAD_ATTACHMENT(3, "附件被下载"),
    POST_COMMENT(4, "发布评论"),
    POST_ARTICLE(5, "发布文章"),
    ADMIN(6, "管理员操作"),
    DEL_ARTICLE(7, "文章被删除"),
    DEL_COMMENT(8, "评论被删除");

    private Integer opType;

    private String desc;

    UserIntegralOperTypeEnum(Integer opType, String desc) {
        this.opType = opType;
        this.desc = desc;
    }

    public static UserIntegralOperTypeEnum getByType(Integer opType) {
        for (UserIntegralOperTypeEnum typeEnum : UserIntegralOperTypeEnum.values()) {
            if (typeEnum.getOpType().equals(opType)) {
                return typeEnum;
            }
        }
        return null;
    }

    public Integer getOpType() {
        return opType;
    }

    public String getDesc() {
        return desc;
    }
}
