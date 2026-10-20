package com.estoque.sistema.service;

import com.estoque.sistema.dto.ComandoDTO;
import com.estoque.sistema.model.ComandoAutomacao;
import com.estoque.sistema.model.ComandoAutomacao.StatusComando;
import com.estoque.sistema.repository.ComandoAutomacaoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ComandoAutomacaoService {

    private final ComandoAutomacaoRepository comandoRepository;

    public List<ComandoAutomacao> listarTodos() {
        return comandoRepository.findAllByOrderByDataEnvioDesc();
    }

    public List<ComandoAutomacao> listarUltimos() {
        return comandoRepository.findTop10ByOrderByDataEnvioDesc();
    }

    // ------------------------------------------------------------ painel da tela de Automacao

    /** Depois desse tempo sem resposta, um comando "Processando" e dado como nao finalizado. */
    private static final int MINUTOS_PARA_EXPIRAR = 10;

    /**
     * Comandos do chat mais recentes.
     * periodo: "hoje" (desde 00:00) ou "tudo".
     */
    @Transactional
    public List<ComandoAutomacao> listarRecentes(int limite, String periodo) {
        expirarParados();
        int tamanho = Math.max(1, Math.min(limite, 100));
        return comandoRepository.findDoChatDesde(inicioDoPeriodo(periodo), PageRequest.of(0, tamanho));
    }

    /** Quantidade de comandos do chat em cada status no periodo. Sempre traz as 4 chaves. */
    @Transactional
    public Map<String, Long> resumo(String periodo) {
        expirarParados();
        Map<String, Long> mapa = new LinkedHashMap<>();
        for (StatusComando s : StatusComando.values()) {
            mapa.put(s.name(), 0L);
        }
        for (Object[] linha : comandoRepository.contarDoChatPorStatusDesde(inicioDoPeriodo(periodo))) {
            mapa.put(((StatusComando) linha[0]).name(), ((Number) linha[1]).longValue());
        }
        return mapa;
    }

    /** Apaga todo o historico de comandos (o estoque nao e alterado). Retorna quantos foram apagados. */
    @Transactional
    public long zerar() {
        long total = comandoRepository.count();
        comandoRepository.deleteAllInBatch();
        return total;
    }

    /** "Processando" parado ha mais de 10 min vira Erro: a conversa parou antes de concluir. */
    @Transactional
    public int expirarParados() {
        LocalDateTime agora = LocalDateTime.now();
        return comandoRepository.expirarParados(
                StatusComando.PROCESSANDO,
                StatusComando.ERRO,
                "Nao finalizado: a conversa parou antes de o assistente concluir a operacao.",
                agora,
                agora.minusMinutes(MINUTOS_PARA_EXPIRAR));
    }

    private static LocalDateTime inicioDoPeriodo(String periodo) {
        if ("hoje".equalsIgnoreCase(periodo)) {
            return LocalDate.now().atStartOfDay();
        }
        return LocalDateTime.of(2000, 1, 1, 0, 0);
    }

    public Optional<ComandoAutomacao> buscarPorId(Long id) {
        return comandoRepository.findById(id);
    }

    public List<ComandoAutomacao> listarPendentes() {
        return comandoRepository.findByStatus(StatusComando.PENDENTE);
    }

    @Transactional
    public ComandoAutomacao enviarComando(ComandoDTO comandoDTO) {
        ComandoAutomacao comando = new ComandoAutomacao();
        comando.setComando(comandoDTO.getComando());
        return comandoRepository.save(comando);
    }

    @Transactional
    public ComandoAutomacao atualizarStatus(Long id, StatusComando status, String resultado) {
        ComandoAutomacao comando = comandoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Comando nao encontrado com id: " + id));

        comando.setStatus(status);
        comando.setResultado(resultado);
        comando.setDataProcessamento(LocalDateTime.now());

        return comandoRepository.save(comando);
    }

    @Transactional
    public ComandoAutomacao marcarComoConcluido(Long id, String resultado) {
        return atualizarStatus(id, StatusComando.CONCLUIDO, resultado);
    }

    @Transactional
    public ComandoAutomacao marcarComoErro(Long id, String erro) {
        return atualizarStatus(id, StatusComando.ERRO, erro);
    }

    // ------------------------------------------------------------ usados pelo ChatService

    /** Registra a mensagem do chat como um comando em PROCESSANDO. */
    @Transactional
    public ComandoAutomacao iniciarComandoChat(String sessaoId, String texto) {
        ComandoAutomacao comando = new ComandoAutomacao();
        comando.setSessaoId(sessaoId);
        comando.setComando(limitar(texto, 1000));
        comando.setStatus(StatusComando.PROCESSANDO);
        return comandoRepository.save(comando);
    }

    /** Grava o resultado da rodada. PROCESSANDO = agente ainda esta coletando dados. */
    @Transactional
    public void finalizarComandoChat(Long id, StatusComando status, String resultado) {
        comandoRepository.findById(id).ifPresent(comando -> {
            comando.setStatus(status);
            comando.setResultado(limitar(resultado, 2000));
            if (status != StatusComando.PROCESSANDO) {
                comando.setDataProcessamento(LocalDateTime.now());
            }
            comandoRepository.save(comando);
        });
    }

    /**
     * Quando a operacao da conversa termina, os comandos anteriores da mesma sessao que
     * ficaram em PROCESSANDO (ex: "cadastra abacaxi" -> agente perguntou o preco) viram CONCLUIDO.
     */
    @Transactional
    public void concluirPendentesDaSessao(String sessaoId, Long exceto) {
        for (ComandoAutomacao c : comandoRepository.findBySessaoIdAndStatus(sessaoId, StatusComando.PROCESSANDO)) {
            if (!c.getId().equals(exceto)) {
                c.setStatus(StatusComando.CONCLUIDO);
                c.setDataProcessamento(LocalDateTime.now());
                comandoRepository.save(c);
            }
        }
    }

    private static String limitar(String texto, int max) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}