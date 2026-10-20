package com.estoque.sistema.controller;

import com.estoque.sistema.dto.ChatRequestDTO;
import com.estoque.sistema.dto.ChatRespostaDTO;
import com.estoque.sistema.model.ChatMensagem;
import com.estoque.sistema.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Chat", description = "Conversa com o agente de estoque (n8n + OpenAI)")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @Operation(summary = "Enviar mensagem ao agente e receber a resposta")
    public ResponseEntity<ChatRespostaDTO> enviar(@Valid @RequestBody ChatRequestDTO request) {
        return ResponseEntity.ok(chatService.enviarMensagem(request));
    }

    @GetMapping("/{sessaoId}/mensagens")
    @Operation(summary = "Historico de mensagens de uma sessao")
    public ResponseEntity<List<ChatMensagem>> mensagens(@PathVariable String sessaoId) {
        return ResponseEntity.ok(chatService.listarMensagens(sessaoId));
    }

    @DeleteMapping("/{sessaoId}")
    @Operation(summary = "Apagar uma conversa")
    public ResponseEntity<Void> limpar(@PathVariable String sessaoId) {
        chatService.limparSessao(sessaoId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Tratamento de erro so deste controller (tem prioridade sobre o GlobalExceptionHandler).
     * Escreve o erro completo no log e devolve a causa para a tela, para facilitar o diagnostico.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> erro(Exception e) {
        log.error("Erro no chat", e);
        Throwable causa = e;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("erro", causa.getClass().getSimpleName() + ": " + causa.getMessage());
        body.put("status", 500);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
