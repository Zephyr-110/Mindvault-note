package org.example.mindvaultaiservice.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.ai.JavaAndPythonContract.*;
import org.example.common.ai.client.PythonVectorClient;
import org.example.common.exception.BusinessException;
import org.example.common.logincheck.UserContext;
import org.example.mindvaultaiapi.dto.*;
import org.example.mindvaultaiapi.service.AIService;
import org.example.mindvaultaiapi.vo.HistoryVO;
import org.example.mindvaultaiapi.vo.SessionVO;
import org.example.common.ai.client.PythonAIChatClient;
import org.example.mindvaultaiservice.entity.AIChatHistory;
import org.example.mindvaultaiservice.entity.AIChatSession;
import org.example.mindvaultaiservice.mapper.AIChatHistoryMapper;
import org.example.mindvaultaiservice.mapper.AIChatSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIServiceImpl implements AIService {

    private final AIChatSessionMapper aiChatSessionMapper;
    private final AIChatHistoryMapper aiChatHistoryMapper;
    private final PythonAIChatClient pythonAIChatClient;
    private final PythonVectorClient pythonVectorClient;

    String TITLE_SYSTEM_PROMPT = "你是一个专业的会话标题生成器，你的任务是根据用户输入的关键词生成一个会话标题。";


    @Override
    public SseEmitter chat(ChatRequestJavaAPIDTO request) {
        return pythonAIChatClient.chatStream(
                toChatRequest(request, ""),
                fullContent -> {
                    saveMessage(request.getSessionId(), "assistant", fullContent);
                }
        );
    }

    @Override
    public ChatResponseDTO agentChat(ChatRequestJavaAPIDTO request) {
        createSession( request);
        // 用户消息落库
        saveMessage(request.getSessionId(), "user", request.getQuery());
        //拿到请求
        ChatRequestDTO chatRequestDTO = toChatRequest(request, "");
        // LLM生成响应
        ChatResponseDTO response = pythonAIChatClient.agentChat(chatRequestDTO);
        if(response == null || response.getContent() == null){
            throw new BusinessException(500, "LLM生成响应失败");
        }
        // 响应落库
        saveMessage(request.getSessionId(), "assistant", response.getContent());
        // 用户消息落向量数据库
        ToEmbeddingMemoriesDTO dto = new ToEmbeddingMemoriesDTO();
        dto.setId(UUID.randomUUID().toString());
        dto.setUserId(UserContext.getUserId().toString());
        dto.setSessionId(request.getSessionId());
        dto.setType("chat");
        dto.setText("user: " + request.getQuery()+ "\nassistant: " + response.getContent());
        dto.setTimestamp(System.currentTimeMillis() / 1000);
        try {
            setEmbeddingMemories(dto);
        } catch (Exception e) {
            log.error("落向量数据库失败", e);
        }
        return response;
    }

    private void createSession(ChatRequestJavaAPIDTO request) {
        if (request.getSessionId() == null) {
            //先创建会话（临时标题）
            AIChatSession session = new AIChatSession(null, UserContext.getUserId(),
                    "思考中...", "", LocalDateTime.now(), LocalDateTime.now());
            aiChatSessionMapper.insert(session);
            request.setSessionId(session.getId().toString());  // ← 设回去
            //再生成标题（此时 toChatRequest 能正常工作了）
            try {
                String title = pythonAIChatClient.chat(
                        toChatRequest(request, TITLE_SYSTEM_PROMPT)
                ).getContent();
                //更新标题
                aiChatSessionMapper.update(null, new LambdaUpdateWrapper<AIChatSession>()
                        .set(AIChatSession::getTitle, title)
                        .eq(AIChatSession::getId, session.getId()));
            } catch (Exception e) {
                log.error("生成标题失败，使用默认标题", e);
            }
            log.info("用户 {} 自动创建了一个新的会话", UserContext.getUserId());
        }
    }

    @Override
    public SseEmitter agentChatStream(ChatRequestJavaAPIDTO request) {
        // 冻结是否新会话
        final boolean isNewSession = request.getSessionId() == null;
        createSession(request);
        // 用户消息落库
        saveMessage(request.getSessionId(), "user", request.getQuery());
        // 提前捕获 userId，避免回调线程中 ThreadLocal 丢失
        Long userId = UserContext.getUserId();
        // 如果是新会话，构建 session_created 事件，等会传给客户端，否则那就传空，而客户端判空之后就不吐了
        String initialEvent = null;
        if (isNewSession) {
            String title = aiChatSessionMapper.selectById(Long.parseLong(request.getSessionId())).getTitle();
            initialEvent = String.format("{\"type\":\"session_created\",\"sessionId\":\"%s\",\"title\":\"%s\"}",
                    request.getSessionId(), title);
        }
        return pythonAIChatClient.agentChatStream(
                toChatRequest(request,  ""),
                fullContent -> {
                    saveMessage(request.getSessionId(), "assistant", fullContent, userId);
                    // 用户消息落向量数据库
                    ToEmbeddingMemoriesDTO dto = new ToEmbeddingMemoriesDTO();
                    dto.setId(UUID.randomUUID().toString());
                    dto.setUserId(userId.toString());
                    dto.setSessionId(request.getSessionId());
                    dto.setType("chat");
                    dto.setText("user: " + request.getQuery()+ "\nassistant: " + fullContent);
                    dto.setTimestamp(System.currentTimeMillis() / 1000);
                    try {
                        setEmbeddingMemories(dto);
                    } catch (Exception e) {
                        log.error("落向量数据库失败", e);
                    }
                },
                initialEvent
        );
    }

    private void setEmbeddingMemories(ToEmbeddingMemoriesDTO dto) {
        pythonVectorClient.toEmbeddingMemories(dto);
    }



    /**
     * 消息落库
     * @param sessionId 会话ID
     * @param role 角色
     * @param content 内容
     */
    private void saveMessage(String sessionId, String role, String content) {
        saveMessage(sessionId, role, content, UserContext.getUserId());
    }

    private void saveMessage(String sessionId, String role, String content, Long userId) {
        AIChatHistory history = new AIChatHistory();
        history.setSessionId(Long.parseLong(sessionId));
        history.setUserId(userId);
        history.setRole(role);
        history.setContent(content);
        history.setCreateTime(LocalDateTime.now());
        aiChatHistoryMapper.insert(history);
    }

    private ChatRequestDTO toChatRequest(ChatRequestJavaAPIDTO request, String systemPrompt) {
        // 查询会话是否存在
        AIChatSession session = aiChatSessionMapper.selectById(Long.parseLong(request.getSessionId()));
        // 提前创建消息列表
        List<ChatMessageDTO> messages = new ArrayList<>();
        // 提前创建系统消息
        ChatMessageDTO system;
        // 如果会话存在，则使用会话的摘要作为系统消息
        if (session != null) {
            system = new ChatMessageDTO("system", systemPrompt);
            // 更新会话时间
            updateUpdatedTime(request.getSessionId());
        } else {
            log.error("用户 {} 在不存在的会话 {}交流", UserContext.getUserId(), request.getSessionId());
            throw new BusinessException(500, "会话不存在");
        }
        // 创建用户消息
        ChatMessageDTO user = new ChatMessageDTO("user", request.getQuery());
        messages.add(system);
        messages.add(user);
        // 将拼接好了的列表返回
        return new ChatRequestDTO(messages, true, request.getSessionId(), UserContext.getUserId().toString());
    }

    /**
     * 更新会话时间
     * @param sessionId 会话ID
     */
    private void updateUpdatedTime(String sessionId) {
        aiChatSessionMapper.update(null,
                new LambdaUpdateWrapper<AIChatSession>()
                        .set(AIChatSession::getUpdateTime, LocalDateTime.now())
                        .eq(AIChatSession::getId, Long.parseLong(sessionId))
        );
    }

    @Override
    public boolean health() {
        return pythonAIChatClient.health();
    }

    @Override
    public List<SessionVO> listSession(ListSessionDTO dto) {
        return toSessionVO(dto.getPage(), dto.getSize());
    }

    /**
     * 分页查询会话，拼接返回值
     * @param page 页码
     * @param size 每页数量
     * @return 会话VO列表
     */
    private List<SessionVO> toSessionVO(Long page, Long size) {
        Page<AIChatSession> p = new Page<>(page, size);
        Page<AIChatSession> result = aiChatSessionMapper.selectPage(p, new LambdaQueryWrapper<AIChatSession>()
                .eq(AIChatSession::getUserId, UserContext.getUserId())
                .orderByDesc(AIChatSession::getUpdateTime));
        return result.getRecords().stream().map(history ->
                new SessionVO(history.getId(), history.getTitle(), history.getSummary(), history.getUpdateTime())).toList();
    }

    @Override
    public List<HistoryVO> listMessagesHistory(ListMessagesHistoryDTO dto) {
        Page<AIChatHistory> page = new Page<>(dto.getPage(), dto.getSize());
        Page<AIChatHistory> result = aiChatHistoryMapper.selectPage(page, new LambdaQueryWrapper<AIChatHistory>()
                .eq(AIChatHistory::getSessionId, dto.getSessionId())
                .orderByAsc(AIChatHistory::getCreateTime));
        return result.getRecords().stream().map(history ->
                new HistoryVO(history.getRole(), history.getContent(), history.getCreateTime())).toList();
    }

    @Override
    public void createSession() {
        log.info("用户 {} 创建了一个新的会话", UserContext.getUserId());
        AIChatSession session = new AIChatSession(null, UserContext.getUserId(), "无标题会话", "", LocalDateTime.now(), LocalDateTime.now());
        aiChatSessionMapper.insert(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SessionVO> deleteSession(DeleteSessionDTO dto) {
        log.info("用户 {} 删除了会话 {}以及其所有消息", UserContext.getUserId(), dto.getSessionId());
        aiChatHistoryMapper.delete(new LambdaQueryWrapper<AIChatHistory>()
                .eq(AIChatHistory::getSessionId, dto.getSessionId()));
        aiChatSessionMapper.deleteById(dto.getSessionId());
        try {
            pythonVectorClient.deleteMemoriesEmbedding(new DeleteMemoriesEmbeddingDTO(dto.getSessionId().toString(), UserContext.getUserId().toString()));
        } catch (Exception e) {
            log.error("删除会话 {} 的向量失败: {}", dto.getSessionId(), e.getMessage());
        }
        return toSessionVO(dto.getPage(), dto.getSize());
    }

    @Override
    public List<SessionVO> updateSessionTitle(UpdateSessionTitleDTO dto) {
        log.info("用户 {} 更新了会话 {} 的标题为 {}", UserContext.getUserId(), dto.getSessionId(), dto.getNewTitle());
        aiChatSessionMapper.update(null,
                new LambdaUpdateWrapper<AIChatSession>()
                        .set(AIChatSession::getTitle, dto.getNewTitle())
                        .eq(AIChatSession::getId, dto.getSessionId())
                        .eq(AIChatSession::getUserId, UserContext.getUserId())
        );
        return toSessionVO(dto.getPage(), dto.getSize());
    }
}