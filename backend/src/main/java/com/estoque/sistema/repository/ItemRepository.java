package com.estoque.sistema.repository;

import com.estoque.sistema.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    // --- Consultas que consideram SOMENTE itens ativos (ativo = true ou null) ---

    @Query("SELECT i FROM Item i WHERE COALESCE(i.ativo, true) = true ORDER BY i.nome")
    List<Item> findAtivos();

    @Query("SELECT i FROM Item i WHERE COALESCE(i.ativo, true) = true AND i.id = :id")
    Optional<Item> findAtivoById(@Param("id") Long id);

    @Query("SELECT i FROM Item i WHERE COALESCE(i.ativo, true) = true AND LOWER(i.nome) = LOWER(:nome)")
    Optional<Item> findAtivoByNome(@Param("nome") String nome);

    @Query("SELECT i FROM Item i WHERE COALESCE(i.ativo, true) = true " +
            "AND LOWER(i.nome) LIKE LOWER(CONCAT('%', :nome, '%')) ORDER BY i.nome")
    List<Item> findAtivosByNomeContendo(@Param("nome") String nome);

    @Query("SELECT i FROM Item i WHERE COALESCE(i.ativo, true) = true " +
            "AND LOWER(i.categoria) = LOWER(:categoria) ORDER BY i.nome")
    List<Item> findAtivosByCategoria(@Param("categoria") String categoria);

    /** Precisam de reposicao: zerados, ou com saldo igual/abaixo do minimo (quando o minimo existe). */
    @Query("SELECT i FROM Item i WHERE COALESCE(i.ativo, true) = true " +
            "AND (i.quantidade <= 0 OR (i.quantidadeMinima IS NOT NULL AND i.quantidade <= i.quantidadeMinima)) " +
            "ORDER BY i.quantidade, i.nome")
    List<Item> findItensEstoqueBaixo();

    @Query("SELECT DISTINCT i.categoria FROM Item i WHERE COALESCE(i.ativo, true) = true " +
            "AND i.categoria IS NOT NULL")
    List<String> findAllCategorias();

    // --- Consultas que incluem inativos (usadas para reativar em vez de duplicar) ---

    Optional<Item> findFirstByNomeIgnoreCase(String nome);

    @Query("SELECT i FROM Item i WHERE COALESCE(i.ativo, true) = false ORDER BY i.nome")
    List<Item> findInativos();

    // --- Metodos antigos, mantidos para nao quebrar outras partes do sistema ---
    // (atencao: estes NAO filtram inativos)

    Optional<Item> findByNomeIgnoreCase(String nome);

    List<Item> findByNomeContainingIgnoreCase(String nome);

    List<Item> findByCategoriaIgnoreCase(String categoria);

    boolean existsByNomeIgnoreCase(String nome);
}