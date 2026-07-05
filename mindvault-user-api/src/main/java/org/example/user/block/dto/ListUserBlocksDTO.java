package org.example.user.block.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListUserBlocksDTO {
    private Long page = 1L;//页码
    private Long size = 10L;//每页大小
}
