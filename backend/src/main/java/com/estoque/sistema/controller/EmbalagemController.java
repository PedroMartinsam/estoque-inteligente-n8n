package com.estoque.sistema.controller;

import com.estoque.sistema.dto.EmbalagemDTO;
import com.estoque.sistema.service.EmbalagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/embalagens")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Embalagens", description = "Quantas unidades cabem em cada embalagem (P, M, G, FARDO...) de um produto")
public class EmbalagemController {

    private final EmbalagemService embalagemService;

    @GetMapping
    @Operation(summary = "Listar todas as embalagens")
    public ResponseEntity<List<EmbalagemDTO>> listarTodas() {
        return ResponseEntity.ok(embalagemService.listarTodas());
    }

    @GetMapping("/produto")
    @Operation(summary = "Listar embalagens de um produto")
    public ResponseEntity<List<EmbalagemDTO>> listarPorProduto(@RequestParam String nome) {
        return ResponseEntity.ok(embalagemService.listarPorProduto(nome));
    }

    @PostMapping("/definir-por-nome")
    @Operation(summary = "Cadastrar ou atualizar embalagem de um produto")
    public ResponseEntity<EmbalagemDTO> definirPorNome(
            @RequestParam String nome,
            @RequestParam String codigo,
            @RequestParam Integer unidades,
            @RequestParam(required = false) String nomeEmbalagem,
            @RequestParam(required = false) BigDecimal preco) {
        return ResponseEntity.ok(embalagemService.definirPorNome(nome, codigo, unidades, nomeEmbalagem, preco));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover embalagem")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        embalagemService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
