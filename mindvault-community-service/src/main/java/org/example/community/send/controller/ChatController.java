package org.example.community.send.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.community.send.MessageService;
import org.example.community.send.dto.SendMessageDTO;
import org.example.community.send.vo.MessageVO;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final PlatformTransactionManager transactionManager;

    @MessageMapping("/chat.send")
    public void handleChat(@Payload SendMessageDTO dto) {
        // WebSocket 不走 HTTP 拦截器，UserContext 为空，必须用客户端传的 senderId
        Long senderId = dto.getSenderId();
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            MessageVO messageVO = messageService.sendInternal(senderId, dto);
            transactionManager.commit(status);

            // 推送给接收者
            messagingTemplate.convertAndSend("/queue/chat." + String.valueOf(dto.getReceiverId()), messageVO);

            messagingTemplate.convertAndSend("/queue/chat." + String.valueOf(senderId), messageVO);

            log.info("消息已推送: {} → {}", senderId, dto.getReceiverId());
        } catch (Exception e) {
            transactionManager.rollback(status);
            log.error("WebSocket 消息发送失败: {} → {}, 原因: {}", senderId, dto.getReceiverId(), e.getMessage());
            String errorMsg = e.getMessage() != null ? e.getMessage() : "发送失败";
            Map<String, Object> errorPayload = new HashMap<>();
            errorPayload.put("error", true);
            errorPayload.put("message", errorMsg);
            messagingTemplate.convertAndSend("/queue/chat." + String.valueOf(senderId), (Object) errorPayload);
        }
    }
}