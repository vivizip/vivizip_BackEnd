package com.example.vivizip.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.servlet.ServletContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static com.example.vivizip.consts.StaticVariable.*;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class SwaggerConfig {

    private final ApplicationContext applicationContext;

    @Bean
    public OpenAPI openAPI(ServletContext servletContext) {
        String contextPath = servletContext.getContextPath();
        Server server = new Server().url(contextPath);
        return new OpenAPI()
                .servers(List.of(server))
                .components(authSetting())
                .addSecurityItem(new SecurityRequirement().addList(SWAGGER_JWT))
                .info(swaggerInfo());
    }

    private Info swaggerInfo() {
        License license = new License();
        license.setUrl("https://github.com/vivizip/vivizip_BackEnd");
        license.setName("Vivizip");

        return new Info()
                .version("v0.0.1")
                .title("\"Vivizip 서버 API문서\"")
                .description("Vivizip 서버의 API 문서 입니다.")
                .license(license);
    }

    private Components authSetting() {
        return new Components()
                .addSecuritySchemes(
                        SWAGGER_JWT,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme(SWAGGER_BEARER)
                                .bearerFormat(AUTHORIZATION)
                                .in(SecurityScheme.In.HEADER)
                                .name(SWAGGER_JWT));
    }
}
