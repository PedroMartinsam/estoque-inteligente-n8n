package com.estoque.sistema.service;

import com.estoque.sistema.dto.EmbalagemDTO;
import com.estoque.sistema.model.Embalagem;
import com.estoque.sistema.model.Item;
import com.estoque.sistema.repository.EmbalagemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmbalagemService {

    /** Nome padrao quando o codigo e P/M/G e ninguem informou um nome. */
    private static final Map<String, String> NOMES_PADRAO = Map.of(
            "P", "caixa pequena",
            "M", "caixa media",
            "G", "caixa grande");

    private final EmbalagemRepository embalagemRepository;
    private final ItemService itemService;

    public List<EmbalagemDTO> listarTodas() {
        return embalagemRepository.findDeItensAtivos().stream().map(EmbalagemDTO::de).toList();
    }

    public List<EmbalagemDTO> listarPorProduto(String nome) {
        return embalagemRepository.findByNomeItem(nome.trim()).stream().map(EmbalagemDTO::de).toList();
    }

    /**
     * Cria ou atualiza a embalagem (produto + codigo sao a chave).
     * nomeEmbalagem e preco sao opcionais: se vierem nulos, mantem o que ja existe.
     */
    @Transactional
    public EmbalagemDTO definirPorNome(String nomeProduto, String codigo, Integer unidades,
                                       String nomeEmbalagem, BigDecimal preco) {
        String cod = codigo == null ? "" : codigo.trim().toUpperCase();
        if (cod.isEmpty() || cod.length() > 20) {
            throw new IllegalArgumentException("Codigo de embalagem invalido: '" + codigo
                    + "'. Use um codigo curto, ex: P, M, G, FARDO, CX-M.");
        }
        if (unidades == null || unidades <= 0) {
            throw new IllegalArgumentException("A embalagem precisa ter pelo menos 1 unidade.");
        }

        Item item = itemService.buscarAtivoPorNomeOuFalhar(nomeProduto);
        Embalagem embalagem = embalagemRepository.findByItemIdAndCodigo(item.getId(), cod)
                .orElseGet(() -> {
                    Embalagem nova = new Embalagem();
                    nova.setItem(item);
                    nova.setCodigo(cod);
                    return nova;
                });

        embalagem.setUnidades(unidades);
        if (nomeEmbalagem != null && !nomeEmbalagem.isBlank()) {
            embalagem.setNome(limitar(nomeEmbalagem.trim(), 60));
        } else if (embalagem.getNome() == null) {
            embalagem.setNome(NOMES_PADRAO.getOrDefault(cod, cod.toLowerCase()));
        }
        if (preco != null) {
            embalagem.setPrecoEmbalagem(preco);
        }
        return EmbalagemDTO.de(embalagemRepository.save(embalagem));
    }

    @Transactional
    public void remover(Long id) {
        if (!embalagemRepository.existsById(id)) {
            throw new EntityNotFoundException("Embalagem nao encontrada com id: " + id);
        }
        embalagemRepository.deleteById(id);
    }

    private static String limitar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
