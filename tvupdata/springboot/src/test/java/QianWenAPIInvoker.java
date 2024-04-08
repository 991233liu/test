import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.alibaba.fastjson.JSONObject;

public class QianWenAPIInvoker {
    private static final String ENDPOINT = "http://20.206.3.45:21434/api/generate"; // 替换为你的本地API实际地址和端口
//    private static final String ACCESS_KEY = "your_access_key"; // 如果有认证需求，提供访问密钥

    public static void main(String[] args) throws Exception {
        System.out.println(UUID.randomUUID().toString().replaceAll("-", ""));
        System.out.println(UUID.randomUUID().toString().replaceAll("-", ""));
        System.out.println(UUID.randomUUID().toString().replaceAll("-", ""));
        System.out.println(UUID.randomUUID().toString().replaceAll("-", ""));
        System.out.println(UUID.randomUUID().toString().replaceAll("-", ""));
        System.out.println(UUID.randomUUID().toString().replaceAll("-", ""));
        System.out.println(UUID.randomUUID().toString().replaceAll("-", ""));
        one();
        two();
    }

    private static void one() throws Exception {
//      // 假设您已经正确设置和初始化了客户端（如ModelScopeClient或自定义HTTP客户端）
        //
//             // 构建请求参数，这里以JSON格式为例
//             String requestBody = "{"
//                 + "\"prompt\": \"您好，请问今天的天气如何？\","
//                 + // 其他可能的参数，比如对话历史、任务类型、生成长度等
//                 + "}";
        //
//             // 设置请求头，例如使用Access Key进行身份验证
//             Map<String, String> headers = new HashMap<>();
//             headers.put("Authorization", "Bearer your_access_key");  // 替换为您的实际访问密钥
//             headers.put("Content-Type", "application/json");
        //
//             // 发起POST请求
//             Response response = client.post("/path/to/api", requestBody, headers);
        //
//             // 处理响应结果
//             if (response.isSuccessful()) {
//                 String responseBody = response.body().string();
//                 // 解析JSON响应并获取AI的回答
//                 JSONObject jsonResult = new JSONObject(responseBody);
//                 String answer = jsonResult.getString("answer");
//                 System.out.println("千问的回答是：" + answer);
//             } else {
//                 int statusCode = response.code();
//                 String errorMessage = response.message();
//                 System.out.println("请求失败，状态码：" + statusCode + ", 错误信息：" + errorMessage);
//             }

        URL url = new URL(ENDPOINT);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        // 如果需要身份验证，则添加相关头信息
        // conn.setRequestProperty("Authorization", "Bearer " + ACCESS_KEY);

        conn.setDoOutput(true);
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(60000);
        int id = new Random().nextInt(1000);
        String str = "你是国中康健的虚拟健康管家，ID为" + id + "的客户，健康数据输入如下：2022年1月1日空腹血糖为6.5;2022年6月4日空腹血糖未7.8;2023年8月3日空腹血糖为9.1;2024年3月12日空腹血糖为10.1。\n" + "请使用如下格式做出回答：\n" + "0、健康状况概述\n"
                + "   XXXXXXXXXXXXXXXXXX\n" + "1、指标项介绍\n" + "    空腹血糖的含义是XXXXXXXXXXXXXXX，一般指标范围是XXXXXXXXXX，影响因子XXXXXXXXXXXXXXX\n" + "2、变化趋势及重点关注\n" + "    XXXXXXXXXXXXXXXX\n"
                + "3、生活注意事项\n" + "    XXXXXXXXXXXXXXX\n" + "4、饮食注意事项\n" + "    XXXXXXXXXXXXXXX\n" + "5、医学治疗方案推荐\n" + "    XXXXXXXXXXXXXXX\n" + "6、相关指标\n" + "    XXXXXXXXXXXXXX";
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("model", "qwen:7b-chat");
        map.put("prompt", str);
        map.put("stream", true);

        String jsonInputString = JSONObject.toJSONString(map);
        long start = System.currentTimeMillis();
        System.out.println(jsonInputString);
//                String jsonInputString = "{\"model\": \"qwen:14b-chat\",\"prompt\":\""+str+"\"}";
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Failed : HTTP error code : " + code);
        }

//                Scanner scanner = new Scanner(conn.getInputStream());
//                String responseJson = scanner.useDelimiter("\\A").next(); // 完整的JSON响应
//                System.out.println(responseJson);
        System.out.println(System.currentTimeMillis() - start);
        
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
            content.append(System.lineSeparator());
            System.out.println(inputLine);
        }

//                scanner.close();
        in.close();
        conn.disconnect();
        System.out.println(System.currentTimeMillis() - start);
    }

    private static void two() throws Exception {

        URL url = new URL("http://20.206.3.45:8083/assistant/prompt/generateV2");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        // 需要身份验证
        conn.setRequestProperty("Authorization", "Basic MWVmYjY5YjU3ZDM3NGExNmIyODBhNjJmMDA3YWRjMDY6NDAyZTEwZjgwNmY4NDNhNGFlMzYwYzQ4ZjlmYjVhMjU=");
        conn.setDoOutput(true);
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(60000);
        String str = "0、健康状况概述\n   XXXXXXXXXXXXXXXXXX\n1、指标项介绍\n    空腹血糖的含义是XXXXXXXXXXXXXXX，一般指标范围是XXXXXXXXXX，影响因子XXXXXXXXXXXXXXX\n2、变化趋势及重点关注\n    XXXXXXXXXXXXXXXX\n3、生活注意事项\n    XXXXXXXXXXXXXXX\n4、饮食注意事项\n    XXXXXXXXXXXXXXX\n5、医学治疗方案推荐\n    XXXXXXXXXXXXXXX\n6、相关指标\n    XXXXXXXXXXXXXX";
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("prompt", "健康数据如下：2022年1月1日空腹血糖为6.5;2022年6月4日空腹血糖未7.8;2023年8月3日空腹血糖为9.1;2024年3月12日空腹血糖为10.1。");
        map.put("answerTemplate", str);
        map.put("userType", "user");
        map.put("userId", "123456");
        map.put("appId", "abc");

        String jsonInputString = JSONObject.toJSONString(map);
        System.out.println(jsonInputString);
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

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
    }
}