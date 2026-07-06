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

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void handleChat(@Payload SendMessageDTO dto) {
        // WebSocket 不走 HTTP 拦截器，UserContext 为空，必须用客户端传的 senderId
        Long senderId = dto.getSenderId();
        try {
            MessageVO messageVO = messageService.sendInternal(senderId, dto);

            // 推送给接收者
            messagingTemplate.convertAndSend("/queue/chat." + dto.getReceiverId(), messageVO);
            // 推送给发送者（双写，保证多端同步）
            messagingTemplate.convertAndSend("/queue/chat." + senderId, messageVO);

            log.info("消息已推送: {} → {}", senderId, dto.getReceiverId());
        } catch (Exception e) {
            log.error("WebSocket 消息发送失败: {} → {}, 原因: {}", senderId, dto.getReceiverId(), e.getMessage());
            String errorMsg = e.getMessage() != null ? e.getMessage() : "发送失败";
            Map<String, Object> errorPayload = new HashMap<>();
            errorPayload.put("error", true);
            errorPayload.put("message", errorMsg);
            messagingTemplate.convertAndSend("/queue/chat." + senderId, (Object) errorPayload);
        }
    }
}