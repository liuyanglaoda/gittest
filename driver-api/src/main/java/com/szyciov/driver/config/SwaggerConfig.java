package com.szyciov.driver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author lzc
 * @date 2017/10/10
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Value("${swagger.enable}")
    private boolean swaggerShow;

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
            .forCodeGeneration(true)
            .apiInfo(apiInfo())
            .enable(this.swaggerShow)
            .select()
            .apis(RequestHandlerSelectors.basePackage("com.szyciov.driver.controller"))
            .paths(PathSelectors.any())
            .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
            .title("driver-api")
            .description("司机端接口相关服务 restfullApi")
            .termsOfServiceUrl("")
            .contact("")
            .version("1.0")
            .build();
    }
}
 