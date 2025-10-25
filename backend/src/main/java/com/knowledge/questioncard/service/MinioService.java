package com.knowledge.questioncard.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
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
     * 上传文件流
     */
    public String uploadFile(InputStream inputStream, String fileName, String contentType) {
        try {
            ensureBucketExists();
            
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(fileName)
                    .stream(inputStream, -1, 10485760) // 最大10MB
                    .contentType(contentType)
                    .build());
            
            // 返回访问URL
            return getFileUrl(fileName);
        } catch (Exception e) {
            log.error("上传文件流失败", e);
            throw new RuntimeException("上传文件流失败: " + e.getMessage());
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
        // 返回完整的可访问URL，直接存储到数据库
        return "https://nohavebug.preview.huawei-zeabur.cn/question-card-images/" + fileName;
    }

    /**
     * 从URL下载图片并上传到MinIO（带重试机制）
     */
    public String uploadFromUrl(String imageUrl) {
        log.info("🔄 [MinIO上传] 开始处理: {}", imageUrl);
        
        int maxRetries = 3;
        int retryCount = 0;
        Exception lastException = null;
        
        while (retryCount < maxRetries) {
            try {
                ensureBucketExists();
                
                // 从URL下载图片（设置超时）
                long downloadStartTime = System.currentTimeMillis();
                URL url = new URL(imageUrl);
                java.net.URLConnection connection = url.openConnection();
                connection.setConnectTimeout(30000); // 连接超时30秒
                connection.setReadTimeout(60000);    // 读取超时60秒
                InputStream inputStream = connection.getInputStream();
                long downloadEndTime = System.currentTimeMillis();
                log.info("⏱️ [MinIO上传] 下载图片完成 - 耗时:{}ms", downloadEndTime - downloadStartTime);
                
                // 从URL中提取文件扩展名，如果没有则默认为.png
                String extension = ".png";
                String path = url.getPath();
                if (path.contains(".")) {
                    extension = path.substring(path.lastIndexOf("."));
                    // 限制扩展名长度，防止异常
                    if (extension.length() > 5) {
                        extension = ".png";
                    }
                }
                
                String fileName = UUID.randomUUID().toString() + extension;
                
                // 上传到MinIO，使用-1让MinIO自动检测大小
                long uploadStartTime = System.currentTimeMillis();
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .object(fileName)
                        .stream(inputStream, -1, 10485760) // 最大10MB
                        .contentType("image/" + extension.substring(1))
                        .build());
                
                inputStream.close();
                long uploadEndTime = System.currentTimeMillis();
                
                if (retryCount > 0) {
                    log.info("✅ [MinIO上传] 成功（第{}次重试）- 上传耗时:{}ms, 总耗时:{}ms: {} -> {}", 
                        retryCount, uploadEndTime - uploadStartTime, uploadEndTime - downloadStartTime, imageUrl, fileName);
                } else {
                    log.info("✅ [MinIO上传] 成功 - 上传耗时:{}ms, 总耗时:{}ms: {} -> {}", 
                        uploadEndTime - uploadStartTime, uploadEndTime - downloadStartTime, imageUrl, fileName);
                }
                return getFileUrl(fileName);
                
            } catch (java.net.SocketTimeoutException e) {
                lastException = e;
                retryCount++;
                log.warn("⏱️ [MinIO上传] 下载超时（第{}次尝试）: {} - {}", retryCount, imageUrl, e.getMessage());
                if (retryCount < maxRetries) {
                    long waitTime = (long) Math.pow(2, retryCount - 1) * 1000;
                    log.info("等待{}ms后进行第{}次重试...", waitTime, retryCount + 1);
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("上传重试被中断: " + ie.getMessage());
                    }
                } else {
                    log.error("❌ [MinIO上传] 多次超时失败（已重试{}次）: {}", maxRetries, imageUrl);
                }
            } catch (java.io.IOException e) {
                lastException = e;
                retryCount++;
                log.warn("🌐 [MinIO上传] 网络IO错误（第{}次尝试）: {} - {}", retryCount, imageUrl, e.getMessage());
                if (retryCount < maxRetries) {
                    long waitTime = (long) Math.pow(2, retryCount - 1) * 1000;
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("上传重试被中断: " + ie.getMessage());
                    }
                } else {
                    log.error("❌ [MinIO上传] 网络错误失败（已重试{}次）: {}", maxRetries, imageUrl);
                }
            } catch (Exception e) {
                lastException = e;
                retryCount++;
                log.warn("⚠️ [MinIO上传] 未知错误（第{}次尝试）: {} - 类型:{}, 消息:{}", 
                    retryCount, imageUrl, e.getClass().getName(), e.getMessage());
                if (retryCount < maxRetries) {
                    long waitTime = (long) Math.pow(2, retryCount - 1) * 1000;
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("上传重试被中断: " + ie.getMessage());
                    }
                } else {
                    log.error("❌ [MinIO上传] 未知错误失败（已重试{}次）: {}", maxRetries, imageUrl, e);
                }
            }
        }
        
        throw new RuntimeException("从URL上传图片失败（已重试" + maxRetries + "次）: " + lastException.getMessage());
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

    /**
     * 下载文件流
     * @param fileName 文件名
     * @return 文件输入流
     */
    public InputStream downloadFile(String fileName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(fileName)
                    .build());
        } catch (Exception e) {
            log.error("下载文件失败: {}", fileName, e);
            throw new RuntimeException("下载文件失败: " + e.getMessage());
        }
    }

    /**
     * 从完整URL中提取文件名并下载
     * @param fileUrl 文件URL（格式：/minio/bucket/filename）
     * @return 文件输入流
     */
    public InputStream downloadFileFromUrl(String fileUrl) {
        try {
            // 从URL中提取文件名
            // 格式：/minio/bucket/filename
            String[] parts = fileUrl.split("/");
            if (parts.length < 4) {
                throw new IllegalArgumentException("无效的文件URL: " + fileUrl);
            }
            String fileName = parts[parts.length - 1];
            return downloadFile(fileName);
        } catch (Exception e) {
            log.error("从URL下载文件失败: {}", fileUrl, e);
            throw new RuntimeException("从URL下载文件失败: " + e.getMessage());
        }
    }
}