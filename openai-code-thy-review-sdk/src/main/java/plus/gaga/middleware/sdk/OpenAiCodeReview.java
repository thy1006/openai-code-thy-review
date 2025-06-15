package plus.gaga.middleware.sdk;

import com.alibaba.fastjson2.JSON;
import com.sun.org.apache.bcel.internal.generic.NEW;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import plus.gaga.middleware.sdk.domain.model.ChatCompletionRequest;
import plus.gaga.middleware.sdk.domain.model.ChatCompletionSyncResponse;
import plus.gaga.middleware.sdk.domain.model.Message;
import plus.gaga.middleware.sdk.domain.model.Model;
import plus.gaga.middleware.sdk.types.utils.BearerTokenUtils;
import plus.gaga.middleware.sdk.types.utils.WXAccessTokenUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class OpenAiCodeReview {

    public static void main(String[] args) throws Exception {

        System.out.println("测试执行");

        String token = System.getenv("GITHUB_TOKEN");
        if (null == token || token.isEmpty()) {
            throw new RuntimeException("token is null");
        }
        //1.代码检出
        ProcessBuilder processBuilder = new ProcessBuilder("git", "diff", "HEAD~1", "HEAD");
        processBuilder.directory(new File("."));
        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        StringBuilder diffCode = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            diffCode.append(line);
        }
        int exitCode = process.waitFor();
        System.out.println("Exited with code:" + exitCode);
        System.out.println("diff code：" + diffCode.toString());

        // 2. chatglm 代码评审
        String log = codeReview(diffCode.toString());
        System.out.println("code review：" + log);

        // 3. 写入评审日志
        String logUrl = writeLog(token, log);
        System.out.println("writeLog：" + logUrl);

        //4. 消息推送
        System.out.println("pushMessage：" + logUrl);
        pushMessage(logUrl);
    }

    private static void pushMessage(String logUrl) {
        String accessToken = WXAccessTokenUtils.getAccessToken();
        System.out.println(accessToken);

        Message message = new Message();
        message.put("project","big-market-thy");
        message.put("review",logUrl);
        message.setUrl(logUrl);
        String url =String.format("https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=%s",accessToken);
        sendPostRequest(url,JSON.toJSONString(message));
    }

    private static void sendPostRequest(String urlString, String jsonBody) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
                String response = scanner.useDelimiter("\\A").next();
                System.out.println(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String codeReview(String diffCode) throws IOException {
        String apiKeySecret = "4d37d6ee89a1414189ee173641d67b58.KsBDOJUF7TEhvtE4";
        //步骤一：获取 Token
        String token = BearerTokenUtils.getToken(apiKeySecret);
        //第二步：配置 HTTP 请求
        //配置请求头，模拟浏览器行为使用 POST 方法发送 JSON 请求体setDoOutput(true) 表示要写入 body 数据
        URL url = new URL("https://open.bigmodel.cn/api/paas/v4/chat/completions");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        //设置请求头 + 方法 + 输出流
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
        connection.setDoOutput(true);
        //构造请求体（JSON）
        ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest();
        chatCompletionRequest.setModel(Model.GLM_4_FLASH.getCode());

//        List<ChatCompletionRequest.Prompt> prompts = new ArrayList<>();
//        prompts.add(new ChatCompletionRequest.Prompt("user", "你是一个高级编程架构师..."));
//        prompts.add(new ChatCompletionRequest.Prompt("user", diffCode));
//        chatCompletionRequest.setMessages(prompts);

        chatCompletionRequest.setMessages(new ArrayList<ChatCompletionRequest.Prompt>() {
            private static final long serialVersionUID = -7988151926241837899L;

            {
                add(new ChatCompletionRequest.Prompt("user", "你是一个高级编程架构师，精通各类场景方案、架构设计和编程语言请，请您根据git diff记录，对代码做出评审。代码如下:"));
                add(new ChatCompletionRequest.Prompt("user", diffCode));
            }
        });
        //发送请求体数据
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = JSON.toJSONString(chatCompletionRequest).getBytes(StandardCharsets.UTF_8);
            os.write(input);
        }
        //读取响应并解析，打印 HTTP 状态码读取 JSON 响应文本
        int responseCode = connection.getResponseCode();
        System.out.println(responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;

        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }

        in.close();
        connection.disconnect();
        //将返回结果解析为 ChatCompletionSyncResponse 对象（需要提前定义对应 Java 类或引入 SDK）。
        //输出模型生成的代码审查内容。解析响应为对象
        System.out.println("评审结果：" + content.toString());
        ChatCompletionSyncResponse response = JSON.parseObject(content.toString(), ChatCompletionSyncResponse.class);

        return response.getChoices().get(0).getMessage().getContent();
    }

    private static String writeLog(String token, String log) throws Exception {
        Git git = Git.cloneRepository().setURI("https://github.com/thy1006/openai-code-review-log.git")
                .setDirectory(new File("repo"))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                .call();
        String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        File dateFolder = new File("repo/" + dateFolderName);
        if (!dateFolder.exists()) {
            dateFolder.mkdir();
        }
        String fileName = generateRandomString(12) + ".md";
        File newFile = new File(dateFolder, fileName);
        try(FileWriter writer = new FileWriter(newFile)){
            writer.write(log);
        }
        git.add().addFilepattern(dateFolderName+"/"+fileName).call();
        git.commit().setMessage("Add new file via GitHub Actions").call();
        git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(token,"")).call();
        System.out.println("Changes have been pushed to the repository.");
        return "https://github.com/thy1006/openai-code-review-log/blob/master/" + dateFolderName + "/" + fileName;
    }
    private static String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return sb.toString();
    }

}
