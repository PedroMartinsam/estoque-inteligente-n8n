package com.estoque.sistema.repository;

import com.estoque.sistema.model.ComandoAutomacao;
import com.estoque.sistema.model.ComandoAutomacao.StatusComando;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ComandoAutomacaoRepository extends JpaRepository<ComandoAutomacao, Long> {

    List<ComandoAutomacao> findByStatus(StatusComando status);

    List<ComandoAutomacao> findAllByOrderByDataEnvioDesc();

    List<ComandoAutomacao> findTop10ByOrderByDataEnvioDesc();

    List<ComandoAutomacao> findAllByOrderByDataEnvioDesc(Pageable pageable);

    List<ComandoAutomacao> findBySessaoIdAndStatus(String sessaoId, StatusComando status);

    long countByStatus(StatusComando status);

    // --- painel da tela de Automacao: so comandos que vieram do chat (sessao_id preenchido) ---

    @Query("SELECT c FROM ComandoAutomacao c WHERE c.sessaoId IS NOT NULL AND c.dataEnvio >= :desde " +
            "ORDER BY c.dataEnvio DESC, c.id DESC")
    List<ComandoAutomacao> findDoChatDesde(@Param("desde") LocalDateTime desde, Pageable pageable);

    @Query("SELECT c.status, COUNT(c) FROM ComandoAutomacao c WHERE c.sessaoId IS NOT NULL AND c.dataEnvio >= :desde " +
            "GROUP BY c.status")
    List<Object[]> contarDoChatPorStatusDesde(@Param("desde") LocalDateTime desde);

    /** Comandos que ficaram em PROCESSANDO e ninguem terminou a conversa. */
    @Modifying
    @Query("UPDATE ComandoAutomacao c SET c.status = :novo, c.resultado = :mensagem, c.dataProcessamento = :agora " +
            "WHERE c.status = :atual AND c.sessaoId IS NOT NULL AND c.dataEnvio < :limite")
    int expirarParados(@Param("atual") StatusComando atual,
                       @Param("novo") StatusComando novo,
                       @Param("mensagem") String mensagem,
                       @Param("agora") LocalDateTime agora,
                       @Param("limite") LocalDateTime limite);
}