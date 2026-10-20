package com.estoque.sistema.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_mensagens", indexes = @Index(name = "idx_chat_msg_sessao", columnList = "sessao_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMensagem {

    public enum Papel { USUARIO, ASSISTENTE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sessao_id", nullable = false, length = 64)
    private String sessaoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Papel papel;

    @Column(nullable = false, length = 4000)
    private String conteudo;

    /** So para mensagens do assistente: a operacao foi executada nesta rodada? */
    private Boolean concluido;

    /** So para mensagens do assistente: houve falha na comunicacao com o n8n? */
    private Boolean erro;

    @Column(name = "data_hora")
    private LocalDateTime dataHora;

    @PrePersist
    protected void onCreate() {
        dataHora = LocalDateTime.now();
    }
}
