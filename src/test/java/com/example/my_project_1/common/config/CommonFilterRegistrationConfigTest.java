package com.example.my_project_1.common.config;

import com.example.my_project_1.common.logging.HttpLoggingFilter;
import com.example.my_project_1.common.logging.SecurityUserMdcFilter;
import com.example.my_project_1.common.logging.TraceIdFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;

class CommonFilterRegistrationConfigTest {

    private final CommonFilterRegistrationConfig config = new CommonFilterRegistrationConfig();

    @Test
    @DisplayName("TraceIdFilter 서블릿 자동 등록을 비활성화한다.")
    void traceIdFilterRegistration_isDisabled() {
        TraceIdFilter filter = new TraceIdFilter();

        FilterRegistrationBean<TraceIdFilter> registration =
                config.traceIdFilterRegistration(filter);

        assertThat(registration.isEnabled()).isFalse();
        assertThat(registration.getFilter()).isSameAs(filter);
    }

    @Test
    @DisplayName("SecurityUserMdcFilter 서블릿 자동 등록을 비활성화한다.")
    void securityUserMdcFilterRegistration_isDisabled() {
        SecurityUserMdcFilter filter = new SecurityUserMdcFilter();

        FilterRegistrationBean<SecurityUserMdcFilter> registration =
                config.securityUserMdcFilterRegistration(filter);

        assertThat(registration.isEnabled()).isFalse();
        assertThat(registration.getFilter()).isSameAs(filter);
    }

    @Test
    @DisplayName("HttpLoggingFilter 서블릿 자동 등록을 비활성화한다.")
    void httpLoggingFilterRegistration_isDisabled() {
        HttpLoggingFilter filter = new HttpLoggingFilter();

        FilterRegistrationBean<HttpLoggingFilter> registration =
                config.httpLoggingFilterRegistration(filter);

        assertThat(registration.isEnabled()).isFalse();
        assertThat(registration.getFilter()).isSameAs(filter);
    }
}
