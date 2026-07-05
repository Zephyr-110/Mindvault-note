package org.example.user.block.service;


import org.example.common.result.PageResult;
import org.example.user.block.dto.BlockUserDTO;
import org.example.user.block.dto.IsBlockedDTO;
import org.example.user.block.dto.ListUserBlocksDTO;
import org.example.user.block.vo.UserBlockVO;

public interface UserBlockService {

    void blockUser(BlockUserDTO dto);

    void unblockUser(BlockUserDTO dto);

    PageResult<UserBlockVO> listBlockedUsers(ListUserBlocksDTO dto);

    Boolean isBlocked(IsBlockedDTO dto);
}
