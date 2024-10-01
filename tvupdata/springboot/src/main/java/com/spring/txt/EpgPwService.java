/** Copyright © 2021-2050 刘路峰版权所有。 */
package com.spring.txt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;

/**
 * https://epg.pw源处理
 *
 */
@Service
public class EpgPwService {
    private static final Logger log = LogManager.getLogger(EpgPwService.class);

    @Value("${config.localGit}")
    private String localGitPath;
    private String baseFilePath;
    private Map<String, Object> epgInfo;

    @PostConstruct
    public void init() {
        baseFilePath = System.getProperty("user.dir") + "/txt";
        epgInfo = getJsonFile("EpgUrl.json");
        if (epgInfo == null)
            epgInfo = new HashMap<>();
    }

    /**
     * 从远端下载文件
     * 
     * @throws IOException
     */
    public boolean start() {
        Map<String, Object> epgUrls = downloadEpgUrls(epgInfo);
        if (writeTempFile(epgUrls))
            return copy2git();
        else
            return false;
    }

    @SuppressWarnings("unchecked")
    private boolean writeTempFile(Map<String, Object> epgUrls) {
        try {
            List<String> tempTexts = new ArrayList<>();
            List<String> texts = FileUtils.readLines(new File(localGitPath + "/sub/live2/tv3b.txt"), "UTF-8");
            String group = "";
            for (String str : texts) {
                if (str.indexOf(",") > 1) {
                    String tvName = str.substring(0, str.indexOf(","));
                    if (str.indexOf("#genre#") > 1) {
                        group = tvName;
                    } else if (epgUrls.containsKey(tvName)) {
                        if (!epgUrls.containsKey(tvName + "_delete_" + group)) {
                            epgUrls.put(tvName + "_delete_" + group, true);
                            List<String> urls = (List<String>) epgUrls.get(tvName);
                            for (String url : urls) {
                                str = tvName + "," + url;
                                tempTexts.add(str);
                            }
                        }
                        continue;
                    }
                }
                tempTexts.add(str);
            }
            FileUtils.write(new File(baseFilePath + "/epgTemp.txt"), tempTexts.stream().collect(Collectors.joining("\n")), "UTF-8");
            return true;
        } catch (IOException e) {
            log.error("保存临时文件时异常：", e);
        }
        return false;
    }

    private boolean copy2git() {
        File srcFile = new File(baseFilePath + "/epgTemp.txt");
        File destFile = new File(localGitPath + "/sub/live2/tv3b.txt");
        try {
            FileUtils.copyFile(srcFile, destFile);
            return true;
        } catch (IOException e) {
            log.error("复制临时文件到git目录时异常：", e);
        }

        return false;
    }

    /**
     * 从远端下载文件
     * 
     * @throws IOException
     */
    public Map<String, Object> downloadEpgUrls(Map<String, Object> epgInfo) {
        Map<String, Object> epgUrls = new HashMap<>();
        epgInfo.forEach((key, value) -> {
            String tvName = (String) value;
            List<String> trs = null;
            try {
                trs = downloadFile(tvName);
            } catch (IOException e) {
                try {
                    // 异常后再试一次
                    trs = downloadFile(tvName);
                } catch (IOException e1) {
                    log.error("拉取URL列表时异常", e);
                }
            }
            List<String> tvUrls = getTvUrlsFromHtml(trs);
            if (tvUrls.size() > 0)
                epgUrls.put(key, tvUrls);
        });
        System.out.println(epgUrls);
        return epgUrls;
    }

    /**
     * 解析html，获取URL信息
     * 
     * @return
     */
    private List<String> getTvUrlsFromHtml(List<String> trs) {
        List<String> tvUrls = new ArrayList<>();
        boolean isTr = false;
        if (trs != null)
            for (String str : trs) {
                if (str.indexOf("<tr>") > -1)
                    isTr = true;
                if (str.indexOf("</tr>") > -1)
                    isTr = false;
                if (str.indexOf("(IPv6)") > -1)
                    isTr = false;
                if (isTr) {
                    int ln = str.indexOf("<a href=\"");
                    if (ln > -1) {
                        str = str.substring(ln + 1, str.indexOf("\" class"));
                        str = "https://epg.pw/stream/" + str.substring(str.lastIndexOf("/") + 1);
                        if (str.indexOf(".png") < 0 && str.indexOf(".jpg") < 0)
                            tvUrls.add(str);
                    }
                }
            }
        if (tvUrls.size() > 0)
            tvUrls = tvUrls.stream().distinct().collect(Collectors.toList());
        System.out.println(tvUrls);
        return tvUrls;
    }

    /**
     * 从远端下载文件
     * 
     * @throws IOException
     */
    private List<String> downloadFile(String tvName) throws IOException {
        String dlUrl = "https://epg.pw/livestreams/" + Base64.getEncoder().encodeToString(tvName.getBytes("UTF-8")) + ".html?lang=zh-hant";
        System.out.println(dlUrl);
        URL url = new URL(dlUrl);
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
        List<String> content = new ArrayList<>();
        boolean start = false;
        while ((inputLine = in.readLine()) != null) {
            if (inputLine.indexOf("<tbody>") > -1)
                start = true;
            if (inputLine.indexOf("</tbody>") > -1)
                start = false;
            if (start) {
                content.add(inputLine);
//                System.out.println(inputLine);
            }
        }

        in.close();
        conn.disconnect();
        return content;
    }

    /**
     * 读取模板
     * 
     * @param fileName
     * @return
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getJsonFile(String fileName) {
        String jsonStr = readFile(fileName);
        if (jsonStr != null) {
            return JSON.parseObject(jsonStr, HashMap.class);
        }
        return null;
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
