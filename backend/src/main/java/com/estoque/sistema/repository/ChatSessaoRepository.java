package com.estoque.sistema.repository;

import com.estoque.sistema.model.ChatSessao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessaoRepository extends JpaRepository<ChatSessao, String> {
}
