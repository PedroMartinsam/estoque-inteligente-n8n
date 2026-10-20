package com.estoque.sistema.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "itens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome do item e obrigatorio")
    @Column(nullable = false)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @NotNull(message = "A quantidade e obrigatoria")
    @Min(value = 0, message = "A quantidade nao pode ser negativa")
    @Column(nullable = false)
    private Integer quantidade;

    @Column(precision = 10, scale = 2)
    private BigDecimal preco;

    private String categoria;

    private String unidade;

    @Column(name = "quantidade_minima")
    private Integer quantidadeMinima;

    /**
     * Exclusao logica: item inativo nao aparece nas listagens nem nas buscas.
     * Coluna nullable de proposito para o ddl-auto=update conseguir adicionar
     * em tabela que ja tem dados; itens antigos (null) contam como ativos.
     */
    @Column(name = "ativo")
    private Boolean ativo = true;

    @Column(name = "data_cadastro")
    private LocalDateTime dataCadastro;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @PrePersist
    protected void onCreate() {
        dataCadastro = LocalDateTime.now();
        dataAtualizacao = LocalDateTime.now();
        if (ativo == null) {
            ativo = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }

    public boolean estaAtivo() {
        return ativo == null || ativo;
    }
}
