package org.example.common.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cutstomOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MindVault 笔记系统 API")
                        .description("MindVault 个人笔记管理系统接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("开发者")
                                .email("your-email@example.com")))
                .addSecurityItem(new SecurityRequirement().addList("JWT"))
                .components(new Components()
                        .addSecuritySchemes("JWT", new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("请输入 JWT Token（无需带 Bearer 前缀）")));
    }
//上边是JWT令牌的配置
    @Bean               // ← 再加一个方法：注册 RestTemplate 类型的 Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
