package com.example.my_project_1.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        Components components = new Components()
                .addSecuritySchemes("jwtAuth", new SecurityScheme()
                        .name("Authorization")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .addSecuritySchemes("refreshTokenCookie", new SecurityScheme()
                        .name("refreshToken")
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE))
                .addSecuritySchemes("refreshTokenHeader", new SecurityScheme()
                        .name("Refresh-Token")
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER));

        return new OpenAPI()
                .info(new Info()
                        .title("My Project API")
                        .description("Spring Boot backend API documentation for frontend integration.")
                        .version("v1.0.0"))
                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("Local development server"))
                .components(components);
    }

    @Bean
    public OpenApiCustomizer authLoginOpenApiCustomizer() {
        return openApi -> openApi.path("/api/auth/login", new PathItem()
                .post(new Operation()
                        .tags(List.of("Auth API"))
                        .summary("로그인")
                        .description("""
                                JwtLoginFilter가 처리하는 filter 기반 로그인 API입니다.
                                성공 시 TokenResponse를 JSON body로 반환하고 refreshToken cookie를 내려줍니다.
                                이 endpoint는 Controller method가 아니므로 OpenAPI에 수동 등록합니다.
                                """)
                        .requestBody(jsonRequestBody("#/components/schemas/LoginRequest"))
                        .responses(new ApiResponses()
                                .addApiResponse("200", jsonResponse(
                                        "로그인 성공. refreshToken cookie도 함께 내려갑니다.",
                                        "#/components/schemas/TokenResponse"
                                ).addHeaderObject("Set-Cookie", new Header()
                                        .description("refreshToken cookie")
                                        .schema(new StringSchema())))
                                .addApiResponse("400", jsonResponse(
                                        "요청 validation 실패",
                                        "#/components/schemas/ValidExceptionResponse"
                                ))
                                .addApiResponse("401", jsonResponse(
                                        "이메일/비밀번호 불일치, 탈퇴 완료 계정 등 인증 실패",
                                        "#/components/schemas/ExceptionResponse"
                                ))
                                .addApiResponse("403", jsonResponse(
                                        "차단 계정 또는 탈퇴 유예 계정 등 로그인 제한",
                                        "#/components/schemas/ExceptionResponse"
                                ))
                                .addApiResponse("429", jsonResponse(
                                        "로그인 시도 횟수 초과",
                                        "#/components/schemas/ExceptionResponse"
                                )))));
    }

    private RequestBody jsonRequestBody(String schemaRef) {
        return new RequestBody()
                .required(true)
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(refSchema(schemaRef))));
    }

    private ApiResponse jsonResponse(String description, String schemaRef) {
        return new ApiResponse()
                .description(description)
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(refSchema(schemaRef))));
    }

    private Schema<?> refSchema(String ref) {
        return new Schema<>().$ref(ref);
    }
}
