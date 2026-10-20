package com.estoque.sistema.controller;

import com.estoque.sistema.dto.ItemDTO;
import com.estoque.sistema.model.Item;
import com.estoque.sistema.service.ItemService;
import com.estoque.sistema.service.ProtecaoRepeticao;
import jakarta.persistence.EntityNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/itens")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Itens", description = "API para gerenciamento de itens do estoque")
public class ItemController {

    private final ItemService itemService;
    private final ProtecaoRepeticao protecaoRepeticao;

    // ================================================================ CONSULTAS

    @GetMapping
    @Operation(summary = "Listar todos os itens ativos")
    public ResponseEntity<List<Item>> listarTodos() {
        return ResponseEntity.ok(itemService.listarTodos());
    }

    @GetMapping("/inativos")
    @Operation(summary = "Listar itens inativos (excluidos logicamente)")
    public ResponseEntity<List<Item>> listarInativos() {
        return ResponseEntity.ok(itemService.listarInativos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar item por ID")
    public ResponseEntity<Item> buscarPorId(@PathVariable Long id) {
        return itemService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nome/{nome}")
    @Operation(summary = "Buscar item por nome exato (404 se nao existir)")
    public ResponseEntity<Item> buscarPorNome(@PathVariable String nome) {
        return itemService.buscarPorNome(nome)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Versao para o agente do n8n: sempre responde 200 e diz se encontrou ou nao.
     * (Um 404 vira "erro de ferramenta" no agente e ele se perde.)
     */
    @GetMapping("/buscar-exato")
    @Operation(summary = "Buscar item por nome exato (sempre 200, com campo 'encontrado')")
    public ResponseEntity<Map<String, Object>> buscarExato(@RequestParam String nome) {
        Map<String, Object> resposta = new LinkedHashMap<>();
        itemService.buscarPorNome(nome).ifPresentOrElse(
                item -> {
                    resposta.put("encontrado", true);
                    resposta.put("item", item);
                },
                () -> {
                    resposta.put("encontrado", false);
                    resposta.put("mensagem", "Nenhum produto ativo com o nome '" + nome.trim() + "'.");
                });
        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/buscar")
    @Operation(summary = "Buscar itens por nome (parcial)")
    public ResponseEntity<List<Item>> buscarPorNomeContendo(@RequestParam String nome) {
        return ResponseEntity.ok(itemService.buscarPorNomeContendo(nome));
    }

    @GetMapping("/categoria/{categoria}")
    @Operation(summary = "Buscar itens por categoria")
    public ResponseEntity<List<Item>> buscarPorCategoria(@PathVariable String categoria) {
        return ResponseEntity.ok(itemService.buscarPorCategoria(categoria));
    }

    @GetMapping("/estoque-baixo")
    @Operation(summary = "Listar itens com estoque baixo")
    public ResponseEntity<List<Item>> listarItensEstoqueBaixo() {
        return ResponseEntity.ok(itemService.listarItensEstoqueBaixo());
    }

    @GetMapping("/categorias")
    @Operation(summary = "Listar todas as categorias")
    public ResponseEntity<List<String>> listarCategorias() {
        return ResponseEntity.ok(itemService.listarCategorias());
    }

    // ================================================================ CADASTRO / ALTERACAO

    @PostMapping
    @Operation(summary = "Cadastrar novo item (reativa se existir inativo com o mesmo nome)")
    public ResponseEntity<Item> salvar(@Valid @RequestBody ItemDTO itemDTO) {
        Item item = itemService.salvar(itemDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar item existente")
    public ResponseEntity<Item> atualizar(@PathVariable Long id, @Valid @RequestBody ItemDTO itemDTO) {
        return ResponseEntity.ok(itemService.atualizar(id, itemDTO));
    }

    @PatchMapping("/{id}/quantidade")
    @Operation(summary = "Definir quantidade exata do item")
    public ResponseEntity<Item> atualizarQuantidade(@PathVariable Long id, @RequestParam Integer quantidade) {
        return ResponseEntity.ok(itemService.atualizarQuantidade(id, quantidade));
    }

    @PatchMapping("/{id}/adicionar")
    @Operation(summary = "Adicionar quantidade ao item")
    public ResponseEntity<Item> adicionarQuantidade(@PathVariable Long id, @RequestParam Integer quantidade) {
        return ResponseEntity.ok(itemService.adicionarQuantidade(id, quantidade));
    }

    // ================================================================ OPERACOES POR NOME (n8n)
    // Usadas pelo agente de IA. Sempre respondem 200 com um texto claro:
    //   {"ok": true,  "mensagem": "...", "item": {...}}
    //   {"ok": false, "erro": "..."}            -> operacao NAO aconteceu
    //   {"ok": false, "aviso": "JA EXECUTADA"}  -> repeticao bloqueada (agente em loop)

    @PostMapping("/adicionar-por-nome")
    @Operation(summary = "Entrada de estoque por nome (so produto ja cadastrado)")
    public ResponseEntity<Map<String, Object>> adicionarQuantidadePorNome(
            @RequestParam String nome,
            @RequestParam Integer quantidade) {
        return executarPorNome("entrada", nome, quantidade,
                "Entrada de " + quantidade + " em '" + nome.trim() + "'",
                () -> itemService.adicionarQuantidadePorNome(nome, quantidade));
    }

    @PostMapping("/remover-por-nome")
    @Operation(summary = "Saida de estoque por nome (nunca deixa negativo)")
    public ResponseEntity<Map<String, Object>> removerQuantidadePorNome(
            @RequestParam String nome,
            @RequestParam Integer quantidade) {
        return executarPorNome("saida", nome, quantidade,
                "Saida de " + quantidade + " de '" + nome.trim() + "'",
                () -> itemService.removerQuantidadePorNome(nome, quantidade));
    }

    @PatchMapping("/definir-por-nome")
    @Operation(summary = "Definir saldo exato por nome (correcao manual)")
    public ResponseEntity<Map<String, Object>> definirQuantidadePorNome(
            @RequestParam String nome,
            @RequestParam Integer quantidade) {
        return executarPorNome("definir", nome, quantidade,
                "Saldo de '" + nome.trim() + "' definido para " + quantidade,
                () -> itemService.definirQuantidadePorNome(nome, quantidade));
    }

    @PatchMapping("/inativar-por-nome")
    @Operation(summary = "Inativar (exclusao logica) por nome")
    public ResponseEntity<Map<String, Object>> inativarPorNome(@RequestParam String nome) {
        return executarPorNome("inativar", nome, "-",
                "Produto '" + nome.trim() + "' inativado",
                () -> itemService.inativarPorNome(nome));
    }

    private ResponseEntity<Map<String, Object>> executarPorNome(String operacao, String nome, Object valor,
                                                                String descricao, Supplier<Item> acao) {
        Map<String, Object> corpo = new LinkedHashMap<>();

        Optional<Long> haSegundos = protecaoRepeticao.feitaRecentemente(operacao, nome, valor);
        if (haSegundos.isPresent()) {
            corpo.put("ok", false);
            corpo.put("aviso", "OPERACAO JA EXECUTADA ha " + haSegundos.get() + " s: " + descricao
                    + ". Ela NAO foi repetida. Nao chame esta ferramenta de novo; apenas confirme ao usuario.");
            itemService.buscarPorNome(nome).ifPresent(i -> corpo.put("saldoAtual", i.getQuantidade()));
            return ResponseEntity.ok(corpo);
        }

        try {
            Item item = acao.get();
            protecaoRepeticao.registrar(operacao, nome, valor);
            corpo.put("ok", true);
            corpo.put("mensagem", descricao + ". Saldo atual: " + item.getQuantidade()
                    + ". Operacao concluida: nao chame esta ferramenta de novo.");
            corpo.put("item", item);
        } catch (EntityNotFoundException | IllegalStateException | IllegalArgumentException e) {
            corpo.put("ok", false);
            corpo.put("erro", e.getMessage() + " (nada foi alterado)");
        }
        return ResponseEntity.ok(corpo);
    }

    // ================================================================ EXCLUSAO LOGICA

    @PatchMapping("/{id}/inativar")
    @Operation(summary = "Inativar item por ID")
    public ResponseEntity<Item> inativar(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.inativar(id));
    }

    @PatchMapping("/{id}/reativar")
    @Operation(summary = "Reativar item inativo")
    public ResponseEntity<Item> reativar(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.reativar(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir item (exclusao logica: apenas inativa)")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        itemService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}