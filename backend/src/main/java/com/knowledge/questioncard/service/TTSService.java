package com.knowledge.questioncard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class TTSService {

    @Value("${volcengine.tts.api-key}")
    private String apiKey;

    @Value("${volcengine.tts.endpoint}")
    private String ttsEndpoint;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public TTSService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 文本转语音
     * @param text 要转换的文本
     * @return Base64编码的音频数据
     */
    public String textToSpeech(String text) {
        try {
            // 构建请求体 - 火山引擎 unidirectional API 格式
        Map<String, Object> requestBody = new HashMap<>();
        
        // req_params 字段
        Map<String, Object> reqParams = new HashMap<>();
        reqParams.put("text", text);
        reqParams.put("speaker", "zh_female_shuangkuaisisi_moon_bigtts");
        
        // additions 参数必须是JSON字符串
        String additions = "{\"silence_duration\":\"125\",\"with_frontend\":\"1\",\"with_timestamp\":\"1\"}";
        reqParams.put("additions", additions);
        
        requestBody.put("req_params", reqParams);
        
        // audio_params 字段
        Map<String, String> audioParams = new HashMap<>();
        audioParams.put("format", "mp3");
        audioParams.put("sample_rate", "24000");
        requestBody.put("audio_params", audioParams);

            // 设置请求头 - 使用x-api-key认证
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("X-Api-Resource-Id", "volc.service_type.10029");
            
            log.info("========== TTS API 调用开始 ==========");
            log.info("请求URL: {}", ttsEndpoint);
            log.info("API Key: {}...{}", apiKey.substring(0, Math.min(8, apiKey.length())), apiKey.length() > 8 ? apiKey.substring(apiKey.length() - 4) : "");
            log.info("请求文本长度: {}", text.length());
            log.info("请求文本内容: {}", text);
            log.info("请求头: {}", headers);
            log.info("完整请求体: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 发送请求 - 火山引擎TTS返回JSON格式响应
            ResponseEntity<String> response = restTemplate.exchange(
                ttsEndpoint,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("响应状态码: {}", response.getStatusCode());
            log.info("响应头: {}", response.getHeaders());
            
            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                log.info("原始响应体: {}", responseBody);
                
                // 解析JSON响应
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                Integer code = (Integer) responseMap.get("code");
                String message = (String) responseMap.get("message");
                String data = (String) responseMap.get("data");
                
                log.info("响应解析结果:");
                log.info("  - code: {}", code);
                log.info("  - message: {}", message);
                log.info("  - data存在: {}", data != null);
                if (data != null) {
                    log.info("  - data长度: {}", data.length());
                    log.info("  - data前100字符: {}", data.substring(0, Math.min(100, data.length())));
                }
                
                // code为0表示成功(根据火山引擎API实际响应)
                if (code != null && code == 0) {
                    // 火山引擎TTS API返回流式数据,需要解析多行JSON获取音频
                    // 响应格式: 多行JSON,每行包含code/message/data字段
                    // 最后一行包含实际的音频数据
                    String[] lines = responseBody.split("\n");
                    StringBuilder audioData = new StringBuilder();
                    
                    for (String line : lines) {
                        if (line.trim().isEmpty()) continue;
                        try {
                            JsonNode lineNode = objectMapper.readTree(line);
                            JsonNode lineData = lineNode.get("data");
                            if (lineData != null && !lineData.isNull() && lineData.isTextual()) {
                                audioData.append(lineData.asText());
                            }
                        } catch (Exception ex) {
                            log.warn("解析响应行失败: {}", line, ex);
                        }
                    }
                    
                    String finalAudioData = audioData.toString();
                    if (!finalAudioData.isEmpty()) {
                        log.info("========== TTS转换成功 ==========");
                        log.info("文本长度: {}", text.length());
                        log.info("Base64数据长度: {}", finalAudioData.length());
                        return finalAudioData;
                    } else {
                        log.error("========== TTS API未返回音频数据 ==========");
                        log.error("完整响应: {}", responseBody);
                        throw new RuntimeException("TTS API未返回音频数据");
                    }
                } else {
                    log.error("========== TTS API返回失败 ==========");
                    log.error("错误码: {}", code);
                    log.error("错误信息: {}", message);
                    log.error("完整响应: {}", responseBody);
                    throw new RuntimeException("TTS API调用失败: " + message);
                }
            } else {
                log.error("========== HTTP请求失败 ==========");
                log.error("状态码: {}", response.getStatusCode());
                log.error("响应体: {}", response.getBody());
                throw new RuntimeException("TTS转换失败: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("TTS转换异常: {}", e.getMessage(), e);
            throw new RuntimeException("TTS转换异常: " + e.getMessage(), e);
        }
    }

    /**
     * 将Base64音频数据转换为字节数组
     * @param base64Audio Base64编码的音频
     * @return 音频字节数组
     */
    public byte[] decodeAudio(String base64Audio) {
        return Base64.getDecoder().decode(base64Audio);
    }
}