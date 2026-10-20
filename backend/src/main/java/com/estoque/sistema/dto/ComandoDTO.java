package com.estoque.sistema.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComandoDTO {

    @NotBlank(message = "O comando e obrigatorio")
    private String comando;
}
