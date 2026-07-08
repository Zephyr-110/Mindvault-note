package org.example.community.follow.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListFollowerDTO {//粉丝列表

    private Long userId;//要查看的用户id，为空则取当前用户
    @NotNull @Min(1)
    private Long page = 1L;
    @NotNull @Min(1) @Max(100)
    private Long size = 10L;

}