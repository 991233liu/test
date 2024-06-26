package com.spring.txt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
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
    private Map<String, Object> ipInfos;

    @PostConstruct
    public void init() {
        baseFilePath = System.getProperty("user.dir") + "/txt";
        ipInfos = getJsonFile("ipInfo.json");
        if (ipInfos == null)
            ipInfos = new HashMap<>();
    }

    /**
     * 从远端下载文件
     * 
     * @throws IOException
     */
    public void download() throws IOException {

        URL url = new URL("https://mirror.ghproxy.com/raw.githubusercontent.com/ssili126/ds/main/ds.txt");
//        URL url = new URL("https://mirror.ghproxy.com/raw.githubusercontent.com/ssili126/tv/main/itvlist.txt");
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
    public boolean writeMyFile() throws IOException {
        File f = getFile("newFile.txt");
        String md5 = MD5.getFileMd5(f);
        System.out.println(md5);
        f = getFile(md5 + ".txt");
        if (f.exists()) { // 已经处理过就不要再处理了
            return false;
        }

        Map<String, Object> defUrl = getJsonFile("defUrl.json");
        Map<String, Object> template = getJsonFile("template.json");
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
        saveUrl2File(result, template, defUrl, "newUrl-d.txt", "河北", "电信");
        saveUrl2File(result, template, defUrl, "newUrl-l.txt", "河北", "联通");

        // 生成md5文件
        f.createNewFile();

        // 复制到git
        FileUtils.copyFile(getFile("newUrl-d.txt"), new File("d:/temp/TVBox/code/sub/live2/tv3hd.txt"));
        FileUtils.copyFile(getFile("newUrl-l.txt"), new File("d:/temp/TVBox/code/sub/live2/tv3hl.txt"));

        System.out.println("成功更新了一次！");
        return true;
    }

    /**
     * 更新git
     * 
     * @throws IOException
     * @throws GitAPIException
     * @throws NoFilepatternException
     */
    public void updataGit() throws IOException, NoFilepatternException, GitAPIException {
        // 打开本地仓库
        File gitDir = new File("d:/temp/TVBox/code/.git");
        Git git = Git.open(gitDir);
//        // 获取工作目录
//        git.checkout().setName("master").call(); // 切换到master分支，如果需要的话
        // 获取仓库状态
        Status status = git.status().call();
        // 检查是否有未追踪的文件
        boolean hasUntracked = !status.getUntracked().isEmpty();
        // 检查是否有已修改的文件（已跟踪但未添加到索引）
        boolean hasModified = !status.getModified().isEmpty();
        // 检查仓库状态，看是否有正在进行的事务（例如merge、rebase等）
        RepositoryState repositoryState = git.getRepository().getRepositoryState();
        boolean isInRebaseOrMerge = repositoryState == RepositoryState.REBASING || repositoryState == RepositoryState.MERGING || repositoryState == RepositoryState.CHERRY_PICKING
                || repositoryState == RepositoryState.BISECTING;
        // 根据检查结果决定是否执行提交
        if (!hasUntracked && !hasModified && !isInRebaseOrMerge) {
            System.out.println("No changes to commit.");
        } else {
            // 添加文件到索引（例如，添加所有未跟踪和已修改的文件）
            git.add().addFilepattern(".").call();
            // 设置提交者信息
            PersonIdent author = new PersonIdent("liulf", "991233liu@163.com");
            PersonIdent committer = author; // 通常情况下，作者和提交者是同一个人

            // 创建并执行提交
            RevCommit commit = git.commit().setMessage(new Date().toString()).setAuthor(author).setCommitter(committer).call();

            System.out.println("New Commit: " + commit.getName());

            // 创建凭据提供者
            String pd = FileUtils.readFileToString(getFile("password.txt"), "UTF-8");
            CredentialsProvider cp = new UsernamePasswordCredentialsProvider("991233liu@163.com", pd);
            // 创建并配置推送命令
            PushCommand pushCommand = git.push();
            pushCommand.setCredentialsProvider(cp);

            // 设置远程仓库URL和推送的分支
            pushCommand.setRemote("origin"); // origin 是默认的远程仓库名称，如果不是，请替换为你实际的远程仓库名称
            pushCommand.setRefSpecs(new RefSpec("refs/heads/master:refs/heads/master")); // 推送本地的master分支到远程的master分支

            // 执行推送操作
            pushCommand.call();
        }

        // 关闭连接
        git.close();
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
    private void saveUrl2File(Map<String, Object> newUrls, Map<String, Object> template, Map<String, Object> defUrl, String fileName, String diqu, String yunyingshang)
            throws IOException {
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
                    List<String> url = group.get(string);
                    // 根据IP信息排序
                    url = sortIpInfo(url, diqu, yunyingshang);
                    for (String string2 : url) {
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

        saveFile(fileName, content.toString());
    }

    /**
     * 根据ip信息排序
     * 
     * @return
     */
    private List<String> sortIpInfo(List<String> url, String diqu, String yunyingshang) {
        Collections.sort(url, new Comparator<String>() {
            @Override
            public int compare(String u1, String u2) {

                String ipInfo = getIpInfo(u1, 3);
                boolean isDianxin1 = false;
                boolean isHebei1 = false;
                if (ipInfo != null) {
                    if (ipInfo.indexOf(yunyingshang) > -1)
                        isDianxin1 = true;
                    if (ipInfo.indexOf(diqu) > -1)
                        isHebei1 = true;
                }
                ipInfo = getIpInfo(u2, 3);
                boolean isDianxin2 = false;
                boolean isHebei2 = false;
                if (ipInfo != null) {
                    if (ipInfo.indexOf(yunyingshang) > -1)
                        isDianxin2 = true;
                    if (ipInfo.indexOf(diqu) > -1)
                        isHebei2 = true;
                }
                if (isDianxin1 && isDianxin2) {
                    if (isHebei1)
                        return -1;
                    else if (isHebei2)
                        return 1;
                    return 0;
                } else if (isDianxin1) {
                    return -1;
                } else if (isDianxin2) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        return url;
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

    private String getIpInfo(String url, int reTry) {
        url = url.substring(url.indexOf("//") + 2, url.length());
        String ip = url.substring(0, url.indexOf(":"));
        if (!ipInfos.containsKey(ip)) {
            try {
                String ipInfo = getIpInfoFrom3W(ip);
                if (ipInfo != null) {
                    ipInfos.put(ip, ipInfo);
                    // 存入文件
                    saveFile("ipInfo.json", JSON.toJSONString(ipInfos));
                }
            } catch (IOException e) {
                if (reTry > 0)
                    getIpInfo(url, --reTry);
                else
                    e.printStackTrace();
            }
        }

        return (String) ipInfos.get(ip);
    }

    private String getIpInfoFrom3W(String ip) throws IOException {
        URL url = new URL("https://qifu-api.baidubce.com/ip/geo/v1/district?ip=" + ip);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
//        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0");

        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

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
            System.out.println(inputLine);
        }

        in.close();
        conn.disconnect();

        return content.toString();
    }
}
