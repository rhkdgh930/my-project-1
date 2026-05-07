package com.example.my_project_1.auth.config;

import com.example.my_project_1.auth.filter.JwtAuthenticationFilter;
import com.example.my_project_1.auth.filter.JwtLoginFilter;
import com.example.my_project_1.auth.handler.*;
import com.example.my_project_1.auth.oauth.CustomOAuth2UserService;
import com.example.my_project_1.auth.provider.CustomAuthenticationProvider;
import com.example.my_project_1.auth.service.RedisLoginAttemptService;
import com.example.my_project_1.auth.utils.EndpointAuthorizationRules;
import com.example.my_project_1.common.logging.HttpLoggingFilter;
import com.example.my_project_1.common.logging.SecurityUserMdcFilter;
import com.example.my_project_1.common.logging.TraceIdFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    private final JwtLoginSuccessHandler jwtLoginSuccessHandler;
    private final JwtLoginFailureHandler jwtLoginFailureHandler;
    private final RedisLoginAttemptService redisLoginAttemptService;

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final CustomOAuth2UserService customOAuth2UserService;

    private final TraceIdFilter traceIdFilter;
    private final HttpLoggingFilter httpLoggingFilter;
    private final SecurityUserMdcFilter securityUserMdcFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, EndpointAuthorizationRules.PUBLIC_GET_ONLY).permitAll()
                        .requestMatchers(EndpointAuthorizationRules.PUBLIC_ANY_METHOD).permitAll()
                        .requestMatchers(EndpointAuthorizationRules.ADMIN_ONLY).hasRole("ADMIN")
                        .requestMatchers(EndpointAuthorizationRules.USER_ONLY).authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                )

                .addFilterBefore(traceIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, TraceIdFilter.class)
                .addFilterAfter(securityUserMdcFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(httpLoggingFilter, SecurityUserMdcFilter.class)
                .addFilterAt(jwtLoginFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:5173",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:3001",
                "http://127.0.0.1:5173"
        ));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Refresh-Token"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
    @Bean
    public JwtLoginFilter jwtLoginFilter() throws Exception {
        return new JwtLoginFilter(authenticationManager(), jwtLoginSuccessHandler, jwtLoginFailureHandler, redisLoginAttemptService);
    }

    @Bean
    public CustomAuthenticationProvider customAuthenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        return new CustomAuthenticationProvider(userDetailsService, passwordEncoder);
    }

    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

}
