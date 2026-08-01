package org.example.mindvaultaiapi.service;

import jakarta.validation.Valid;
import org.example.mindvaultaiapi.dto.*;
import org.example.common.ai.JavaAndPythonContract.ChatResponseDTO;
import org.example.mindvaultaiapi.vo.HistoryVO;
import org.example.mindvaultaiapi.vo.SessionVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AIService {

    SseEmitter chat(ChatRequestJavaAPIDTO request);

    ChatResponseDTO agentChat(@Valid ChatRequestJavaAPIDTO request);

    SseEmitter agentChatStream(@Valid ChatRequestJavaAPIDTO request);

    boolean health();

    List<SessionVO> listSession(ListSessionDTO dto);

    List<HistoryVO> listMessagesHistory(ListMessagesHistoryDTO dto);

    void createSession();

    List<SessionVO> deleteSession(DeleteSessionDTO dto);

    List<SessionVO> updateSessionTitle(@Valid UpdateSessionTitleDTO dto);
}
