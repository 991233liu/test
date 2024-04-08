package com.spring.ollama;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.client.RestTemplateBuilderConfigurer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * 基于Ollama的AI对话模型
 *
 */
@Service
public class PromptService {
    private static final Logger log = LogManager.getLogger(PromptService.class);

    private RestTemplate restTemplate;
    @Value("${ollama.url}")
    private String url;
    @Value("${ollama.connectTimeout:10000}")
    private int connectTimeout;
    @Value("${ollama.readTimeout:600000}")
    private int readTimeout;
    @Value("${ollama.model:qwen:7b-chat}")
    private String model;
    private HttpHeaders headers = new HttpHeaders();
    private String baseFilePath;

    @PostConstruct
    public void init() {
        RestTemplateBuilderConfigurer configurer = new RestTemplateBuilderConfigurer();
        RestTemplateBuilder builder = new RestTemplateBuilder().setConnectTimeout(Duration.ofMillis(connectTimeout)).setReadTimeout(Duration.ofSeconds(readTimeout));
        restTemplate = configurer.configure(builder).build();

        headers.set("Connection", "keep-alive");
        headers.set("Content-Type", "application/json; utf-8");

        baseFilePath = System.getProperty("user.dir") + "/cache";
    }

    /**
     * 开始一个新会话。
     * 
     * @param appId
     * @param userId
     * @param userType
     * @return 会话id
     */
    @SuppressWarnings("unchecked")
    public List<Integer> start(String appId, String userId, String userType, Map<String, Object> options) {
        // 从历史会话中获取可用context
        Map<String, Object> history = getHistory(appId, userId, userType);
        List<Integer> context = getHistoryContext(history);
        if (context != null && context.size() > 0)
            return context;

        Map<String, Object> param = generateParameter(assemblePrompt(userId, userType, null, null, false), null, options);
        param.put("keep_alive", "13h");
        param.put("stream", false);
        String result = send2ollama(param);
        context = (List<Integer>) JSON.parseObject(result, HashMap.class).get("context");
        // 保存context到历史会话
        saveHistoryContext(appId, userId, userType, context, history);
        return context;
    }

    /**
     * 与AI开始对话
     * 
     * @param appId
     * @param userId
     * @param userType
     * @param prompt
     * @param answerTemplate
     * @param context
     * @param options
     * @return AI回答
     */
    public Object generate(String appId, String userId, String userType, String prompt, String answerTemplate, List<Integer> context, Map<String, Object> options) {
        if (prompt == null || prompt.isEmpty())
            return null;
        prompt = assemblePrompt(userId, userType, prompt, answerTemplate, context != null && context.size() > 0);
        // 从历史会话中获取回答
        Map<String, Object> history = getHistory(appId, userId, userType);
        Object answer = getHistoryAnswer(history, prompt);
        if (answer != null)
            return answer;

        Map<String, Object> param = generateParameter(prompt, context, options);
        // 新增流式接口，此处写死
        param.put("stream", false);
        String result = send2ollama(param);
        answer = JSON.parseObject(result, HashMap.class);
        // 保存回答到历史会话
        saveHistoryAnswer(appId, userId, userType, prompt, answer, history);
        return answer;
    }

    /**
     * 与AI开始对话(流式)
     * 
     * @param appId
     * @param userId
     * @param userType
     * @param prompt
     * @param answerTemplate
     * @param context
     * @param options
     * @param response
     * @return AI回答
     * @throws Exception
     */
    public void generateV2(String appId, String userId, String userType, String prompt, String answerTemplate, List<Integer> context, Map<String, Object> options,
            HttpServletResponse response) throws Exception {
        if (prompt == null || prompt.isEmpty())
            return;
        prompt = assemblePrompt(userId, userType, prompt, answerTemplate, context != null && context.size() > 0);
        // 从历史会话中获取回答
        Map<String, Object> history = getHistory(appId, userId, userType);
        Object answer = getHistoryAnswer(history, prompt + "-stream");
        if (answer != null) {
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(((String) answer).getBytes("UTF-8")), "UTF-8"));
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.getWriter().write(inputLine);
                    response.getWriter().write(System.lineSeparator());
                    response.getWriter().flush();
                }
            } catch (Exception e) {
                log.error(e);
                throw e;
            } finally {
                try {
                    if (in != null)
                        in.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
            return;
        }

        Map<String, Object> param = generateParameter(prompt, context, options);
        param.put("stream", true);
        StringBuilder result = new StringBuilder();
        BufferedReader in = null;
        HttpURLConnection conn = null;
        try {
            URL generateUrl = new URL(url + "/api/generate");
            conn = (HttpURLConnection) generateUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);

            String jsonInputString = JSONObject.toJSONString(param);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Failed : HTTP error code : " + code);
            }
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            while ((inputLine = in.readLine()) != null) {
                result.append(inputLine);
                result.append(System.lineSeparator());
                response.getWriter().write(inputLine);
                response.getWriter().write(System.lineSeparator());
                response.getWriter().flush();
            }
        } catch (Exception e) {
            log.error(e);
            throw e;
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {
                log.error(e);
            }
            if (conn != null)
                conn.disconnect();
        }

        // 保存回答到历史会话
        saveHistoryAnswer(appId, userId, userType, prompt + "-stream", result.toString(), history);
    }

    /**
     * 拼装对话问题
     * 
     * @param userId
     * @param userType
     * @param prompt
     * @param answerTemplate
     * @param hasContext
     * @return
     */
    private String assemblePrompt(String userId, String userType, String prompt, String answerTemplate, boolean hasContext) {
        StringBuffer bf = new StringBuffer();
        if (!hasContext) {
            bf.append("你是国中康健的虚拟健康管家，");
            if ("user".equals(userType)) {
                bf.append("我是ID为");
                bf.append(userId);
                bf.append("的客户，");
            } else if ("doctor".equals(userType)) {
                bf.append("我是ID为");
                bf.append(userId);
                bf.append("的医生，");
            }
        }
        if (prompt != null && !prompt.isEmpty())
            bf.append(prompt);
        if (answerTemplate != null && !answerTemplate.isEmpty()) {
            bf.append("\n请使用如下格式做出回答：\n");
            bf.append(answerTemplate);
        }
        return bf.toString();
    }

    /**
     * 将对话发送到Ollama
     * 
     * @param param
     * @return
     */
    private String send2ollama(Map<String, Object> param) {
        String generateUrl = url + "/api/generate";
        HttpEntity<String> res = restTemplate.exchange(generateUrl, HttpMethod.POST, new HttpEntity<>(param, headers), String.class);
        String result = res.getBody();
        log.debug("send2ollama.result={}", result);
        return result;
    }

    /**
     * 生成请求参数
     * 
     * @param data
     * @param clazz
     * @return
     */
    private Map<String, Object> generateParameter(String prompt, List<Integer> context, Map<String, Object> options) {
        Map<String, Object> param = new HashMap<>();
        param.put("model", model);
        param.put("prompt", prompt);
        if (context != null && context.size() > 0) {
            param.put("context", context);
//            param.put("keep_alive", "24h");
        }
        if (options != null && !options.isEmpty())
            for (Map.Entry<String, Object> entry : options.entrySet()) {
                param.put(entry.getKey(), entry.getValue());
            }

        return param;
    }

    /**
     * 从历史会话中获取回答
     * 
     * @param appId
     * @param userId
     * @param userType
     * @return
     */
    @SuppressWarnings("unchecked")
    private Object getHistoryAnswer(Map<String, Object> history, String prompt) {
        String key = getAnswerKey(prompt);
        if (history.containsKey(key)) {
            Map<String, Object> map = (Map<String, Object>) history.get(key);
            return map.get("answer");
        }
        return null;
    }

    /**
     * 从历史会话中获取可用context
     * 
     * @param appId
     * @param userId
     * @param userType
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<Integer> getHistoryContext(Map<String, Object> history) {
        if (history.containsKey("context")) {
            Long expTime = (Long) history.get("context-expTime");
            if (System.currentTimeMillis() < expTime) {
                return (List<Integer>) history.get("context");
            }
        }
        return null;
    }

    /**
     * 保存context到历史会话
     * 
     * @param appId
     * @param userId
     * @param userType
     * @return
     */
    private void saveHistoryContext(String appId, String userId, String userType, List<Integer> context, Map<String, Object> history) {
        history.put("context", context);
        Instant expTime = Instant.now().plus(Duration.ofHours(12));
        history.put("context-expTime", expTime.toEpochMilli());
        saveHistory(appId, userId, userType, history);
    }

    /**
     * 保存回答到历史会话
     * 
     * @param appId
     * @param userId
     * @param userType
     * @return
     */
    @SuppressWarnings("unchecked")
    private void saveHistoryAnswer(String appId, String userId, String userType, String prompt, Object answer, Map<String, Object> history) {
        String key = getAnswerKey(prompt);
        Map<String, Object> map = new HashMap<>();
        map.put("prompt", prompt);
        map.put("answer", answer);
        history.put(key, map);
        // 只保留200条
        List<String> keys = null;
        if (history.containsKey("keys"))
            keys = (List<String>) history.get("keys");
        else {
            keys = new ArrayList<>();
            history.put("keys", keys);
        }
        keys.add(key);
        if (keys.size() > 200) {
            String rmKey = keys.remove(0);
            history.remove(rmKey);
        }
        saveHistory(appId, userId, userType, history);
    }

    /**
     * 获取历史会话
     * 
     * @param appId
     * @param userId
     * @param userType
     * @return
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getHistory(String appId, String userId, String userType) {
        try {
            File file = getHistoryFile(appId, userId, userType);
            if (file.exists()) {
                String jsonStr = FileUtils.readFileToString(file, "UTF-8");
                if (jsonStr != null) {
                    return JSON.parseObject(jsonStr, HashMap.class);
                }
            }
        } catch (IOException e) {
            log.error(e);
        }
        return new HashMap<>();
    }

    /**
     * 保存历史会话到文件
     * 
     * @param appId
     * @param userId
     * @param userType
     * @param history
     * @return
     */
    private void saveHistory(String appId, String userId, String userType, Map<String, Object> history) {
        try {
            File file = getHistoryFile(appId, userId, userType);
            String jsonStr = JSON.toJSONString(history);
            FileUtils.writeStringToFile(file, jsonStr, "UTF-8", false);
        } catch (IOException e) {
            log.error(e);
        }
    }

    private File getHistoryFile(String appId, String userId, String userType) {
        return new File(baseFilePath + "/" + appId + "/" + userType + "/" + userId + ".db");
    }

    private String getAnswerKey(String prompt) {
        try {
            // SHA-256作为key
            byte[] hashBytes;
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(prompt.getBytes(StandardCharsets.UTF_8));
            hashBytes = messageDigest.digest();
            return String.format("%064x", new BigInteger(1, hashBytes));
        } catch (Exception e) {
            log.error(e);
        }
        return null;
    }
}
