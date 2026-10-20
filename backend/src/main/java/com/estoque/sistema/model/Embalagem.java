package com.estoque.sistema.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Quantas unidades de um produto cabem em cada embalagem.
 * Ex.: Banana, codigo M ("caixa media") = 30 unidades.
 * Mapeado para a tabela "embalagens" que ja existe no banco.
 */
@Entity
@Table(name = "embalagens",
        uniqueConstraints = @UniqueConstraint(name = "uk_embalagem_item_codigo", columnNames = {"item_id", "codigo"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Embalagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /** Ex.: P, M, G, FARDO, CX-M */
    @Column(nullable = false, length = 20)
    private String codigo;

    /** Ex.: caixa grande, fardo */
    @Column(length = 60)
    private String nome;

    @Column(name = "unidades_por_embalagem", nullable = false)
    private Integer unidades;

    @Column(name = "preco_embalagem", precision = 10, scale = 2)
    private BigDecimal precoEmbalagem;

    @Column(name = "data_cadastro")
    private LocalDateTime dataCadastro;

    @PrePersist
    protected void onCreate() {
        if (dataCadastro == null) {
            dataCadastro = LocalDateTime.now();
        }
    }
}
