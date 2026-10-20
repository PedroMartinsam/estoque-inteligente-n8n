package com.estoque.sistema.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** O que o frontend recebe a cada mensagem enviada. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRespostaDTO {

    private String sessaoId;
    private String resposta;
    private boolean concluido;
    private boolean erro;

    /** Id do registro em comandos_automacao criado para esta mensagem. */
    private Long comandoId;

    /** Status do comando: CONCLUIDO, PROCESSANDO ou ERRO. */
    private String status;
}