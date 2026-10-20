package com.estoque.sistema.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDTO {

    @NotBlank(message = "O id da sessao e obrigatorio")
    @Size(max = 64)
    private String sessaoId;

    @NotBlank(message = "A mensagem e obrigatoria")
    @Size(max = 2000, message = "A mensagem pode ter no maximo 2000 caracteres")
    private String mensagem;
}
