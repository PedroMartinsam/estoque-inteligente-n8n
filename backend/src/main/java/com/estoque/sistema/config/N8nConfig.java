package com.estoque.sistema.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class N8nConfig {

    /**
     * RestTemplate exclusivo para o n8n. O agente pode chamar varias tools
     * (e a OpenAI) antes de responder, por isso o timeout de leitura e longo.
     */
    @Bean(name = "n8nRestTemplate")
    public RestTemplate n8nRestTemplate(
            @Value("${n8n.timeout.conexao-ms:5000}") int timeoutConexao,
            @Value("${n8n.timeout.leitura-ms:120000}") int timeoutLeitura) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutConexao);
        factory.setReadTimeout(timeoutLeitura);
        return new RestTemplate(factory);
    }
}
