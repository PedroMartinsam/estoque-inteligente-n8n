package com.estoque.sistema.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Uma conversa com o agente. Guarda o "estado" (dados ja coletados) entre as mensagens. */
@Entity
@Table(name = "chat_sessoes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessao {

    @Id
    @Column(name = "sessao_id", length = 64)
    private String sessaoId;

    /** JSON (como texto) devolvido pelo agente em 'estadoAtualizado'. */
    @Column(name = "estado_atual", length = 4000)
    private String estadoAtual = "{}";

    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
        dataAtualizacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }
}
