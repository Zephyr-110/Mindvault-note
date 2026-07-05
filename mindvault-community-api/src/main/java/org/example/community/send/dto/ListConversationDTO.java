package org.example.community.send.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListConversationDTO {

    private Long page = 1L;
    private Long size = 10L;
}
