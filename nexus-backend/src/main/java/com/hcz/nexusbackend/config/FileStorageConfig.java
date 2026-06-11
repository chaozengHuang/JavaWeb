package com.hcz.nexusbackend.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Slf4j
@Configuration
public class FileStorageConfig {

    @Value("${file.upload.dir:auto}")
    private String configuredDir;

    private String resolvedDir;

    @PostConstruct
    public void init() {
        if (!"auto".equals(configuredDir)) {
            resolvedDir = configuredDir;
        } else {
            // 自动检测：优先用 user.dir（IDE启动=项目根目录, jar启动=jar所在目录）
            String cwd = System.getProperty("user.dir");
            File candidate = new File(cwd, "uploads");
            if (candidate.exists() || candidate.mkdirs()) {
                resolvedDir = candidate.getAbsolutePath();
            } else {
                // 回退到用户主目录
                candidate = new File(System.getProperty("user.home"), ".nexus-uploads");
                candidate.mkdirs();
                resolvedDir = candidate.getAbsolutePath();
            }
        }
        log.info("文件上传目录: {}", resolvedDir);
    }

    /** 获取已解析的绝对路径 */
    public String getUploadDir() {
        return resolvedDir;
    }
}
