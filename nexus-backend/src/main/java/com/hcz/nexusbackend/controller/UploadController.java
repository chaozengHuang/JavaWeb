package com.hcz.nexusbackend.controller;

import com.hcz.nexusbackend.common.Result;
import com.hcz.nexusbackend.config.FileStorageConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Autowired
    private FileStorageConfig fileStorageConfig;

    @PostMapping("/image")
    public Result<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        return doUpload(file, "images");
    }

    @PostMapping("/file")
    public Result<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        return doUpload(file, "files");
    }

    private Result<Map<String, Object>> doUpload(MultipartFile file, String subDir) {
        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }
        try {
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String fileName = subDir + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

            File dir = new File(fileStorageConfig.getUploadDir() + "/" + subDir);
            if (!dir.exists()) dir.mkdirs();

            file.transferTo(new File(dir, fileName));

            String url = "/uploads/" + subDir + "/" + fileName;
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("url", url);
            data.put("name", originalName);
            return Result.success("上传成功", data);
        } catch (Exception e) {
            return Result.error(500, "上传失败: " + e.getMessage());
        }
    }
}
