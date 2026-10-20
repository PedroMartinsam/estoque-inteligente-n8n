package com.estoque.sistema.service;

import com.estoque.sistema.dto.ItemDTO;
import com.estoque.sistema.model.Item;
import com.estoque.sistema.repository.ItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    // ------------------------------------------------------------------ leitura

    public List<Item> listarTodos() {
        return itemRepository.findAtivos();
    }

    public List<Item> listarInativos() {
        return itemRepository.findInativos();
    }

    public Optional<Item> buscarPorId(Long id) {
        return itemRepository.findAtivoById(id);
    }

    public Optional<Item> buscarPorNome(String nome) {
        return itemRepository.findAtivoByNome(nome.trim());
    }

    public List<Item> buscarPorNomeContendo(String nome) {
        return itemRepository.findAtivosByNomeContendo(nome.trim());
    }

    public List<Item> buscarPorCategoria(String categoria) {
        return itemRepository.findAtivosByCategoria(categoria.trim());
    }

    public List<Item> listarItensEstoqueBaixo() {
        return itemRepository.findItensEstoqueBaixo();
    }

    public List<String> listarCategorias() {
        return itemRepository.findAllCategorias();
    }

    // ------------------------------------------------------------------ escrita

    /**
     * Cadastra um item novo. Se ja existir um item ATIVO com o mesmo nome, recusa
     * (evita duplicar). Se existir um INATIVO com o mesmo nome, reativa ele com os
     * dados novos em vez de criar outro registro.
     */
    @Transactional
    public Item salvar(ItemDTO itemDTO) {
        String nome = itemDTO.getNome().trim();
        Optional<Item> existente = itemRepository.findFirstByNomeIgnoreCase(nome);

        if (existente.isPresent() && existente.get().estaAtivo()) {
            throw new IllegalStateException("Ja existe um item ativo com o nome '" + nome
                    + "' (id " + existente.get().getId() + "). Use a entrada de estoque ou a atualizacao.");
        }

        Item item = existente.orElseGet(Item::new);
        copiarCampos(itemDTO, item);
        item.setNome(nome);
        item.setAtivo(true);
        return itemRepository.save(item);
    }

    @Transactional
    public Item atualizar(Long id, ItemDTO itemDTO) {
        Item item = buscarAtivoOuFalhar(id);

        String novoNome = itemDTO.getNome().trim();
        itemRepository.findAtivoByNome(novoNome)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new IllegalStateException("Ja existe outro item com o nome '" + novoNome + "'.");
                });

        copiarCampos(itemDTO, item);
        item.setNome(novoNome);
        return itemRepository.save(item);
    }

    @Transactional
    public Item atualizarQuantidade(Long id, Integer quantidade) {
        validarNaoNegativo(quantidade);
        Item item = buscarAtivoOuFalhar(id);
        item.setQuantidade(quantidade);
        return itemRepository.save(item);
    }

    @Transactional
    public Item adicionarQuantidade(Long id, Integer quantidade) {
        validarPositivo(quantidade);
        Item item = buscarAtivoOuFalhar(id);
        item.setQuantidade(item.getQuantidade() + quantidade);
        return itemRepository.save(item);
    }

    /**
     * Entrada de estoque por nome. SO funciona para produto que ja existe e esta ativo.
     * (Antes criava o produto sozinho, e o agente de IA acabava cadastrando coisas sem preco.)
     */
    @Transactional
    public Item adicionarQuantidadePorNome(String nome, Integer quantidade) {
        validarPositivo(quantidade);
        String nomeLimpo = nome.trim();
        Item item = itemRepository.findAtivoByNome(nomeLimpo).orElseThrow(() ->
                new EntityNotFoundException("Produto '" + nomeLimpo + "' nao esta cadastrado, entao a entrada NAO foi feita. "
                        + "Para cadastrar, pergunte ao usuario a quantidade e o preco e use cadastrar_item."));
        item.setQuantidade(item.getQuantidade() + quantidade);
        return itemRepository.save(item);
    }

    /** Saida de estoque por nome. Nunca deixa o saldo negativo. */
    @Transactional
    public Item removerQuantidadePorNome(String nome, Integer quantidade) {
        validarPositivo(quantidade);
        Item item = buscarAtivoPorNomeOuFalhar(nome);

        if (quantidade > item.getQuantidade()) {
            throw new IllegalStateException("Saldo insuficiente de '" + item.getNome() + "': ha "
                    + item.getQuantidade() + " em estoque e foi pedida a saida de " + quantidade + ".");
        }
        item.setQuantidade(item.getQuantidade() - quantidade);
        return itemRepository.save(item);
    }

    /** Correcao manual: define o saldo exato. */
    @Transactional
    public Item definirQuantidadePorNome(String nome, Integer quantidade) {
        validarNaoNegativo(quantidade);
        Item item = buscarAtivoPorNomeOuFalhar(nome);
        item.setQuantidade(quantidade);
        return itemRepository.save(item);
    }

    /** Exclusao logica por id. */
    @Transactional
    public Item inativar(Long id) {
        Item item = buscarAtivoOuFalhar(id);
        item.setAtivo(false);
        return itemRepository.save(item);
    }

    /** Exclusao logica por nome (usada pelo agente do n8n). */
    @Transactional
    public Item inativarPorNome(String nome) {
        Item item = buscarAtivoPorNomeOuFalhar(nome);
        item.setAtivo(false);
        return itemRepository.save(item);
    }

    @Transactional
    public Item reativar(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Item nao encontrado com id: " + id));
        item.setAtivo(true);
        return itemRepository.save(item);
    }

    /** Mantido por compatibilidade: agora o DELETE tambem e exclusao logica. */
    @Transactional
    public void deletar(Long id) {
        inativar(id);
    }

    // ------------------------------------------------------------------ helpers

    public Item buscarAtivoPorNomeOuFalhar(String nome) {
        return itemRepository.findAtivoByNome(nome.trim())
                .orElseThrow(() -> new EntityNotFoundException("Produto '" + nome.trim()
                        + "' nao encontrado no estoque."));
    }

    private Item buscarAtivoOuFalhar(Long id) {
        return itemRepository.findAtivoById(id)
                .orElseThrow(() -> new EntityNotFoundException("Item nao encontrado com id: " + id));
    }

    private void copiarCampos(ItemDTO dto, Item item) {
        item.setDescricao(dto.getDescricao());
        item.setQuantidade(dto.getQuantidade());
        item.setPreco(dto.getPreco());
        item.setCategoria(dto.getCategoria());
        item.setUnidade(dto.getUnidade());
        item.setQuantidadeMinima(dto.getQuantidadeMinima());
    }

    private void validarPositivo(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
    }

    private void validarNaoNegativo(Integer quantidade) {
        if (quantidade == null || quantidade < 0) {
            throw new IllegalArgumentException("A quantidade nao pode ser negativa.");
        }
    }
}