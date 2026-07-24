package org.example.mindvaultaiapi.service;

import org.example.mindvaultaiapi.dto.*;
import org.example.mindvaultaiapi.vo.HistoryVO;
import org.example.mindvaultaiapi.vo.SessionVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AIService {

    SseEmitter chat(ChatRequestJavaAPIDTO request);

    boolean health();

    List<SessionVO> listSession(ListSessionDTO dto);

    List<HistoryVO> listMessagesHistory(ListMessagesHistoryDTO dto);

    void createSession();

    List<SessionVO> deleteSession(DeleteSessionDTO dto);
}
