package org.example.community.send;

import jakarta.validation.Valid;
import org.example.common.result.PageResult;
import org.example.community.send.dto.ListConversationDTO;
import org.example.community.send.dto.MessageHistoryRecordDTO;
import org.example.community.send.dto.ReadDTO;
import org.example.community.send.dto.SendMessageDTO;
import org.example.community.send.vo.ConversationVO;
import org.example.community.send.vo.MessageVO;
import org.example.community.send.vo.UnreadCountVO;

public interface MessageService {
    MessageVO send(@Valid SendMessageDTO sendMessageDTO);

    MessageVO sendInternal(Long senderId, SendMessageDTO dto);

    PageResult<ConversationVO> listConversation(ListConversationDTO listConversationDTO);

    PageResult<MessageVO> history(@Valid MessageHistoryRecordDTO messageHistoryRecordDTO);

    void markRead(@Valid ReadDTO readDTO);

    UnreadCountVO unreadCount();
}
