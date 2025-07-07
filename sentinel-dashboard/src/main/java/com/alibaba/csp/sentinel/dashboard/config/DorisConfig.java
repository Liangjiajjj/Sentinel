package com.alibaba.csp.sentinel.dashboard.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DorisProperties.class)
public class DorisConfig {

    private final DorisProperties dorisProperties;

    public DorisConfig(DorisProperties dorisProperties) {
        this.dorisProperties = dorisProperties;
    }

    // todo: new a jdbc client

}
