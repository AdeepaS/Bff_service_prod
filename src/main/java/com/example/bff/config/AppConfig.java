package com.example.bff.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "config")
@Getter
@Setter
public class AppConfig {
    // Dynamic routes
    private List<RouteConfig> routes;
}
