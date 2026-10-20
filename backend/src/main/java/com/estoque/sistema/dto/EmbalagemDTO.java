package com.estoque.sistema.dto;

import com.estoque.sistema.model.Embalagem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Formato "achatado" para o agente ler facilmente: produto -> codigo -> unidades. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbalagemDTO {

    private Long id;
    private Long itemId;
    private String produto;
    private String codigo;
    private String nome;
    private Integer unidades;
    private BigDecimal precoEmbalagem;

    public static EmbalagemDTO de(Embalagem e) {
        return new EmbalagemDTO(e.getId(), e.getItem().getId(), e.getItem().getNome(),
                e.getCodigo(), e.getNome(), e.getUnidades(), e.getPrecoEmbalagem());
    }
}
