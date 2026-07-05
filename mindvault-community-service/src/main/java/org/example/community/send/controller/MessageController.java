package org.example.community.send.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.common.result.Result;
import org.example.common.result.PageResult;
import org.example.community.send.dto.ListConversationDTO;
import org.example.community.send.dto.MessageHistoryRecordDTO;
import org.example.community.send.MessageService;
import org.example.community.send.dto.ReadDTO;
import org.example.community.send.dto.SendMessageDTO;
import org.example.community.send.vo.ConversationVO;
import org.example.community.send.vo.MessageVO;
import org.example.community.send.vo.UnreadCountVO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/community/message")
public class MessageController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/send")
    public Result<MessageVO> send(@Valid @RequestBody SendMessageDTO sendMessageDTO) {
        MessageVO messageVO = messageService.send(sendMessageDTO);
        messagingTemplate.convertAndSend("/queue/chat." + String.valueOf(sendMessageDTO.getReceiverId()), messageVO);
        return Result.success(messageVO);
    }

    @GetMapping("/list-conversations")
    public Result<PageResult<ConversationVO>> listConversation(@Valid ListConversationDTO listConversationDTO) {
        return Result.success(messageService.listConversation(listConversationDTO));
    }

    @GetMapping("/history")
    public Result<PageResult<MessageVO>> history(@Valid MessageHistoryRecordDTO messageHistoryRecordDTO) {
        return Result.success(messageService.history(messageHistoryRecordDTO));
    }

    @PutMapping("/mark-read")
    public Result<Void> markRead(@Valid @RequestBody ReadDTO readDTO) {
        messageService.markRead(readDTO);
        return Result.success();
    }
    //未读私信总数

    @GetMapping("/unread-count")
    public Result<UnreadCountVO> unreadCount() {
        return Result.success(messageService.unreadCount());
    }
}