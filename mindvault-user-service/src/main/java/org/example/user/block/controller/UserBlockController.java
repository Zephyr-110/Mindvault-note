package org.example.user.block.controller;

import lombok.RequiredArgsConstructor;
import org.example.common.result.PageResult;
import org.example.common.result.Result;
import org.example.user.block.dto.BlockUserDTO;
import org.example.user.block.dto.IsBlockedDTO;
import org.example.user.block.dto.ListUserBlocksDTO;
import org.example.user.block.service.UserBlockService;
import org.example.user.block.vo.UserBlockVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user-block")
@Validated
public class UserBlockController {

    private final UserBlockService userBlockService;

    @PostMapping("/block")
    public Result<?> blockUser(@RequestBody BlockUserDTO blockUserDTO) {
        userBlockService.blockUser(blockUserDTO);
        return Result.success();
    }

    @DeleteMapping("/unblock")
    public Result<?> unblockUser(BlockUserDTO blockUserDTO) {
        userBlockService.unblockUser(blockUserDTO);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<PageResult<UserBlockVO>> listUserBlocks(ListUserBlocksDTO listUserBlocksDTO) {
        return Result.success(userBlockService.listBlockedUsers(listUserBlocksDTO));
    }

    @GetMapping("/is-blocked")
    public Result<Boolean> isBlocked(IsBlockedDTO isBlockedDTO) {
        return Result.success(userBlockService.isBlocked(isBlockedDTO));
    }
}