package org.example.community.send.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.annotation.CheckOwner;
import org.example.common.annotation.CheckResource;
import org.example.common.exception.BusinessException;
import org.example.common.logincheck.UserContext;
import org.example.community.follow.FollowService;
import org.example.user.setting.service.UserSettingService;
import org.example.user.user.service.UserService;
import org.example.common.result.PageResult;
import org.example.community.send.dto.ListConversationDTO;
import org.example.community.send.dto.MessageHistoryRecordDTO;
import org.example.community.send.dto.ReadDTO;
import org.example.community.send.dto.SendMessageDTO;
import org.example.community.send.MessageService;
import org.example.community.send.entity.Message;
import org.example.community.send.mapper.MessageMapper;
import org.example.community.send.vo.ConversationVO;
import org.example.community.send.vo.MessageVO;
import org.example.community.send.vo.UnreadCountVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final int NON_MUTUAL_LIMIT = 3;

    private final MessageMapper messageMapper;
    private final UserService userService;
    private final FollowService followService;
    private final UserSettingService userSettingService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageVO send(SendMessageDTO sendMessageDTO) {
        return sendInternal(UserContext.getUserId(), sendMessageDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageVO sendInternal(Long senderId, SendMessageDTO dto) {
        //校验
        if (senderId == null) {
            throw new BusinessException(400, "发送者身份校验失败，请重新登录");
        }
        if (senderId.equals(dto.getReceiverId())) {
            throw new BusinessException(400, "不能给自己发消息");
        }
        if (!userService.existsById(dto.getReceiverId())) {
            throw new BusinessException(404, "对方用户不存在");
        }
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new BusinessException(400, "消息内容不能为空");
        }
        if (dto.getContent().length() > 500) {
            throw new BusinessException(400, "消息内容不能超过500字");
        }

        // 校验接收者是否开启允许陌生人聊天（WebSocket 和 REST 统一走这里）
        String settingKey = "privacy.allowStrangerChat";
        var setting = userSettingService.getSetting(settingKey, dto.getReceiverId());
        if (setting != null && "false".equals(setting.getSettingValue())) {
            if (!followService.isMutualFollow(senderId, dto.getReceiverId())) {
                throw new BusinessException(400, "对方未开启允许陌生人聊天");
            }
        }

        // 消息限制：非互关最多 3 条未读消息
        if (!followService.isMutualFollow(senderId, dto.getReceiverId())) {
            long unreadCount = messageMapper.selectCount(
                    new LambdaQueryWrapper<Message>()
                            .eq(Message::getSenderId, senderId)
                            .eq(Message::getReceiverId, dto.getReceiverId())
                            .eq(Message::getIsRead, false));
            if (unreadCount >= NON_MUTUAL_LIMIT) {
                throw new BusinessException(400,
                        "对方未回复，暂时无法发送更多消息（限制" + NON_MUTUAL_LIMIT + "条）");
            }
        }

        //落库
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(dto.getReceiverId());
        message.setContent(dto.getContent());
        message.setIsRead(false);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);

        log.info("消息已发送: {} → {}", senderId, dto.getReceiverId());
        //调用toVO方法构建返回值返回
        return toVO(message);
    }

    //这个方法很精髓，可以学习一下
    @Override
    public PageResult<ConversationVO> listConversation(ListConversationDTO dto) {
        Long userId = UserContext.getUserId();
        //查出所有与我相关的消息，按时间倒序
        List<Message> allMessages = new LambdaQueryChainWrapper<>(messageMapper)
                .and(w -> w.eq(Message::getSenderId, userId).or().eq(Message::getReceiverId, userId))
                .orderByDesc(Message::getCreateTime)
                .list();
        //按对方ID分组，取每组第一条（消息已倒序，第一条就是最新消息）
        Map<Long, Message> latestMap = new LinkedHashMap<>();
        for (Message msg : allMessages) {
            //取对方id
            Long otherId = msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId();
            //对方的id对应着关于用户的所有消息
            latestMap.putIfAbsent(otherId, msg);
        }
        //遍历分组，构建VO
        List<ConversationVO> voList = new ArrayList<>();
        for (Map.Entry<Long, Message> entry : latestMap.entrySet()) {
            Long otherId = entry.getKey();
            Message latest = entry.getValue();
            ConversationVO vo = new ConversationVO();
            vo.setUserId(otherId);
            vo.setNickname(userService.getNickname(otherId));
            vo.setAvatar(userService.getAvatar(otherId));
            vo.setLastMessage(latest.getContent());
            vo.setLastTime(latest.getCreateTime());
            vo.setIsLastMessageFromMe(latest.getSenderId().equals(userId));
            //未读数：对方发给我的、且未读的
            Long unread = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                    .eq(Message::getSenderId, otherId)
                    .eq(Message::getReceiverId, userId)
                    .eq(Message::getIsRead, false));
            vo.setUnreadCount(unread.intValue());
            voList.add(vo);
        }
        //手动分页截取
        int start = (int) ((dto.getPage() - 1) * dto.getSize());
        int end = Math.min(start + dto.getSize().intValue(), voList.size());
        List<ConversationVO> pageList = start >= voList.size() ? List.of() : voList.subList(start, end);
        log.info("用户 {} 获取了 {} 页的会话列表", UserContext.getUserId(), dto.getPage());
        return new PageResult<>((long) voList.size(), dto.getPage(), dto.getSize(), pageList);
    }


    @Override
    public PageResult<MessageVO> history(MessageHistoryRecordDTO messageHistoryRecordDTO) {
        Page<Message> page = new Page<>(messageHistoryRecordDTO.getPage(), messageHistoryRecordDTO.getSize());
        //获得一页消息记录
        Page<Message> result = new LambdaQueryChainWrapper<>(messageMapper)
                .eq(Message::getSenderId, messageHistoryRecordDTO.getWithUserId())
                .eq(Message::getReceiverId, UserContext.getUserId())
                .or()
                .eq(Message::getSenderId, UserContext.getUserId())
                .eq(Message::getReceiverId, messageHistoryRecordDTO.getWithUserId())
                .orderByDesc(Message::getCreateTime)
                .page(page);
        List<MessageVO> records = new ArrayList<>();
        //构建VO列表
        for (Message message : result.getRecords()) {
            MessageVO vo = new MessageVO(message.getId(),
                    message.getSenderId(),
                    message.getReceiverId(),
                    message.getContent(),
                    message.getIsRead(),
                    message.getCreateTime());
            records.add(vo);
        }
        log.info("用户 {} 获取了 {} 页的会话 {} 的聊天记录", UserContext.getUserId(), messageHistoryRecordDTO.getPage(), messageHistoryRecordDTO.getWithUserId());
        return new PageResult<>(result.getTotal(), messageHistoryRecordDTO.getPage(), messageHistoryRecordDTO.getSize(), records);
    }


    @Override
    public void markRead(ReadDTO readDTO) {
        Message updateEntity = new Message();
        updateEntity.setIsRead(true);
        //更新对方用户发给我消息的消息为已读
        messageMapper.update(updateEntity,
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getSenderId, readDTO.getWithUserId())
                        .eq(Message::getReceiverId, UserContext.getUserId())
                        .eq(Message::getIsRead, false)
        );
        log.info("用户 {} 将 {} 的所有未读消息已读", UserContext.getUserId(), readDTO.getWithUserId());
    }

    @Override
    public UnreadCountVO unreadCount() {
        Long count = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, UserContext.getUserId())
                .eq(Message::getIsRead, false));
        return new UnreadCountVO(count);
    }
    //私有方法，构建返回值，仅服务于发送消息
    private MessageVO toVO(Message message) {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setSenderId(message.getSenderId());
        vo.setReceiverId(message.getReceiverId());
        vo.setContent(message.getContent());
        vo.setIsRead(message.getIsRead());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}