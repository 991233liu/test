package com.spring.txt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;

/**
 * 定时更新列表
 *
 */
@Service
public class UpdataService {
    private static final Logger log = LogManager.getLogger(UpdataService.class);

    private String baseFilePath;

    @PostConstruct
    public void init() {
        baseFilePath = System.getProperty("user.dir") + "/txt";
    }

    /**
     * 从远端下载文件
     * 
     * @throws IOException
     */
    public void download() throws IOException {

        URL url = new URL("https://mirror.ghproxy.com/raw.githubusercontent.com/ssili126/tv/main/itvlist.txt");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setDoOutput(true);
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(60000);

        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Failed : HTTP error code : " + code);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
            content.append(System.lineSeparator());
//            System.out.println(inputLine);
        }

        in.close();
        conn.disconnect();
        saveFile("newFile.txt", content.toString());
    }

    /**
     * 保存MyFile
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public void writeMyFile() throws IOException {
        File f = getFile("newFile.txt");
        String md5 = MD5.getFileMd5(f);
        System.out.println(md5);
        f = getFile(md5 + ".txt");
        if (f.exists()) { // 已经处理过就不要再处理了
            return;
        }

        Map<String, Object> defUrl = getTemplate("defUrl.json");
        Map<String, Object> template = getTemplate("template.json");
        Map<String, Object> result = new HashMap<>();

        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(getFile("newFile.txt"))));
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            if (inputLine != null) {
                int ln = inputLine.indexOf(",");
                if (ln > 2) {
//                    System.out.println(inputLine);
                    String[] urls = generateUrl(inputLine, template);
                    if (urls != null) {
                        Map<String, List<String>> group = (Map<String, List<String>>) result.get(urls[0]);
                        if (group == null) {
                            group = new HashMap<>();
                            result.put(urls[0], group);
                        }
                        List<String> url = group.get(urls[1]);
                        if (url == null) {
                            url = new ArrayList<>();
                            group.put(urls[1], url);
                        }
                        url.add(urls[2]);
                    }
                }
            }
        }
        in.close();

        // 将url保存到文件
        saveUrl2File(result, template, defUrl);

        // 生成md5文件
        f.createNewFile();

        System.out.println("成功更新了一次！");
    }

    /**
     * 生成
     * 
     * @param inputLine
     * @param template
     */
    @SuppressWarnings("unchecked")
    private String[] generateUrl(String inputLine, Map<String, Object> template) {
        String[] strs = inputLine.split(",");
        // TODO 目前算法中之支持一个分组，bug
        for (Entry<String, Object> entry : template.entrySet()) {
            Map<String, String> group = (Map<String, String>) entry.getValue();
            for (Entry<String, String> gurl : group.entrySet()) {
                if (strs[0].indexOf(gurl.getKey()) > -1) {
                    return new String[] { entry.getKey(), gurl.getKey(), strs[1] };
                } else if (!gurl.getValue().isEmpty()) {
                    String[] v = gurl.getValue().split(",");
                    for (String string : v) {
                        if (strs[0].indexOf(string) > -1) {
                            return new String[] { entry.getKey(), gurl.getKey(), strs[1] };
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * 将url保存到文件
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private void saveUrl2File(Map<String, Object> newUrls, Map<String, Object> template, Map<String, Object> defUrl) throws IOException {
        // TODO 没有找到，则使用上次的或者defUrl
        List<String> out = getTemplateForWrite("template.json");
        StringBuilder content = new StringBuilder();
        Map<String, List<String>> group = null;
        for (String string : out) {
            if (string.indexOf("#genre#") > 0) { // 分组
                group = (Map<String, List<String>>) newUrls.get(string);
                content.append(string);
                content.append(System.lineSeparator());
            } else { // 值
                // TODO 有新URL时使用新的，没有时使用上次的
                if (group.containsKey(string)) {
                    for (String string2 : group.get(string)) {
                        content.append(string);
                        content.append(",");
                        content.append(string2);
                        content.append(System.lineSeparator());
                    }
                } else {

                }

                // 默认值加上
                if (defUrl.containsKey(string)) {
                    content.append(string);
                    content.append(",");
                    content.append(defUrl.get(string));
                    content.append(System.lineSeparator());
                }
            }
        }

        saveFile("newUrl.txt", content.toString());
    }

    /**
     * 读取模板
     * 
     * @param fileName
     * @return
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getTemplate(String fileName) {
        String jsonStr = readFile(fileName);
        if (jsonStr != null) {
            return JSON.parseObject(jsonStr, HashMap.class);
        }
        return null;
    }

    private List<String> getTemplateForWrite(String fileName) throws IOException {
        List<String> result = new ArrayList<>();
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(getFile(fileName))));
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            if (inputLine.indexOf("\"") > -1) {
                inputLine = inputLine.substring(inputLine.indexOf("\"") + 1, inputLine.length());
                inputLine = inputLine.substring(0, inputLine.indexOf("\""));
                result.add(inputLine);
            }
        }
        in.close();

        return result;
    }

    /**
     * 保存到文件
     * 
     * @param fileName
     * @param jsonStr
     */
    private void saveFile(String fileName, String jsonStr) {
        try {
            File file = getFile(fileName);
            FileUtils.writeStringToFile(file, jsonStr, "UTF-8", false);
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * 读取文件
     * 
     * @param fileName
     * @return
     */
    private String readFile(String fileName) {
        try {
            File file = getFile(fileName);
            return FileUtils.readFileToString(file, "UTF-8");
        } catch (IOException e) {
            log.error(e);
        }
        return null;
    }

    private File getFile(String fileName) {
        return new File(baseFilePath + "/" + fileName);
    }
}
