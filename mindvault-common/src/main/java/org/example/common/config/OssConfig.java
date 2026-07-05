package org.example.common.config;


import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {
    private String endpoint;//阿里云OSS的endpoint,地区
    private String accessKeyId;//阿里云OSS的accessKeyId
    private String accessKeySecret;//阿里云OSS的accessKeySecret
    private String bucketName;//容器名

    @Bean
    public OSS ossClient() {
        //创建OSSClient实例。
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}
//用于阿里云OSS的配置类
