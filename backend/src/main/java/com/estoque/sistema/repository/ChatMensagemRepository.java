package com.estoque.sistema.repository;

import com.estoque.sistema.model.ChatMensagem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMensagemRepository extends JpaRepository<ChatMensagem, Long> {

    List<ChatMensagem> findBySessaoIdOrderByIdAsc(String sessaoId);

    /** Ultimas N mensagens (vem da mais nova para a mais antiga). */
    List<ChatMensagem> findBySessaoIdOrderByIdDesc(String sessaoId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM ChatMensagem m WHERE m.sessaoId = :sessaoId")
    void deleteBySessaoId(@Param("sessaoId") String sessaoId);
}
