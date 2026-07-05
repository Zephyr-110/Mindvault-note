package org.example.community.post.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeedDTO {

    @NotNull
    private Long page = 1L;
    @NotNull
    private Long size = 10L;
    private String sortBy = "time";//time或hot
}
