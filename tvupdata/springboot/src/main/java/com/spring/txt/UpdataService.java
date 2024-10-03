package com.spring.txt;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

/**
 * 定时live更新列表
 *
 */
@Service
public class UpdataService implements CommandLineRunner {
    private static final Logger log = LogManager.getLogger(UpdataService.class);

    @Autowired
    GitHubService gitHubService;
    @Autowired
    EpgPwService epgPwService;

    @Value("${config.localGit}")
    private String localGitPath;
    private String baseFilePath;

    @PostConstruct
    public void init() {
        baseFilePath = System.getProperty("user.dir") + "/txt";
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run(String... args) throws Exception {
        boolean epgFlag = epgPwService.start();
//        gitHubService.download();
        boolean gitHubFlag = gitHubService.writeMyFile();
        if (epgFlag || gitHubFlag)
            updataGit();
        //
//        Map<String, Object> epgInfo = new HashMap<>();
//        gitHubService.template.forEach((key, value) -> {
//            ((Map<String, Object>) value).forEach((key2, value2) -> {
//                epgInfo.put(key2, key2);
//            });
//        });
//        Map<String, Object> epgUrls = epgPwService.downloadEpgUrls(epgInfo);
//        System.out.println(epgUrls);
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
        File gitDir = new File(localGitPath + "/.git");
        Git git = Git.open(gitDir);
//        // 获取工作目录
        git.checkout().setName("master").call(); // 切换到master分支，如果需要的话
        // 从远端更新git
        git.pull().call();
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
            log.info("No changes to commit.");
        } else {
            // 添加文件到索引（例如，添加所有未跟踪和已修改的文件）
            git.add().addFilepattern(".").call();
            // 设置提交者信息
            PersonIdent author = new PersonIdent("liulf", "991233liu@163.com");
            PersonIdent committer = author; // 通常情况下，作者和提交者是同一个人

            // 创建并执行提交
            RevCommit commit = git.commit().setMessage(new Date().toString()).setAuthor(author).setCommitter(committer).call();

            log.info("New Commit: {}", commit.getName());

            // 创建凭据提供者
            String pd = FileUtils.readFileToString(new File(baseFilePath + "/" + "password.txt"), "UTF-8");
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
}
