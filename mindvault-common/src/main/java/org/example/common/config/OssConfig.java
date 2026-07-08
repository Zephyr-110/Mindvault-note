package org.example.common.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    @Bean
    public CredentialsProvider credentialsProvider() {
        // 启动时就校验，避免运行时才报错
        if (accessKeyId == null || accessKeyId.isBlank()) {
            throw new IllegalStateException("请设置环境变量 ALIYUN_ACCESS_KEY_ID");
        }
        if (accessKeySecret == null || accessKeySecret.isBlank()) {
            throw new IllegalStateException("请设置环境变量 ALIYUN_ACCESS_KEY_SECRET");
        }

        log.info("OSS凭证已加载，Bucket: {}", bucketName);
        return new DefaultCredentialProvider(accessKeyId, accessKeySecret);
    }

    @Bean(destroyMethod = "shutdown")
    public OSS ossClient(CredentialsProvider credentialsProvider) {
        // 使用 CredentialsProvider，更安全
        return new OSSClientBuilder().build(endpoint, credentialsProvider);
    }
}