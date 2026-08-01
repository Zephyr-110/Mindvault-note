package org.example.common.ai.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.ai.JavaAndPythonContract.ChatRequestDTO;
import org.example.common.ai.JavaAndPythonContract.ChatResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class PythonAIChatClient {

    @Value("${ai.python.url:http://localhost:8000}")
    private String pythonBaseUrl;

    private final PythonHttpClient pythonHttpClient;



    public ChatResponseDTO chat(ChatRequestDTO request) {
        return pythonHttpClient.toJson("/api/ai/chat", request, ChatResponseDTO.class);
    }

    public SseEmitter chatStream(ChatRequestDTO request,  Consumer<String> onComplete) {
        return pythonHttpClient.toStream("/api/ai/stream_chat", request, onComplete);
    }


    public ChatResponseDTO agentChat(ChatRequestDTO request) {
        return pythonHttpClient.toJson("/api/ai/agent_chat", request, ChatResponseDTO.class);
    }

    public SseEmitter agentChatStream(ChatRequestDTO request,  Consumer<String> onComplete, String initialEvent) {
        return pythonHttpClient.toStreamAgent("/api/ai/stream_agent_chat", request, onComplete, initialEvent);
    }

    public boolean health() {
        HttpURLConnection connection = null;
        try {
            URI uri = URI.create(pythonBaseUrl + "/api/ai/health");
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int status = connection.getResponseCode();
            return status == 200;
        } catch (IOException e) {
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }



}