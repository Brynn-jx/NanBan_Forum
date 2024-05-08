package com.NanBan.entity.dto;

import com.NanBan.annotation.VerifyParam;

public class SysSetting4LikeDto {

    @VerifyParam(required = true)
    private  Integer likeDayCountThreshold;

    public Integer getLikeDayCountThreshold() {
        return likeDayCountThreshold;
    }

    public void setLikeDayCountThreshold(Integer likeDayCountThreshold) {
        this.likeDayCountThreshold = likeDayCountThreshold;
    }
}
