package org.example.community.favorite.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListFavoriteDTO {

    @NotNull @Min(1)
    private Long page = 1L;
    @NotNull @Min(1) @Max(100)
    private Long size = 10L;
}
