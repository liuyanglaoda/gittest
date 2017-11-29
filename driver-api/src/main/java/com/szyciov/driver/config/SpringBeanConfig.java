package com.szyciov.driver.config;

import com.supervision.service.BasicMqService;
import com.supervision.service.EvaluateMqService;
import com.supervision.service.GpsMqService;
import com.supervision.service.OperationMqService;
import com.supervision.service.OrderMqService;
import com.supervision.service.RelationMqService;
import com.szyciov.util.CustomPropertyConfigurer;
import com.szyciov.util.SMSTempPropertyConfigurer;
import com.szyciov.util.SupervisionMessageUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

/**
 * Created by shikang on 2017/9/5.
 */
@Configuration
public class SpringBeanConfig {

    /**
     * 读取配置文件
     * @return
     */
    @Bean
    public CustomPropertyConfigurer propertyConfigurer2(Environment env) {
        CustomPropertyConfigurer configurer = new CustomPropertyConfigurer();
        configurer.setIgnoreResourceNotFound(true);
        configurer.setIgnoreUnresolvablePlaceholders(true);
        configurer.setLocations(new ClassPathResource(env.getProperty("spring.profiles.active") + "/web.properties"),
            new ClassPathResource(env.getProperty("spring.profiles.active") + "/redis.properties"),
                new ClassPathResource(env.getProperty("spring.profiles.active") + "/rabbit.properties"));
        return configurer;
    }

    /**
     * 读取短信配置
     * @return
     */
    @Bean
    public SMSTempPropertyConfigurer smsTempPropertyConfigurer() {
        SMSTempPropertyConfigurer configurer = new SMSTempPropertyConfigurer();
        configurer.setIgnoreResourceNotFound(true);
        configurer.setIgnoreUnresolvablePlaceholders(true);
        configurer.setLocation(new ClassPathResource("public/sms.properties"));
        return configurer;
    }

    /**
     * 监管工具类
     * @return
     */
    @Bean
    public SupervisionMessageUtil supervisionMessageUtil() {
        return new SupervisionMessageUtil();
    }

    /**
     * 静态数据服务
     * @return
     */
    @Bean
    public BasicMqService basicMqService(){
        return  new BasicMqService();
    }

    /**
     * 订单服务
     * @return
     */
    @Bean
    public OrderMqService orderMqService(){
        return  new OrderMqService();
    }

    /**
     * 运营服务
     * @return
     */
    @Bean
    public OperationMqService operationMqService(){
        return new OperationMqService();
    }

    /**
     * GPS服务
     * @return
     */
    @Bean
    public GpsMqService gpsMqService(){
        return new GpsMqService();
    }

    /**
     * 服务质量信息
     * @return
     */
    @Bean
    public EvaluateMqService evaluateMqService(){
        return  new EvaluateMqService();
    }


    /**
     * 人车对应关系信息
     * @return
     */
    @Bean
    public RelationMqService relationMqService(){
        return  new RelationMqService();
    }

}
 