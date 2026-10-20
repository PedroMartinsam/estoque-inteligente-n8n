# Documentacao de Casos de Uso e Atores

## Sistema de Gerenciamento de Estoque com Automacao n8n

---

## 1. ATORES DO SISTEMA

### 1.1 Atores Primarios

#### Usuario
- **Descricao**: Pessoa responsavel pela gestao do estoque da empresa
- **Tipo**: Humano
- **Responsabilidades**:
  - Cadastrar, editar e excluir itens do estoque
  - Consultar itens e suas quantidades
  - Visualizar dashboard com estatisticas
  - Enviar comandos em linguagem natural para automacao
  - Monitorar historico de comandos processados
- **Requisitos de Acesso**: Navegador web com acesso ao sistema

### 1.2 Atores Secundarios

#### n8n (Plataforma de Automacao)
- **Descricao**: Sistema externo de automacao que processa comandos em linguagem natural
- **Tipo**: Sistema Externo
- **Responsabilidades**:
  - Consultar comandos pendentes via API REST
  - Processar linguagem natural usando IA (ChatGPT/OpenAI)
  - Extrair informacoes dos comandos (nome do item, quantidade)
  - Executar cadastros automaticos via API
  - Atualizar status dos comandos processados
- **Comunicacao**: API REST (HTTP)

#### Sistema
- **Descricao**: Processos automaticos internos do sistema
- **Tipo**: Sistema Interno
- **Responsabilidades**:
  - Calcular estatisticas do dashboard
  - Identificar itens com estoque baixo
  - Manter consistencia dos dados
  - Registrar timestamps de operacoes

---

## 2. CASOS DE USO

### UC01 - Cadastrar Item

| Campo | Descricao |
|-------|-----------|
| **ID** | UC01 |
| **Nome** | Cadastrar Item |
| **Ator Principal** | Usuario |
| **Descricao** | Permite ao usuario cadastrar um novo item no estoque |
| **Pre-condicoes** | Usuario deve estar na pagina de cadastro |
| **Pos-condicoes** | Item cadastrado e disponivel na listagem |

**Fluxo Principal:**
1. Usuario acessa a pagina de cadastro
2. Sistema exibe formulario de cadastro
3. Usuario preenche os dados do item (nome, quantidade, categoria, preco, etc.)
4. Usuario clica em "Cadastrar"
5. Sistema valida os dados
6. Sistema salva o item no banco de dados
7. Sistema exibe mensagem de sucesso
8. Sistema redireciona para listagem de itens

**Fluxo Alternativo - Dados Invalidos:**
5a. Sistema detecta dados invalidos
5b. Sistema exibe mensagens de erro
5c. Usuario corrige os dados
5d. Retorna ao passo 4

---

### UC02 - Editar Item

| Campo | Descricao |
|-------|-----------|
| **ID** | UC02 |
| **Nome** | Editar Item |
| **Ator Principal** | Usuario |
| **Descricao** | Permite ao usuario editar um item existente |
| **Pre-condicoes** | Item deve existir no sistema |
| **Pos-condicoes** | Dados do item atualizados |

**Fluxo Principal:**
1. Usuario acessa a listagem de itens
2. Usuario clica no botao "Editar" do item desejado
3. Sistema carrega dados do item no formulario
4. Usuario altera os dados necessarios
5. Usuario clica em "Atualizar"
6. Sistema valida e salva as alteracoes
7. Sistema exibe mensagem de sucesso

---

### UC03 - Excluir Item

| Campo | Descricao |
|-------|-----------|
| **ID** | UC03 |
| **Nome** | Excluir Item |
| **Ator Principal** | Usuario |
| **Descricao** | Permite ao usuario excluir um item do estoque |
| **Pre-condicoes** | Item deve existir no sistema |
| **Pos-condicoes** | Item removido do banco de dados |

**Fluxo Principal:**
1. Usuario acessa a listagem de itens
2. Usuario clica no botao "Excluir" do item desejado
3. Sistema exibe modal de confirmacao
4. Usuario confirma a exclusao
5. Sistema remove o item do banco de dados
6. Sistema atualiza a listagem
7. Sistema exibe mensagem de sucesso

**Fluxo Alternativo - Cancelar Exclusao:**
4a. Usuario clica em "Cancelar"
4b. Sistema fecha o modal
4c. Caso de uso encerrado

---

### UC04 - Listar Itens

| Campo | Descricao |
|-------|-----------|
| **ID** | UC04 |
| **Nome** | Listar Itens |
| **Ator Principal** | Usuario |
| **Descricao** | Exibe todos os itens cadastrados no estoque |
| **Pre-condicoes** | Nenhuma |
| **Pos-condicoes** | Lista de itens exibida na tela |

**Fluxo Principal:**
1. Usuario acessa a pagina de itens
2. Sistema consulta todos os itens no banco de dados
3. Sistema exibe tabela com os itens (ID, nome, quantidade, categoria, preco, etc.)

---

### UC05 - Buscar Item

| Campo | Descricao |
|-------|-----------|
| **ID** | UC05 |
| **Nome** | Buscar Item |
| **Ator Principal** | Usuario |
| **Descricao** | Permite buscar itens por nome |
| **Pre-condicoes** | Estar na pagina de listagem |
| **Pos-condicoes** | Itens filtrados exibidos |

**Fluxo Principal:**
1. Usuario digita termo de busca no campo de pesquisa
2. Usuario pressiona Enter ou clica em buscar
3. Sistema filtra itens que contenham o termo
4. Sistema exibe resultados filtrados

---

### UC06 - Visualizar Estoque Baixo

| Campo | Descricao |
|-------|-----------|
| **ID** | UC06 |
| **Nome** | Visualizar Estoque Baixo |
| **Ator Principal** | Usuario |
| **Descricao** | Exibe itens com quantidade abaixo do minimo |
| **Pre-condicoes** | Nenhuma |
| **Pos-condicoes** | Lista de itens com estoque baixo exibida |

**Fluxo Principal:**
1. Sistema identifica itens onde quantidade <= quantidade_minima
2. Sistema destaca esses itens na interface (cor vermelha)
3. Dashboard exibe contador de itens com estoque baixo

---

### UC07 - Enviar Comando em Linguagem Natural

| Campo | Descricao |
|-------|-----------|
| **ID** | UC07 |
| **Nome** | Enviar Comando em Linguagem Natural |
| **Ator Principal** | Usuario |
| **Descricao** | Usuario envia um comando de texto para processamento automatico |
| **Pre-condicoes** | Usuario na pagina de automacao |
| **Pos-condicoes** | Comando salvo com status "PENDENTE" |

**Fluxo Principal:**
1. Usuario acessa a pagina de automacao
2. Usuario digita comando em linguagem natural (ex: "Cadastre 5 batatas e 6 macas")
3. Usuario clica em "Enviar Comando"
4. Sistema salva o comando com status "PENDENTE"
5. Sistema exibe mensagem de sucesso
6. Comando aparece no historico

**Exemplos de Comandos:**
- "Cadastre 5 batatas e 6 macas no estoque"
- "Adicione 10 unidades de arroz"
- "Registre 3 caixas de leite na categoria laticinios"

---

### UC08 - Processar Comando (n8n)

| Campo | Descricao |
|-------|-----------|
| **ID** | UC08 |
| **Nome** | Processar Comando |
| **Ator Principal** | n8n |
| **Descricao** | n8n busca e processa comandos pendentes |
| **Pre-condicoes** | Existir comandos com status "PENDENTE" |
| **Pos-condicoes** | Comando processado e itens cadastrados |

**Fluxo Principal:**
1. n8n consulta endpoint GET /api/comandos/pendentes
2. Sistema retorna lista de comandos pendentes
3. n8n envia comando para IA (ChatGPT) para interpretacao
4. IA extrai informacoes (nome do item, quantidade)
5. n8n executa POST /api/itens/adicionar-por-nome para cada item
6. n8n atualiza status do comando para "CONCLUIDO"
7. n8n registra resultado do processamento

---

### UC09 - Cadastrar Item Automaticamente

| Campo | Descricao |
|-------|-----------|
| **ID** | UC09 |
| **Nome** | Cadastrar Item Automaticamente |
| **Ator Principal** | n8n |
| **Descricao** | n8n cadastra item extraido do comando |
| **Pre-condicoes** | Comando processado com sucesso |
| **Pos-condicoes** | Item cadastrado ou quantidade atualizada |

**Fluxo Principal:**
1. n8n envia requisicao POST /api/itens/adicionar-por-nome
2. Sistema verifica se item existe pelo nome
3a. Se existe: Sistema adiciona quantidade ao item
3b. Se nao existe: Sistema cria novo item com a quantidade
4. Sistema retorna item criado/atualizado

---

### UC10 - Atualizar Status do Comando

| Campo | Descricao |
|-------|-----------|
| **ID** | UC10 |
| **Nome** | Atualizar Status do Comando |
| **Ator Principal** | n8n |
| **Descricao** | n8n atualiza o status apos processamento |
| **Pre-condicoes** | Comando foi processado |
| **Pos-condicoes** | Status atualizado (CONCLUIDO ou ERRO) |

**Fluxo Principal:**
1. n8n envia PATCH /api/comandos/{id}/concluido com resultado
2. Sistema atualiza status para "CONCLUIDO"
3. Sistema registra data de processamento
4. Sistema salva mensagem de resultado

**Fluxo Alternativo - Erro no Processamento:**
1a. n8n envia PATCH /api/comandos/{id}/erro com mensagem de erro
2a. Sistema atualiza status para "ERRO"
3a. Sistema registra mensagem de erro

---

### UC11 - Visualizar Dashboard

| Campo | Descricao |
|-------|-----------|
| **ID** | UC11 |
| **Nome** | Visualizar Dashboard |
| **Ator Principal** | Usuario |
| **Descricao** | Exibe painel com informacoes resumidas |
| **Pre-condicoes** | Nenhuma |
| **Pos-condicoes** | Dashboard exibido |

**Fluxo Principal:**
1. Usuario acessa a pagina inicial
2. Sistema calcula estatisticas
3. Sistema exibe cards com: total de itens, categorias, estoque baixo

---

### UC12 - Visualizar Estatisticas

| Campo | Descricao |
|-------|-----------|
| **ID** | UC12 |
| **Nome** | Visualizar Estatisticas |
| **Ator Principal** | Sistema |
| **Descricao** | Sistema calcula e exibe estatisticas |
| **Pre-condicoes** | Dados existentes no banco |
| **Pos-condicoes** | Estatisticas calculadas e exibidas |

**Fluxo Principal:**
1. Sistema conta total de itens cadastrados
2. Sistema conta categorias distintas
3. Sistema identifica itens com estoque baixo
4. Sistema retorna dados para o dashboard

---

### UC13 - Visualizar Historico de Comandos

| Campo | Descricao |
|-------|-----------|
| **ID** | UC13 |
| **Nome** | Visualizar Historico de Comandos |
| **Ator Principal** | Usuario |
| **Descricao** | Exibe historico de comandos enviados |
| **Pre-condicoes** | Estar na pagina de automacao |
| **Pos-condicoes** | Historico exibido |

**Fluxo Principal:**
1. Usuario acessa pagina de automacao
2. Sistema consulta ultimos 10 comandos
3. Sistema exibe lista com: comando, status, data, resultado
4. Sistema atualiza lista automaticamente a cada 5 segundos

---

## 3. MATRIZ DE RASTREABILIDADE

| Caso de Uso | Usuario | n8n | Sistema |
|-------------|:-------:|:---:|:-------:|
| UC01 - Cadastrar Item | X | | |
| UC02 - Editar Item | X | | |
| UC03 - Excluir Item | X | | |
| UC04 - Listar Itens | X | | |
| UC05 - Buscar Item | X | | |
| UC06 - Visualizar Estoque Baixo | X | | |
| UC07 - Enviar Comando | X | | |
| UC08 - Processar Comando | | X | |
| UC09 - Cadastrar Automaticamente | | X | |
| UC10 - Atualizar Status | | X | |
| UC11 - Visualizar Dashboard | X | | |
| UC12 - Visualizar Estatisticas | | | X |
| UC13 - Visualizar Historico | X | | |

---

## 4. FLUXO DE AUTOMACAO n8n

```
[Usuario] --> [Sistema Web] --> [API Backend] --> [Banco de Dados]
                                      |
                                      v
                              [GET /comandos/pendentes]
                                      |
                                      v
                                  [n8n Workflow]
                                      |
                                      v
                              [OpenAI/ChatGPT API]
                                      |
                                      v
                              [Extrair: nome, quantidade]
                                      |
                                      v
                              [POST /itens/adicionar-por-nome]
                                      |
                                      v
                              [PATCH /comandos/{id}/concluido]
```

---

## 5. ENDPOINTS DA API PARA n8n

### Obter comandos pendentes
```
GET /api/comandos/pendentes
Response: [{ id, comando, status, dataEnvio }]
```

### Cadastrar/adicionar item por nome
```
POST /api/itens/adicionar-por-nome?nome=batata&quantidade=5
Response: { id, nome, quantidade, ... }
```

### Marcar comando como concluido
```
PATCH /api/comandos/{id}/concluido
Body: { "resultado": "5 batatas e 6 macas cadastradas" }
Response: { id, status: "CONCLUIDO", resultado, dataProcessamento }
```

### Marcar comando como erro
```
PATCH /api/comandos/{id}/erro
Body: { "erro": "Nao foi possivel interpretar o comando" }
Response: { id, status: "ERRO", resultado, dataProcessamento }
```
