package com.estoque.sistema.repository;

import com.estoque.sistema.model.Embalagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmbalagemRepository extends JpaRepository<Embalagem, Long> {

    @Query("SELECT e FROM Embalagem e WHERE COALESCE(e.item.ativo, true) = true ORDER BY e.item.nome, e.codigo")
    List<Embalagem> findDeItensAtivos();

    @Query("SELECT e FROM Embalagem e WHERE COALESCE(e.item.ativo, true) = true " +
            "AND LOWER(e.item.nome) = LOWER(:nome) ORDER BY e.codigo")
    List<Embalagem> findByNomeItem(@Param("nome") String nome);

    Optional<Embalagem> findByItemIdAndCodigo(Long itemId, String codigo);
}
