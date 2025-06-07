package plus.gaga.middleware.sdk;

import com.alibaba.fastjson2.JSON;
import com.sun.org.apache.bcel.internal.generic.NEW;
import plus.gaga.middleware.sdk.domain.model.ChatCompletionRequest;
import plus.gaga.middleware.sdk.domain.model.ChatCompletionSyncResponse;
import plus.gaga.middleware.sdk.domain.model.Model;
import plus.gaga.middleware.sdk.types.utils.BearerTokenUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class OpenAiCodeReview {

    public static void main(String[] args) throws Exception {
        System.out.println("测试执行");
        //1.代码检出
        ProcessBuilder processBuilder = new ProcessBuilder("git", "diff", "HEAD~1", "HEAD");
        processBuilder.directory(new File("."));
        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        StringBuilder diffCode = new StringBuilder();

        while ((line=reader.readLine())!=null){
            diffCode.append(line);
        }
        int exitCode = process.waitFor();
        System.out.println("Exited with code:" + exitCode);
        System.out.println("diff code：" + diffCode.toString());

        // 2. chatglm 代码评审
        String log = codeReview(diffCode.toString());
        System.out.println("code review：" + log);
    }

    private static String codeReview(String diffCode) throws IOException {
        String apiKeySecret = "4d37d6ee89a1414189ee173641d67b58.KsBDOJUF7TEhvtE4";
        //步骤一：获取 Token
        String token = BearerTokenUtils.getToken(apiKeySecret);
        //配置请求头，模拟浏览器行为使用 POST 方法发送 JSON 请求体setDoOutput(true) 表示要写入 body 数据
        URL url = new URL("https://open.bigmodel.cn/api/paas/v4/chat/completions");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
        connection.setDoOutput(true);

        ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest();
        chatCompletionRequest.setModel(Model.GLM_4_FLASH.getCode());
        chatCompletionRequest.setMessages(new ArrayList<ChatCompletionRequest.Prompt>() {
            private static final long serialVersionUID = -7988151926241837899L;

            {
                add(new ChatCompletionRequest.Prompt("user", "你是一个高级编程架构师，精通各类场景方案、架构设计和编程语言请，请您根据git diff记录，对代码做出评审。代码如下:"));
                add(new ChatCompletionRequest.Prompt("user", diffCode));
            }
        });

        try(OutputStream os = connection.getOutputStream()){
            byte[] input = JSON.toJSONString(chatCompletionRequest).getBytes(StandardCharsets.UTF_8);
            os.write(input);
        }
        //读取响应并解析，打印 HTTP 状态码读取 JSON 响应文本
        int responseCode = connection.getResponseCode();
        System.out.println(responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;

        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null){
            content.append(inputLine);
        }

        in.close();
        connection.disconnect();
        //将返回结果解析为 ChatCompletionSyncResponse 对象（需要提前定义对应 Java 类或引入 SDK）。
        //输出模型生成的代码审查内容。
        System.out.println("评审结果：" + content.toString());
        ChatCompletionSyncResponse response = JSON.parseObject(content.toString(), ChatCompletionSyncResponse.class);

        return response.getChoices().get(0).getMessage().getContent();
    }

}
