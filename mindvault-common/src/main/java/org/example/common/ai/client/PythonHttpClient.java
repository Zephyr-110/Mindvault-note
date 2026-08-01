package org.example.common.ai.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class PythonHttpClient {

    @Value("${ai.python.url:http://localhost:8000}")
    private String pythonBaseUrl;

    //JSON序列化工具，可以将Java对象序列化为JSON字符串
    public static final ObjectMapper mapper = new ObjectMapper();

    public <T> T toJson(String path, Object body, Class<T> responseType) {
        String json ;
        //这里可能会报序列化异常
        try {
            //将DTO对象序列化为JSON字符串
            json = mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new BusinessException(400, "JSON序列化失败");
        }
        //初始化HTTP连接对象
        HttpURLConnection connection = null;
        try {
            //把字符串变成URI对象
            URI uri = URI.create(pythonBaseUrl + path);
            //打开到 Python 服务器的连接
            connection = (HttpURLConnection) uri.toURL().openConnection();
            //设置请求方法为POST
            connection.setRequestMethod("POST");
            //说明请求体是json
            connection.setRequestProperty("Content-Type", "application/json");
            // 允许输出请求体数据
            connection.setDoOutput(true);
            //设置连接超时为10秒
            connection.setConnectTimeout(10000);
            //设置读取超时为5分钟
            connection.setReadTimeout(300000);
            //获取连接对象的输出流，用于向python服务器发送请求体
            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            //获取响应状态码
            int status = connection.getResponseCode();
            if(status != 200) {
                throw new BusinessException(status, "Python服务返回错误: HTTP " + status);
            }
            //获取响应体数据
            StringBuilder sb = new StringBuilder();
            try(BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            //反序列化JSON字符串为指定类型对象
            return mapper.readValue(sb.toString(), responseType);
        } catch (IOException e) {
            log.error("Python API 请求失败, path: {}", path, e);
            throw new BusinessException(400, "Python客户端HTTP请求异常");
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 发送POST请求到Python服务，不需要返回值
     */
    public void requestJson(String path, Object body, String requestMethod) {
        String json;
        try {
            json = mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new BusinessException(400, "JSON序列化失败");
        }
        HttpURLConnection connection = null;
        try {
            URI uri = URI.create(pythonBaseUrl + path);
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod(requestMethod);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(300000);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            int status = connection.getResponseCode();
            if (status != 200) {
                throw new BusinessException(status, "Python服务返回错误: HTTP " + status);
            }
        } catch (IOException e) {
            log.error("Python API 请求失败, path: {}", path, e);
            throw new BusinessException(400, "Python客户端HTTP请求异常");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 调用Python AI服务进行对话
     * @param path 调用方写死的路径，就是调用python服务接口的路径
     * @param body 各种DTO参数
     * @return SseEmitter 流式链接
     */

    public SseEmitter toStream(String path, Object body, Consumer<String> onComplete){
        String json ;
        //这里可能会报序列化异常
        try {
            //将DTO对象序列化为JSON字符串
            json = mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(e);
            //返回异常的SseEmitter对象
            return emitter;
        }
        // 创建一个SseEmitter对象， 设置超时时间为5分钟
        SseEmitter emitter = new SseEmitter(300000L);
        // 开新线程，异步调用Python AI服务
        new Thread(() -> {
            // 初始化HTTP连接对象
            HttpURLConnection connection = null;
            try {
                // 把字符串变成URI对象
                URI uri = URI.create(pythonBaseUrl + path);
                // 打开到 Python 服务器的连接
                connection = (HttpURLConnection) uri.toURL().openConnection();
                // 设置请求方法为POST
                connection.setRequestMethod("POST");
                // 说明请求体是json
                connection.setRequestProperty("Content-Type", "application/json");
                // 允许输出请求体数据
                connection.setDoOutput(true);
                // 设置连接超时时间为10秒
                connection.setConnectTimeout(10000);
                // 设置读取超时为5分钟
                connection.setReadTimeout(300000);
                // 获取连接对象的输出流，用于向python服务器发送请求体
                try (OutputStream os = connection.getOutputStream()) {
                    // 向python服务器发送json数据
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                    // 把缓冲区的数据推出去
                    os.flush();
                }
                // 获取响应状态码
                int status = connection.getResponseCode();
                if(status != 200) {
                    // 不是响应成功的状态码，直接返回错误返回值
                    emitter.send(SseEmitter.event()
                            .data("{\"error\": \"Python 返回错误: HTTP " + status + "\"}"));
                    emitter.complete();
                    return;
                }
                // 能走到这里说明前面几关过了，开始准备接收python服务器响应过来的数据
                try (BufferedReader reader = new BufferedReader(
                        //获取输入流，读取python的响应
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    StringBuilder bucket = new StringBuilder();
                    // 开始一行一行读取数据
                    while ((line = reader.readLine()) != null) {
                        // 直到data: 开头的那一行，我们接收起来，然后让SseEmitter对象发送
                        if(line.startsWith("data: ")){
                            String data = line.substring(6);
                            bucket.append(data);
                            emitter.send(SseEmitter.event().data(data));
                        }
                    }

                    emitter.onCompletion(() -> onComplete.accept(bucket.toString()));
                }
                // 完成
                emitter.complete();
            } catch (IOException e) {
                // 到这里抛异常，说明python服务请求失败
                log.error("Python API 请求失败", e);
                try {
                    // 发送错误信息
                    emitter.send(SseEmitter.event().data("{\"error\": \"Python API 请求失败: " + e.getMessage() + "\"}"));
                    // 完成
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            } finally {
                //无论如何，都关闭连接
                if(connection != null) {
                    // 关闭连接，释放资源
                    connection.disconnect();
                }
            }
        }).start();
        return emitter;
    }

    public SseEmitter toStreamAgent(String path, Object body, Consumer<String> onComplete, String initialEvent){
        String json ;
        //这里可能会报序列化异常
        try {
            //将DTO对象序列化为JSON字符串
            json = mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(e);
            //返回异常的SseEmitter对象
            return emitter;
        }
        // 创建一个SseEmitter对象， 设置超时时间为5分钟
        SseEmitter emitter = new SseEmitter(300000L);
        // 开新线程，异步调用Python AI服务
        new Thread(() -> {
            // 初始化HTTP连接对象
            HttpURLConnection connection = null;
            try {
                // 把字符串变成URI对象
                URI uri = URI.create(pythonBaseUrl + path);
                // 打开到 Python 服务器的连接
                connection = (HttpURLConnection) uri.toURL().openConnection();
                // 设置请求方法为POST
                connection.setRequestMethod("POST");
                // 说明请求体是json
                connection.setRequestProperty("Content-Type", "application/json");
                // 允许输出请求体数据
                connection.setDoOutput(true);
                // 设置连接超时时间为10秒
                connection.setConnectTimeout(10000);
                // 设置读取超时为5分钟
                connection.setReadTimeout(300000);
                // 获取连接对象的输出流，用于向python服务器发送请求体
                try (OutputStream os = connection.getOutputStream()) {
                    // 向python服务器发送json数据
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                    // 把缓冲区的数据推出去
                    os.flush();
                }
                // 获取响应状态码
                int status = connection.getResponseCode();
                if(status != 200) {
                    // 不是响应成功的状态码，直接返回错误返回值
                    emitter.send(SseEmitter.event()
                            .data("{\"error\": \"Python 返回错误: HTTP " + status + "\"}"));
                    emitter.complete();
                    return;
                }
                // 能走到这里说明前面几关过了，开始准备接收python服务器响应过来的数据
                try (BufferedReader reader = new BufferedReader(
                        //获取输入流，读取python的响应
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    StringBuilder bucket = new StringBuilder();
                    // 发送初始事件（如 session_created）
                    if (initialEvent != null && !initialEvent.isEmpty()) {
                        emitter.send(SseEmitter.event().data(initialEvent));
                    }
                    // 开始一行一行读取数据
                    while ((line = reader.readLine()) != null) {
                        // 直到data: 开头的那一行，我们接收起来，然后让SseEmitter对象发送
                        if(line.startsWith("data: ")){
                            // 截取到data: 后面的数据
                            String data = line.substring(6);
                            // 把响应的json数据解析为JsonNode对象
                            JsonNode node = mapper.readTree(data);
                            // 获取type字段的value，并转化为文本字符串赋值给type
                            String type = node.get("type").asText();
                            // 把状态事件发送给客户端
                            emitter.send(SseEmitter.event().data(data));
                            // 如果type为content，则说明是最终回复的片段，加入到bucket中，用于落库
                            if("content".equals( type)){
                                bucket.append(node.get("text").asText());
                            }
                            // 如果type为done，则说明回复已经完成，直接返回给客户端
                            if("done".equals(type)){
                                JsonNode fc = node.get("full_content");
                                onComplete.accept(fc != null ? fc.asText() : bucket.toString());
                            }
                        }
                    }
                }
                // 完成
                emitter.complete();
            } catch (IOException e) {
                // 到这里抛异常，说明python服务请求失败
                log.error("Python API 请求失败", e);
                try {
                    // 发送错误信息
                    emitter.send(SseEmitter.event().data("{\"error\": \"Python API 请求失败: " + e.getMessage() + "\"}"));
                    // 完成
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            } finally {
                //无论如何，都关闭连接
                if(connection != null) {
                    // 关闭连接，释放资源
                    connection.disconnect();
                }
            }
        }).start();
        return emitter;
    }
}