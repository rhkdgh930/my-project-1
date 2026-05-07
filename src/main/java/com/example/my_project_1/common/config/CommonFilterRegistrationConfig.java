package com.example.my_project_1.common.config;

import com.example.my_project_1.common.logging.HttpLoggingFilter;
import com.example.my_project_1.common.logging.SecurityUserMdcFilter;
import com.example.my_project_1.common.logging.TraceIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonFilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration(TraceIdFilter filter) {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<SecurityUserMdcFilter> securityUserMdcFilterRegistration(SecurityUserMdcFilter filter) {
        FilterRegistrationBean<SecurityUserMdcFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<HttpLoggingFilter> httpLoggingFilterRegistration(HttpLoggingFilter filter) {
        FilterRegistrationBean<HttpLoggingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
