package org.example.user.user.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchUserDTO {
    private String keyword;
    @NotNull @Min(1L)
    private Long page = 1L;
    @NotNull @Min(1L) @Max(100L)
    private Long size = 10L;
}
