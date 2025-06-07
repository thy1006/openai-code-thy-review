package plus.gaga.middleware;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.Test;
import plus.gaga.middleware.sdk.domain.model.ChatCompletionRequest;
import plus.gaga.middleware.sdk.domain.model.ChatCompletionSyncResponse;
import plus.gaga.middleware.sdk.domain.model.Message;
import plus.gaga.middleware.sdk.domain.model.Model;
import plus.gaga.middleware.sdk.types.utils.BearerTokenUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {
    public static void main(String[] args) {
        String apiKeySecret = "4d37d6ee89a1414189ee173641d67b58.KsBDOJUF7TEhvtE4";
        String token = BearerTokenUtils.getToken(apiKeySecret);
        System.out.println(token);
    }

    @Test
    public void test_http() throws IOException {
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

        String code = "1+1";
        //构造 JSON 请求体（问题 + 代码）
        String jsonInpuString = "{"
                + "\"model\":\"glm-4-flash\","
                + "\"messages\": ["
                + "    {"
                + "        \"role\": \"user\","
                + "        \"content\": \"你是一个高级编程架构师，精通各类场景方案、架构设计和编程语言请，请您根据git diff记录，对代码做出评审。代码为: " + code + "\""
                + "    }"
                + "]"
                + "}";

        //发送请求体，写入 JSON 数据到 HTTP 请求体中。
        try(OutputStream os = connection.getOutputStream()){
            byte[] input = jsonInpuString.getBytes(StandardCharsets.UTF_8);
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
        ChatCompletionSyncResponse response = JSON.parseObject(content.toString(), ChatCompletionSyncResponse.class);
        System.out.println(response.getChoices().get(0).getMessage().getContent());

    }

}
