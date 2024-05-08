package com.NanBan.entity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.stereotype.Component;

@Component
public class AppConfig {
    @Value("${project.folder:}")
    private String projectFolder;

    @Value("${isDev:}")
    private Boolean isDev;

    @Value("${inner.api.appKey:}")
    private String innerApiAppKey;

    @Value("${inner.api.appSecret:}")
    private String innerApiAppSecret;

    public String getInnerApiAppKey() {
        return innerApiAppKey;
    }

    public String getInnerApiAppSecret() {
        return innerApiAppSecret;
    }

    public String getProjectFolder() {
        return projectFolder;
    }

    public Boolean getDev() {
        return isDev;
    }
}
