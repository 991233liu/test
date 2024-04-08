package com.spring.ollama;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 基于Ollama的AI对话模型
 *
 */
@RestController
@RequestMapping("/prompt")
public class PromptController {

    @Autowired
    private PromptService promptService;

    /**
     * 开始一个新会话。
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/start")
    public Map<String, Object> start(@RequestBody Map<String, Object> param) {
        Map<String, Object> data = new HashMap<>();
        data.put("context",
                promptService.start((String) param.get("appId"), (String) param.get("userId"), (String) param.get("userType"), (Map<String, Object>) param.get("options")));
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "操作成功");
        result.put("data", data);
        return result;
    }

    /**
     * 与AI开始对话
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "操作成功");
        result.put("data", promptService.generate((String) param.get("appId"), (String) param.get("userId"), (String) param.get("userType"), (String) param.get("prompt"),
                (String) param.get("answerTemplate"), (List<Integer>) param.get("context"), (Map<String, Object>) param.get("options")));
        return result;
    }

    /**
     * 与AI开始对话(流式)
     * @throws Exception 
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/generateV2")
    public void generateV2(@RequestBody Map<String, Object> param, HttpServletResponse response) throws Exception {
        promptService.generateV2((String) param.get("appId"), (String) param.get("userId"), (String) param.get("userType"), (String) param.get("prompt"),
                (String) param.get("answerTemplate"), (List<Integer>) param.get("context"), (Map<String, Object>) param.get("options"), response);
    }
}
