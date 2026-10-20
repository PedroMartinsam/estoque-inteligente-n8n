package com.estoque.sistema.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDTO {

    private Long id;

    @NotBlank(message = "O nome do item e obrigatorio")
    private String nome;

    private String descricao;

    @NotNull(message = "A quantidade e obrigatoria")
    @Min(value = 0, message = "A quantidade nao pode ser negativa")
    private Integer quantidade;

    private BigDecimal preco;

    private String categoria;

    private String unidade;

    private Integer quantidadeMinima;
}
