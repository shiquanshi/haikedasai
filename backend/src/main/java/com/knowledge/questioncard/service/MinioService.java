package com.knowledge.questioncard.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {
    private final MinioClient minioClient;
    private final com.knowledge.questioncard.config.MinioConfig minioConfig;

    /**
     * 确保bucket存在并设置公共读取权限
     */
    public void ensureBucketExists() {
        try {
            String bucketName = minioConfig.getBucketName();
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("创建MinIO bucket: {}", bucketName);
            }
            
            // 设置bucket为公共读取权限
            String policy = String.format(
                "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"*\"},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::%s/*\"]}]}",
                bucketName);
            
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(policy)
                    .build());
            log.info("已设置bucket {} 为公共读取权限", bucketName);
            
        } catch (Exception e) {
            log.error("配置bucket失败", e);
            throw new RuntimeException("配置bucket失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件
     */
    public String uploadFile(MultipartFile file) {
        try {
            ensureBucketExists();
            
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            String fileName = UUID.randomUUID().toString() + extension;
            
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            
            // 返回访问URL
            return getFileUrl(fileName);
        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new RuntimeException("上传文件失败: " + e.getMessage());
        }
    }

    /**
     * 上传Base64图片
     */
    public String uploadBase64Image(String base64Data) {
        try {
            ensureBucketExists();
            
            // 解析Base64数据
            String[] parts = base64Data.split(",");
            if (parts.length != 2) {
                throw new IllegalArgumentException("无效的Base64图片数据");
            }
            
            String dataHeader = parts[0];
            String actualData = parts[1];
            
            // 从data header中提取图片类型
            String extension = ".jpg";
            if (dataHeader.contains("png")) {
                extension = ".png";
            } else if (dataHeader.contains("gif")) {
                extension = ".gif";
            } else if (dataHeader.contains("jpeg") || dataHeader.contains("jpg")) {
                extension = ".jpg";
            }
            
            byte[] imageBytes = Base64.getDecoder().decode(actualData);
            String fileName = UUID.randomUUID().toString() + extension;
            
            try (InputStream inputStream = new ByteArrayInputStream(imageBytes)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .object(fileName)
                        .stream(inputStream, imageBytes.length, -1)
                        .contentType("image/" + extension.substring(1))
                        .build());
            }
            
            return getFileUrl(fileName);
        } catch (Exception e) {
            log.error("上传Base64图片失败", e);
            throw new RuntimeException("上传Base64图片失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件访问URL
     */
    public String getFileUrl(String fileName) {
        // 返回通过Nginx代理的相对路径
        return "/minio/" + minioConfig.getBucketName() + "/" + fileName;
    }

    /**
     * 删除文件
     */
    public void deleteFile(String fileName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(fileName)
                    .build());
        } catch (Exception e) {
            log.error("删除文件失败", e);
            throw new RuntimeException("删除文件失败: " + e.getMessage());
        }
    }
}