package com.estoque.sistema.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "comandos_automacao")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComandoAutomacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String comando;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusComando status;

    @Column(length = 2000)
    private String resultado;

    @Column(name = "data_envio")
    private LocalDateTime dataEnvio;

    @Column(name = "data_processamento")
    private LocalDateTime dataProcessamento;

    /** Conversa do chat que gerou o comando (null para comandos do fluxo antigo). */
    @Column(name = "sessao_id", length = 64)
    private String sessaoId;

    public enum StatusComando {
        PENDENTE,
        PROCESSANDO,
        CONCLUIDO,
        ERRO
    }

    @PrePersist
    protected void onCreate() {
        dataEnvio = LocalDateTime.now();
        if (status == null) {
            status = StatusComando.PENDENTE;
        }
    }
}
