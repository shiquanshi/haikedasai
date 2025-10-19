package com.knowledge.questioncard.controller;

import com.knowledge.questioncard.service.TTSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/tts")
@RequiredArgsConstructor
public class TTSController {

    private final TTSService ttsService;

    /**
     * 文本转语音接口
     * @param request 包含text字段的请求体
     * @return Base64编码的音频数据
     */
    @PostMapping("/convert")
    public ResponseEntity<Map<String, Object>> textToSpeech(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            if (text == null || text.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "文本内容不能为空");
                return ResponseEntity.badRequest().body(error);
            }

            String audioData = ttsService.textToSpeech(text);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("audioData", audioData);
            response.put("format", "mp3");
            response.put("encoding", "base64");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("TTS转换失败: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "TTS转换失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 文本转语音并直接返回音频文件
     * @param request 包含text字段的请求体
     * @return MP3音频文件
     */
    @PostMapping("/convert/audio")
    public ResponseEntity<byte[]> textToSpeechAudio(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            String base64Audio = ttsService.textToSpeech(text);
            byte[] audioBytes = ttsService.decodeAudio(base64Audio);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentDispositionFormData("attachment", "speech.mp3");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(audioBytes);
        } catch (Exception e) {
            log.error("TTS转换失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}