package com.codbid.telemetry.demo.telemetry;

import com.codbid.telemetry.demo.service.LoadPressureService;
import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoadPressureFilterConfig {

    @Bean
    public FilterRegistrationBean<Filter> loadPressureFilter(LoadPressureService loadPressureService) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();

        registration.setFilter((request, response, chain) -> {
            loadPressureService.requestStarted();
            try {
                chain.doFilter(request, response);
            } finally {
                loadPressureService.requestFinished();
            }
        });
        registration.setName("loadPressureFilter");
        registration.setOrder(Integer.MIN_VALUE);

        return registration;
    }
}
