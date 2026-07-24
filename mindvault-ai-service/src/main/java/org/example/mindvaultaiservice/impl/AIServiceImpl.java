package org.example.mindvaultaiservice.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BusinessException;
import org.example.common.logincheck.UserContext;
import org.example.community.post.entity.Post;
import org.example.community.post.mapper.PostMapper;
import org.example.mindvaultaiapi.dto.*;
import org.example.mindvaultaiapi.service.AIService;
import org.example.mindvaultaiapi.vo.HistoryVO;
import org.example.mindvaultaiapi.vo.SessionVO;
import org.example.mindvaultaiservice.client.PythonAIClient;
import org.example.mindvaultaiservice.entity.AIChatHistory;
import org.example.mindvaultaiservice.entity.AIChatSession;
import org.example.mindvaultaiservice.mapper.AIChatHistoryMapper;
import org.example.mindvaultaiservice.mapper.AIChatSessionMapper;
import org.example.note.document.entity.Document;
import org.example.note.document.mapper.DocumentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIServiceImpl implements AIService {

    private final AIChatSessionMapper aiChatSessionMapper;
    private final AIChatHistoryMapper aiChatHistoryMapper;
    private final PostMapper postMapper;
    private final DocumentMapper documentMapper;
    private final PythonAIClient pythonAIClient;

    private final String INTENTION_CHECK_PROMPT = """
        你是一个意图检测器。判断用户输入是否需要搜索笔记或社区帖子。
        
        规则：
        - 用户询问个人笔记、知识库、历史记录相关 → 需要搜索
        - 用户询问社区讨论、他人分享的帖子 → 需要搜索
        - 普通闲聊、问候、常识问题 → 不需要搜索
        
        你必须只返回一行 JSON，不要任何其他文字：
        - 需要搜索：{"search":true,"query":"提取的关键词"}
        - 不需要搜索：{"search":false}
        """;

    private final String GENERATE_SUMMARY_PROMPT = """
        你是一个知识库总结生成器。
        
        你的任务是生成一个总结，用于总结前文的摘要，要达到使人一眼就能知道前文都说了什么，使得对话有逻辑的进行下去。
        
        总结应该包含以下内容：
        - 笔记或社区帖子的内容
        - 笔记或社区帖子的结构
        - 笔记或社区帖子的关系
        - 笔记或社区帖子的适用场景
        """;

    @Override
    public SseEmitter chat(ChatRequestJavaAPIDTO request) {
        // 意图检测
        ChatMessageDTO checkSystem = new ChatMessageDTO("system", INTENTION_CHECK_PROMPT);
        ChatMessageDTO checkUser = new ChatMessageDTO("user", request.getQuery());
        ChatRequestDTO intentionCheckRequest = new ChatRequestDTO(List.of(checkSystem, checkUser), false, UserContext.getUserId().toString());
        ChatResponseDTO intentionCheckResponse = pythonAIClient.chat(intentionCheckRequest);
        if (intentionCheckResponse.getError() != null) {
            throw new BusinessException(500, intentionCheckResponse.getError());
        }
        String content = intentionCheckResponse.getContent();
        if(content == null) {
            log.error("用户 {} 在意图检测时返回了空结果", UserContext.getUserId());
            throw new BusinessException(500, "意图检测失败");
        }
        // 如果只是普通闲聊，则直接使用普通对话
        if (content.contains("\"search\":false")) {
            // 调用方法先把用户的输入落库
            saveMessage(request.getSessionId(), "user", request.getQuery());
            // 返回LLM的输出，并将其输出落库
            return pythonAIClient.chatStream(
                    toChatRequest(request),
                    fullContent -> {
                        saveMessage(request.getSessionId(), "assistant", fullContent);
                    }
            );
        }
        // 若需要搜索，则调用Rerank接口进行搜索排序
        saveMessage(request.getSessionId(), "user", request.getQuery());
        // 返回LLM的输出，并将其输出落库
        return pythonAIClient.rerank(
                toRerankRequest( request),
                fullContent -> {
                    saveMessage(request.getSessionId(), "assistant", fullContent);
                }
        );
    }

    /**
     * 消息落库
     * @param sessionId 会话ID
     * @param role 角色
     * @param content 内容
     */
    private void saveMessage(String sessionId, String role, String content) {
        AIChatHistory history = new AIChatHistory();
        history.setSessionId(Long.parseLong(sessionId));
        history.setUserId(UserContext.getUserId());
        history.setRole(role);
        history.setContent(content);
        history.setCreateTime(LocalDateTime.now());
        aiChatHistoryMapper.insert(history);
    }

    private ChatRequestDTO toChatRequest(ChatRequestJavaAPIDTO request) {
        // 查询会话是否存在
        AIChatSession session = aiChatSessionMapper.selectById(Long.parseLong(request.getSessionId()));
        // 提前创建消息列表
        List<ChatMessageDTO> messages = new ArrayList<>();
        // 提前创建系统消息
        ChatMessageDTO system;
        // 如果会话存在，则使用会话的摘要作为系统消息
        if (session != null) {
            system = new ChatMessageDTO("system", session.getSummary());
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
        return new ChatRequestDTO(messages, true, UserContext.getUserId().toString());
    }

    private RerankDTO toRerankRequest(ChatRequestJavaAPIDTO request) {
        // 查询会话是否存在
        AIChatSession session = aiChatSessionMapper.selectById(Long.parseLong(request.getSessionId()));
        // 会话不存在则抛出异常
        if (session == null) {
            log.error("用户 {} 在不存在的会话 {}交流", UserContext.getUserId(), request.getSessionId());
            throw new BusinessException(500, "会话不存在");
        }
        // 更新会话时间
        updateUpdatedTime(request.getSessionId());
        // 查询所有帖子
        List<Post> allPosts = postMapper.selectList(null);
        // 查询所有笔记
        List<Document> allDocuments = documentMapper.selectList(null);
        // 提前创建待排序列表
        List<RerankSourceItem> sources = new ArrayList<>();
        // 帖子入表
        for (Post post : allPosts) {
            sources.add(new RerankSourceItem("post", post.getId().toString(), post.getTitle(), post.getContent()));
        }
        // 笔记入表
        for (Document document : allDocuments) {
            sources.add(new RerankSourceItem("document", document.getId().toString(), document.getTitle(), document.getContent()));
        }
        // 返回RerankDTO
        return new RerankDTO(request.getQuery(), sources, 5, UserContext.getUserId().toString());
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
        return pythonAIClient.health();
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
        return toSessionVO(dto.getPage(), dto.getSize());
    }


}
