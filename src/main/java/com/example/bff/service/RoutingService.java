package com.example.bff.service;

import com.example.bff.config.AppConfig;
import com.example.bff.config.RouteConfig;
import org.springframework.stereotype.Service;

@Service
public class RoutingService {

    private final AppConfig appConfig;

    public RoutingService(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public String determineBackendUrl(String requestUri) {

        // Iterate over the list of routes in the config
        for (RouteConfig routeConfig : appConfig.getRoutes()) {
            if (requestUri.startsWith(routeConfig.getPath())) {
                return routeConfig.getUrl(); // Return the corresponding URL for the route
            }
        }
        return null; // Return null if no match is found
    }
}
