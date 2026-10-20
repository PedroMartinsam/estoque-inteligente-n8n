package com.estoque.sistema.service;

import com.estoque.sistema.dto.ChatRequestDTO;
import com.estoque.sistema.dto.ChatRespostaDTO;
import com.estoque.sistema.model.ChatMensagem;
import com.estoque.sistema.model.ComandoAutomacao;
import com.estoque.sistema.model.ComandoAutomacao.StatusComando;
import com.estoque.sistema.model.ChatMensagem.Papel;
import com.estoque.sistema.model.ChatSessao;
import com.estoque.sistema.repository.ChatMensagemRepository;
import com.estoque.sistema.repository.ChatSessaoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ponte entre o frontend e o workflow "Agente de Estoque" do n8n.
 *
 * Fluxo de cada mensagem:
 *   front -> POST /api/chat -> este service -> webhook do n8n (agente + tools)
 *   n8n responde {resposta, concluido, erro, estadoAtualizado}
 *   -> salvamos a resposta e o estado -> devolvemos para o front.
 *
 * O agente nao tem memoria propria: a cada chamada mandamos o historico
 * recente e o estado (dados ja coletados) da conversa.
 */
@Slf4j
@Service
public class ChatService {

    private static final int MENSAGENS_NO_HISTORICO = 20;

    private final ChatSessaoRepository sessaoRepository;
    private final ChatMensagemRepository mensagemRepository;
    private final RestTemplate n8nRestTemplate;
    private final ObjectMapper objectMapper;
    private final ComandoAutomacaoService comandoService;
    private final String webhookUrl;

    public ChatService(ChatSessaoRepository sessaoRepository,
                       ChatMensagemRepository mensagemRepository,
                       @Qualifier("n8nRestTemplate") RestTemplate n8nRestTemplate,
                       ObjectMapper objectMapper,
                       ComandoAutomacaoService comandoService,
                       @Value("${n8n.webhook.url}") String webhookUrl) {
        this.sessaoRepository = sessaoRepository;
        this.mensagemRepository = mensagemRepository;
        this.n8nRestTemplate = n8nRestTemplate;
        this.objectMapper = objectMapper;
        this.comandoService = comandoService;
        this.webhookUrl = webhookUrl;
    }

    // Sem @Transactional de proposito: a chamada ao n8n pode levar dezenas de
    // segundos e nao deve segurar uma transacao/conexao do banco aberta.
    public ChatRespostaDTO enviarMensagem(ChatRequestDTO request) {
        String sessaoId = request.getSessaoId().trim();
        String mensagem = request.getMensagem().trim();

        ChatSessao sessao = sessaoRepository.findById(sessaoId).orElseGet(() -> {
            ChatSessao nova = new ChatSessao();
            nova.setSessaoId(sessaoId);
            nova.setEstadoAtual("{}");
            return sessaoRepository.save(nova);
        });

        // historico ANTES da mensagem nova (a nova vai no campo 'mensagem')
        String historico = montarHistorico(sessaoId);
        salvarMensagem(sessaoId, Papel.USUARIO, mensagem, null, null);

        // aparece no painel da tela como "Processando" enquanto o agente trabalha
        ComandoAutomacao comando = comandoService.iniciarComandoChat(sessaoId, mensagem);

        Map<String, Object> payload = new HashMap<>();
        payload.put("sessaoId", sessaoId);
        payload.put("mensagem", mensagem);
        payload.put("estadoAtual", sessao.getEstadoAtual() == null ? "{}" : sessao.getEstadoAtual());
        payload.put("historico", historico);

        String resposta;
        boolean concluido = false;
        boolean erro = false;
        String novoEstado = sessao.getEstadoAtual();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // le como texto e interpreta aqui: aceita os varios formatos que o n8n pode devolver
            String bruto = n8nRestTemplate.postForObject(
                    webhookUrl, new HttpEntity<>(payload, headers), String.class);

            JsonNode corpo = normalizarResposta(bruto);
            if (corpo == null) {
                throw new IllegalStateException("Resposta do n8n sem texto para o usuario: " + bruto);
            }

            resposta = corpo.get("resposta").asText();
            concluido = corpo.path("concluido").asBoolean(false)
                    || "true".equalsIgnoreCase(corpo.path("concluido").asText());
            erro = corpo.path("erro").asBoolean(false);

            JsonNode estado = corpo.get("estadoAtualizado");
            novoEstado = concluido || estado == null || estado.isNull()
                    ? "{}"
                    : estadoComoTexto(estado.isTextual() ? estado.asText() : estado.toString());
            sessao.setEstadoAtual(novoEstado);
            sessaoRepository.save(sessao);

        } catch (ResourceAccessException e) {
            log.error("n8n inacessivel ou demorou demais ({}): {}", webhookUrl, e.getMessage());
            resposta = "Nao consegui falar com o assistente agora (o n8n esta fora do ar ou demorou demais). "
                    + "Tente de novo em instantes.";
            erro = true;
        } catch (RestClientResponseException e) {
            log.error("n8n respondeu HTTP {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            resposta = e.getStatusCode().value() == 404
                    ? "O webhook do assistente nao foi encontrado. Verifique se o workflow esta ATIVO no n8n."
                    : "O assistente teve um problema ao processar sua mensagem. Tente de novo.";
            erro = true;
        } catch (RestClientException | IllegalStateException e) {
            log.error("Falha ao processar resposta do n8n", e);
            resposta = "O assistente respondeu em um formato inesperado. Tente de novo.";
            erro = true;
        }

        salvarMensagem(sessaoId, Papel.ASSISTENTE, resposta, concluido, erro);

        // Status do comando no painel:
        //  ERRO        -> falhou (n8n fora, erro do agente)
        //  CONCLUIDO   -> operacao executada, ou consulta respondida
        //  PROCESSANDO -> agente ainda esta coletando dados (ex: perguntou o preco)
        StatusComando status;
        if (erro) {
            status = StatusComando.ERRO;
        } else if (concluido || (!temOperacaoPendente(novoEstado) && !terminaComPergunta(resposta))) {
            status = StatusComando.CONCLUIDO;
        } else {
            status = StatusComando.PROCESSANDO;
        }
        comandoService.finalizarComandoChat(comando.getId(), status, resposta);
        if (status == StatusComando.CONCLUIDO) {
            comandoService.concluirPendentesDaSessao(sessaoId, comando.getId());
        }

        return new ChatRespostaDTO(sessaoId, resposta, concluido, erro, comando.getId(), status.name());
    }

    public List<ChatMensagem> listarMensagens(String sessaoId) {
        return mensagemRepository.findBySessaoIdOrderByIdAsc(sessaoId);
    }

    @Transactional
    public void limparSessao(String sessaoId) {
        mensagemRepository.deleteBySessaoId(sessaoId);
        sessaoRepository.deleteById(sessaoId);
    }

    // ------------------------------------------------------------------ helpers

    private String montarHistorico(String sessaoId) {
        List<ChatMensagem> ultimas = new ArrayList<>(mensagemRepository.findBySessaoIdOrderByIdDesc(
                sessaoId, PageRequest.of(0, MENSAGENS_NO_HISTORICO)));
        if (ultimas.isEmpty()) {
            return "(inicio da conversa)";
        }
        Collections.reverse(ultimas);

        StringBuilder sb = new StringBuilder();
        for (ChatMensagem m : ultimas) {
            sb.append(m.getPapel() == Papel.USUARIO ? "Usuario: " : "Assistente: ")
                    .append(m.getConteudo())
                    .append('\n');
        }
        return sb.toString().trim();
    }

    /**
     * O n8n pode devolver a resposta de jeitos diferentes, dependendo de como o agente saiu:
     *   {"resposta": "...", ...}                      (formato esperado)
     *   [{"resposta": "...", ...}]                    (lista de itens)
     *   {"output": {"resposta": "...", ...}}          (saida do agente sem o no de resposta formatar)
     *   {"output": "texto"} ou {"output": "{\"resposta\": ...}"}  (parser nao aplicado)
     *   {"text": "..."} / {"message": "..."}          (outros nos)
     *   "texto puro"
     * Devolve sempre um objeto com o campo "resposta", ou null se nao achar texto nenhum.
     */
    private JsonNode normalizarResposta(String bruto) {
        if (bruto == null || bruto.isBlank()) {
            return null;
        }
        JsonNode no;
        try {
            no = objectMapper.readTree(bruto);
        } catch (JsonProcessingException e) {
            // nao e JSON: usa o texto como resposta
            return objectMapper.createObjectNode().put("resposta", bruto.trim());
        }
        return extrairObjetoComResposta(no, 0);
    }

    private JsonNode extrairObjetoComResposta(JsonNode no, int profundidade) {
        if (no == null || no.isNull() || profundidade > 4) {
            return null;
        }
        if (no.isArray()) {
            return no.isEmpty() ? null : extrairObjetoComResposta(no.get(0), profundidade + 1);
        }
        if (no.isTextual()) {
            String texto = no.asText().trim();
            // o agente as vezes devolve o JSON como texto, as vezes dentro de ```json ... ```
            String semCerca = texto.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "").trim();
            if (semCerca.startsWith("{")) {
                try {
                    JsonNode interno = extrairObjetoComResposta(objectMapper.readTree(semCerca), profundidade + 1);
                    if (interno != null) {
                        return interno;
                    }
                } catch (JsonProcessingException ignorado) {
                    // segue e usa como texto
                }
            }
            return texto.isEmpty() ? null : objectMapper.createObjectNode().put("resposta", texto);
        }
        if (no.isObject()) {
            JsonNode resposta = no.get("resposta");
            if (resposta != null && !resposta.isNull() && !resposta.asText().isBlank()) {
                return no;
            }
            for (String campo : new String[]{"output", "text", "message", "mensagem"}) {
                JsonNode interno = extrairObjetoComResposta(no.get(campo), profundidade + 1);
                if (interno != null) {
                    return interno;
                }
            }
        }
        return null;
    }

    /** O agente esta pedindo algo ao usuario (ex: "Qual o preco?") -> a operacao ainda nao terminou. */
    private static boolean terminaComPergunta(String texto) {
        if (texto == null) {
            return false;
        }
        String t = texto.trim().replaceAll("[\\s\\p{So}!.)]+$", "");
        return t.endsWith("?");
    }

    /** true se o estado ainda tem dados de uma operacao em andamento (nao e vazio / "{}"). */
    private boolean temOperacaoPendente(String estado) {
        if (estado == null || estado.isBlank()) {
            return false;
        }
        try {
            JsonNode no = objectMapper.readTree(estado);
            if (no == null || no.isNull() || no.isMissingNode()) {
                return false;
            }
            return !(no.isContainerNode() && no.isEmpty());
        } catch (JsonProcessingException e) {
            return !"{}".equals(estado.trim());
        }
    }

    /** O agente deveria mandar texto, mas se mandar um objeto JSON tambem aceitamos. */
    private String estadoComoTexto(Object estado) {
        if (estado == null) {
            return "{}";
        }
        if (estado instanceof String s) {
            return s.isBlank() ? "{}" : limitar(s, 4000);
        }
        try {
            return limitar(objectMapper.writeValueAsString(estado), 4000);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private void salvarMensagem(String sessaoId, Papel papel, String conteudo, Boolean concluido, Boolean erro) {
        ChatMensagem m = new ChatMensagem();
        m.setSessaoId(sessaoId);
        m.setPapel(papel);
        m.setConteudo(limitar(conteudo, 4000));
        m.setConcluido(concluido);
        m.setErro(erro);
        mensagemRepository.save(m);
    }

    private static String limitar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}