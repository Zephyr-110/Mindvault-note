package org.example.mindvaultaiapi.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionVO {
    private Long id;
    private String title;
    private String summary;
    private LocalDateTime updateTime;
}
