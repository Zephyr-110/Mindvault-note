package org.example.mindvaultaiapi.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HistoryVO {
    private String role;
    private String content;
    private LocalDateTime createTime;
}
