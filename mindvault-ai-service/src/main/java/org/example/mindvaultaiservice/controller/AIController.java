package org.example.mindvaultaiservice.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.common.result.Result;
import org.example.mindvaultaiapi.dto.*;
import org.example.common.ai.JavaAndPythonContract.ChatResponseDTO;
import org.example.mindvaultaiapi.service.AIService;
import org.example.mindvaultaiapi.vo.HistoryVO;
import org.example.mindvaultaiapi.vo.SessionVO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequestJavaAPIDTO request) {
        return aiService.chat(request);
    }

    @PostMapping("/chat/agent-chat")
    public Result<ChatResponseDTO> agentChat(@Valid @RequestBody ChatRequestJavaAPIDTO request) {
        return Result.success(aiService.agentChat(request));
    }

    @PostMapping("/chat/stream-agent-chat")
    public SseEmitter chatStreamAgentChat(@Valid @RequestBody ChatRequestJavaAPIDTO request) {
        return aiService.agentChatStream(request);
    }


    @GetMapping("/health")
    public Result<Boolean> health() {
        return Result.success(aiService.health());
    }

    @PostMapping("create-session")
    public Result<?> createSession() {
        aiService.createSession();
        return Result.success();
    }

    @DeleteMapping("/delete-session")
    public Result<List<SessionVO>> deleteSession(@Valid @RequestBody DeleteSessionDTO dto) {
        return Result.success(aiService.deleteSession(dto));
    }

    @PutMapping("/update-session-title")
    public Result<List<SessionVO>> updateSessionTitle(@Valid @RequestBody UpdateSessionTitleDTO dto) {
        return Result.success(aiService.updateSessionTitle(dto));
    }

    @GetMapping("/list-session")
    public Result<List<SessionVO>> listSession(@Valid ListSessionDTO dto) {
        return Result.success(aiService.listSession(dto));
    }

    @GetMapping("/list-messages-history")
    public Result<List<HistoryVO>> listMessagesHistory(@Valid ListMessagesHistoryDTO dto) {
        return Result.success(aiService.listMessagesHistory(dto));
    }

}
