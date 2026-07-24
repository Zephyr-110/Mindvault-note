package org.example.mindvaultaiapi.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListSessionDTO {
    private Long page = 1L;
    private Long size = 20L;
}
