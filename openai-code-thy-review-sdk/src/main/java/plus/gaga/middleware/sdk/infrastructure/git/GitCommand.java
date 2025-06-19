package plus.gaga.middleware.sdk.infrastructure.git;

import com.sun.deploy.util.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plus.gaga.middleware.sdk.types.utils.RandomStringUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class GitCommand {
    private final Logger logger = LoggerFactory.getLogger(GitCommand.class);

    private final String githubReviewLogUri;

    private final String githubToken;

    private final String project;

    private final String branch;

    private final String author;

    private final String message;

    public GitCommand(String githubReviewLogUri, String githubToken, String project, String branch, String author, String message) {
        this.githubReviewLogUri = githubReviewLogUri;
        this.githubToken = githubToken;
        this.project = project;
        this.branch = branch;
        this.author = author;
        this.message = message;
    }

    public String diff() throws IOException, InterruptedException {
        // openai.itedus.cn
        //获取最新提交的 Hash 值：
        ProcessBuilder logProcessBuilder = new ProcessBuilder("git", "log", "-1", "--pretty=format:%H");
        logProcessBuilder.directory(new File("."));
        Process logProcess = logProcessBuilder.start();
        //读取输出并获取最新提交的哈希值
        BufferedReader logRead = new BufferedReader(new InputStreamReader(logProcess.getInputStream()));
        String latestCommitHash = logRead.readLine();
        logRead.close();
        //等待进程执行完成
        logProcess.waitFor();

        //获取最新提交与其前一个提交之间的差异：
        ProcessBuilder diffProcessBuilder = new ProcessBuilder("git", "diff", latestCommitHash + "^", latestCommitHash);
        diffProcessBuilder.directory(new File("."));
        Process diffProcess = diffProcessBuilder.start();
        //读取并构建差异内容：
        StringBuilder diffCode = new StringBuilder();
        BufferedReader diffReader = new BufferedReader(new InputStreamReader(diffProcess.getInputStream()));
        String line;
        while ((line = diffReader.readLine()) != null) {
            diffCode.append(line);
        }
        diffReader.close();
        int exitCode = diffProcess.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Failed to get diff, exit code:" + exitCode);
        }
        //返回差异内容：
        return diffCode.toString();
    }

    public String commitAndPush(String recommend) throws Exception {
        //克隆仓库：
        Git git = Git.cloneRepository()
                .setURI(githubReviewLogUri + ".git") // 仓库的 URI 地址
                .setDirectory(new File("repo"))   // 本地目录路径
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubToken, ""))  // 使用 token 进行身份验证
                .call(); // 克隆仓库
        //创建分支
        String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        File dateFolder = new File("repo/" + dateFolderName);
        if (!dateFolder.exists()) {
            dateFolder.mkdir(); // 如果文件夹不存在，则创建
        }

        //创建日期文件夹：
        String fileName = project + "-" + branch + "-" + author + System.currentTimeMillis() + "-" + RandomStringUtils.randomNumeric(4) + ".md";
        File newFile = new File(dateFolder, fileName);
        try (FileWriter writer = new FileWriter(newFile)) {
            writer.write(recommend); // 将传入的建议写入文件
        }
        //创建新文件并写入内容：
        git.add().addFilepattern(dateFolderName + "/" + fileName).call();  // 添加新文件到 git 索引
        git.commit().setMessage("add code review new file" + fileName).call(); // 提交更改
        // // 推送提交到 GitHub 远程仓库
        git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubToken, "")).call();
        logger.info("openai-code-review git commit and push done! {}", fileName);
        // 返回文件的 GitHub 链接
        return githubReviewLogUri + "/blob/master/" + dateFolderName + "/" + fileName;

    }

    public Logger getLogger() {
        return logger;
    }

    public String getGithubReviewLogUri() {
        return githubReviewLogUri;
    }

    public String getGithubToken() {
        return githubToken;
    }

    public String getProject() {
        return project;
    }

    public String getBranch() {
        return branch;
    }

    public String getAuthor() {
        return author;
    }

    public String getMessage() {
        return message;
    }
}
