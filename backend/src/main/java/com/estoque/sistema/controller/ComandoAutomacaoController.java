package com.estoque.sistema.controller;

import com.estoque.sistema.dto.ComandoDTO;
import com.estoque.sistema.model.ComandoAutomacao;
import com.estoque.sistema.model.ComandoAutomacao.StatusComando;
import com.estoque.sistema.service.ComandoAutomacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comandos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Comandos de Automacao", description = "API para comandos de automacao com n8n")
public class ComandoAutomacaoController {

    private final ComandoAutomacaoService comandoService;

    @GetMapping
    @Operation(summary = "Listar todos os comandos")
    public ResponseEntity<List<ComandoAutomacao>> listarTodos() {
        return ResponseEntity.ok(comandoService.listarTodos());
    }

    @GetMapping("/ultimos")
    @Operation(summary = "Listar ultimos 10 comandos")
    public ResponseEntity<List<ComandoAutomacao>> listarUltimos() {
        return ResponseEntity.ok(comandoService.listarUltimos());
    }

    @GetMapping("/recentes")
    @Operation(summary = "Comandos do chat mais recentes (periodo: hoje | tudo; limite padrao 30, max 100)")
    public ResponseEntity<List<ComandoAutomacao>> listarRecentes(
            @RequestParam(defaultValue = "30") int limite,
            @RequestParam(defaultValue = "tudo") String periodo) {
        return ResponseEntity.ok(comandoService.listarRecentes(limite, periodo));
    }

    @GetMapping("/resumo")
    @Operation(summary = "Quantidade de comandos do chat por status (periodo: hoje | tudo)")
    public ResponseEntity<Map<String, Long>> resumo(@RequestParam(defaultValue = "tudo") String periodo) {
        return ResponseEntity.ok(comandoService.resumo(periodo));
    }

    @DeleteMapping
    @Operation(summary = "Zerar: apaga todo o historico de comandos (o estoque nao muda)")
    public ResponseEntity<Map<String, Long>> zerar() {
        return ResponseEntity.ok(Map.of("apagados", comandoService.zerar()));
    }

    /** Mesmo que o DELETE acima, via POST (passa por configuracoes de CORS que nao liberam DELETE). */
    @PostMapping("/zerar")
    @Operation(summary = "Zerar (via POST): apaga todo o historico de comandos (o estoque nao muda)")
    public ResponseEntity<Map<String, Long>> zerarViaPost() {
        return ResponseEntity.ok(Map.of("apagados", comandoService.zerar()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar comando por ID")
    public ResponseEntity<ComandoAutomacao> buscarPorId(@PathVariable Long id) {
        return comandoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pendentes")
    @Operation(summary = "Listar comandos pendentes (para n8n)")
    public ResponseEntity<List<ComandoAutomacao>> listarPendentes() {
        return ResponseEntity.ok(comandoService.listarPendentes());
    }

    @PostMapping
    @Operation(summary = "Enviar novo comando")
    public ResponseEntity<ComandoAutomacao> enviarComando(@Valid @RequestBody ComandoDTO comandoDTO) {
        ComandoAutomacao comando = comandoService.enviarComando(comandoDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(comando);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status do comando (usado pelo n8n)")
    public ResponseEntity<ComandoAutomacao> atualizarStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String statusStr = body.get("status");
        String resultado = body.get("resultado");

        StatusComando status = StatusComando.valueOf(statusStr.toUpperCase());
        ComandoAutomacao comando = comandoService.atualizarStatus(id, status, resultado);

        return ResponseEntity.ok(comando);
    }

    @PatchMapping("/{id}/concluido")
    @Operation(summary = "Marcar comando como concluido")
    public ResponseEntity<ComandoAutomacao> marcarComoConcluido(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String resultado = body.getOrDefault("resultado", "Comando executado com sucesso");
        ComandoAutomacao comando = comandoService.marcarComoConcluido(id, resultado);

        return ResponseEntity.ok(comando);
    }

    @PatchMapping("/{id}/erro")
    @Operation(summary = "Marcar comando como erro")
    public ResponseEntity<ComandoAutomacao> marcarComoErro(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String erro = body.getOrDefault("erro", "Erro ao executar comando");
        ComandoAutomacao comando = comandoService.marcarComoErro(id, erro);

        return ResponseEntity.ok(comando);
    }
}