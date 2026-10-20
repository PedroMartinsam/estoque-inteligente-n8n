package com.estoque.sistema.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trava de seguranca contra o agente de IA repetir a MESMA operacao de escrita em sequencia
 * (ex: dar 7 entradas de 20 limoes quando o usuario pediu uma).
 *
 * Se a mesma operacao (tipo + produto + valor) ja foi feita ha menos de 30 segundos,
 * ela nao e executada de novo e a API devolve um aviso para o agente parar.
 */
@Component
public class ProtecaoRepeticao {

    private static final long JANELA_MS = 30_000;

    private final Map<String, Long> ultimas = new ConcurrentHashMap<>();

    /** Se a operacao foi feita ha pouco, devolve ha quantos segundos. Senao, vazio. */
    public Optional<Long> feitaRecentemente(String operacao, String produto, Object valor) {
        long agora = System.currentTimeMillis();
        ultimas.entrySet().removeIf(e -> agora - e.getValue() > JANELA_MS);
        Long anterior = ultimas.get(chave(operacao, produto, valor));
        return anterior == null ? Optional.empty() : Optional.of((agora - anterior) / 1000);
    }

    /** Chamar depois que a operacao deu certo. */
    public void registrar(String operacao, String produto, Object valor) {
        ultimas.put(chave(operacao, produto, valor), System.currentTimeMillis());
    }

    private static String chave(String operacao, String produto, Object valor) {
        String nome = produto == null ? "" : produto.trim().toLowerCase(Locale.ROOT);
        return operacao + "|" + nome + "|" + valor;
    }
}