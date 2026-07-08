package org.example.community.send.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageHistoryRecordDTO {

    @NotNull(message = "对方id不能为空")
    private Long withUserId;//与谁聊天
    @NotNull @Min(1)
    private Long page = 1L;
    @NotNull @Min(1) @Max(100)
    private Long size = 10L;
}
