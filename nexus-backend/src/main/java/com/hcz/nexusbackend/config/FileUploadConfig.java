package com.hcz.nexusbackend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    @Autowired
    private FileStorageConfig fileStorageConfig;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        String dir = fileStorageConfig.getUploadDir();
        new File(dir).mkdirs();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + new File(dir).getAbsolutePath() + "/");
    }
}
