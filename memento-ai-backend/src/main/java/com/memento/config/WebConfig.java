package com.memento.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-path}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = new File(uploadPath).getAbsolutePath();
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        
        // 映射 /uploads/** 到本地文件路径
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + path);
    }
}
