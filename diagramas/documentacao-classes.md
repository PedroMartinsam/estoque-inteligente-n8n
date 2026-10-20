# Documentacao dos Diagramas de Classes

## Sistema de Gerenciamento de Estoque

---

## 1. Visao Geral da Arquitetura

O sistema segue a arquitetura em camadas (Layered Architecture):

```
┌─────────────────────────────────────────────────────────────┐
│                    FRONTEND (React)                          │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────────────┐   │
│  │  Home   │ │  Itens  │ │Cadastro │ │    Automacao    │   │
│  └────┬────┘ └────┬────┘ └────┬────┘ └────────┬────────┘   │
│       └───────────┴───────────┴───────────────┘             │
│                         │                                    │
│                    api.js (Axios)                            │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP/REST
┌─────────────────────────┴───────────────────────────────────┐
│                    BACKEND (Spring Boot)                     │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Controller Layer (REST API)              │   │
│  │  ┌─────────────────┐    ┌──────────────────────────┐ │   │
│  │  │ ItemController  │    │ ComandoAutomacaoController│ │   │
│  │  └────────┬────────┘    └─────────────┬────────────┘ │   │
│  └───────────┼───────────────────────────┼──────────────┘   │
│              │                           │                   │
│  ┌───────────┴───────────────────────────┴──────────────┐   │
│  │              Service Layer (Business Logic)           │   │
│  │  ┌─────────────────┐    ┌──────────────────────────┐ │   │
│  │  │   ItemService   │    │ ComandoAutomacaoService  │ │   │
│  │  └────────┬────────┘    └─────────────┬────────────┘ │   │
│  └───────────┼───────────────────────────┼──────────────┘   │
│              │                           │                   │
│  ┌───────────┴───────────────────────────┴──────────────┐   │
│  │              Repository Layer (Data Access)           │   │
│  │  ┌─────────────────┐    ┌──────────────────────────┐ │   │
│  │  │ ItemRepository  │    │ComandoAutomacaoRepository│ │   │
│  │  └────────┬────────┘    └─────────────┬────────────┘ │   │
│  └───────────┼───────────────────────────┼──────────────┘   │
│              │                           │                   │
│  ┌───────────┴───────────────────────────┴──────────────┐   │
│  │              Model Layer (Entities)                   │   │
│  │  ┌─────────────────┐    ┌──────────────────────────┐ │   │
│  │  │      Item       │    │    ComandoAutomacao      │ │   │
│  │  └─────────────────┘    └──────────────────────────┘ │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────┬───────────────────────────────────┘
                          │
              ┌───────────┴───────────┐
              │     H2 Database       │
              │  ┌────────┐ ┌───────┐ │
              │  │ itens  │ │comand.│ │
              │  └────────┘ └───────┘ │
              └───────────────────────┘
```

---

## 2. Classes do Backend

### 2.1 Camada Model (Entidades)

#### Item

| Atributo | Tipo | Descricao |
|----------|------|-----------|
| id | Long | Chave primaria (auto-gerada) |
| nome | String | Nome do produto (obrigatorio) |
| descricao | String | Descricao detalhada |
| quantidade | Integer | Quantidade em estoque (obrigatorio) |
| preco | BigDecimal | Preco unitario |
| categoria | String | Categoria do produto |
| unidade | String | Unidade de medida |
| quantidadeMinima | Integer | Limite para alerta de estoque baixo |
| dataCadastro | LocalDateTime | Data de criacao |
| dataAtualizacao | LocalDateTime | Data da ultima alteracao |

**Metodos:**
- `onCreate()`: Executado antes de persistir (define datas)
- `onUpdate()`: Executado antes de atualizar (atualiza dataAtualizacao)

#### ComandoAutomacao

| Atributo | Tipo | Descricao |
|----------|------|-----------|
| id | Long | Chave primaria (auto-gerada) |
| comando | String | Texto do comando em linguagem natural |
| status | StatusComando | Estado atual do comando |
| resultado | String | Resultado do processamento |
| dataEnvio | LocalDateTime | Data de envio do comando |
| dataProcessamento | LocalDateTime | Data em que foi processado |

#### StatusComando (Enum)

| Valor | Descricao |
|-------|-----------|
| PENDENTE | Aguardando processamento pelo n8n |
| PROCESSANDO | Em processamento |
| CONCLUIDO | Processado com sucesso |
| ERRO | Erro no processamento |

---

### 2.2 Camada DTO

#### ItemDTO

| Atributo | Tipo | Validacao |
|----------|------|-----------|
| id | Long | - |
| nome | String | @NotBlank |
| descricao | String | - |
| quantidade | Integer | @NotNull, @Min(0) |
| preco | BigDecimal | - |
| categoria | String | - |
| unidade | String | - |
| quantidadeMinima | Integer | - |

#### ComandoDTO

| Atributo | Tipo | Validacao |
|----------|------|-----------|
| comando | String | @NotBlank |

---

### 2.3 Camada Repository

#### ItemRepository

| Metodo | Retorno | Descricao |
|--------|---------|-----------|
| findByNomeIgnoreCase(nome) | Optional<Item> | Busca por nome exato |
| findByNomeContainingIgnoreCase(nome) | List<Item> | Busca por nome parcial |
| findByCategoriaIgnoreCase(categoria) | List<Item> | Busca por categoria |
| findItensEstoqueBaixo() | List<Item> | Itens com qtd <= qtdMinima |
| findAllCategorias() | List<String> | Lista categorias distintas |
| existsByNomeIgnoreCase(nome) | boolean | Verifica se existe |

#### ComandoAutomacaoRepository

| Metodo | Retorno | Descricao |
|--------|---------|-----------|
| findByStatus(status) | List<ComandoAutomacao> | Filtra por status |
| findAllByOrderByDataEnvioDesc() | List<ComandoAutomacao> | Ordenado por data |
| findTop10ByOrderByDataEnvioDesc() | List<ComandoAutomacao> | Ultimos 10 |

---

### 2.4 Camada Service

#### ItemService

| Metodo | Descricao |
|--------|-----------|
| listarTodos() | Retorna todos os itens |
| buscarPorId(id) | Busca item por ID |
| buscarPorNome(nome) | Busca por nome exato |
| buscarPorNomeContendo(nome) | Busca por nome parcial |
| buscarPorCategoria(categoria) | Filtra por categoria |
| listarItensEstoqueBaixo() | Itens com estoque baixo |
| listarCategorias() | Lista de categorias |
| salvar(itemDTO) | Cadastra novo item |
| atualizar(id, itemDTO) | Atualiza item existente |
| atualizarQuantidade(id, qtd) | Atualiza apenas quantidade |
| adicionarQuantidade(id, qtd) | Adiciona a quantidade atual |
| adicionarQuantidadePorNome(nome, qtd) | Adiciona por nome (cria se nao existir) |
| deletar(id) | Remove item |

#### ComandoAutomacaoService

| Metodo | Descricao |
|--------|-----------|
| listarTodos() | Retorna todos os comandos |
| listarUltimos() | Retorna ultimos 10 |
| buscarPorId(id) | Busca por ID |
| listarPendentes() | Lista comandos pendentes |
| enviarComando(comandoDTO) | Cria novo comando |
| atualizarStatus(id, status, resultado) | Atualiza status |
| marcarComoConcluido(id, resultado) | Marca como concluido |
| marcarComoErro(id, erro) | Marca como erro |

---

### 2.5 Camada Controller

#### ItemController

| Endpoint | Metodo | Descricao |
|----------|--------|-----------|
| /api/itens | GET | Lista todos |
| /api/itens/{id} | GET | Busca por ID |
| /api/itens/nome/{nome} | GET | Busca por nome |
| /api/itens/buscar?nome= | GET | Busca parcial |
| /api/itens/categoria/{cat} | GET | Por categoria |
| /api/itens/estoque-baixo | GET | Estoque baixo |
| /api/itens/categorias | GET | Lista categorias |
| /api/itens | POST | Cadastra |
| /api/itens/{id} | PUT | Atualiza |
| /api/itens/{id}/quantidade | PATCH | Atualiza qtd |
| /api/itens/{id}/adicionar | PATCH | Adiciona qtd |
| /api/itens/adicionar-por-nome | POST | Adiciona por nome |
| /api/itens/{id} | DELETE | Remove |

#### ComandoAutomacaoController

| Endpoint | Metodo | Descricao |
|----------|--------|-----------|
| /api/comandos | GET | Lista todos |
| /api/comandos/ultimos | GET | Ultimos 10 |
| /api/comandos/{id} | GET | Busca por ID |
| /api/comandos/pendentes | GET | Lista pendentes |
| /api/comandos | POST | Envia comando |
| /api/comandos/{id}/status | PATCH | Atualiza status |
| /api/comandos/{id}/concluido | PATCH | Marca concluido |
| /api/comandos/{id}/erro | PATCH | Marca erro |

---

## 3. Classes do Frontend

### 3.1 Componentes

#### Navbar
- Navegacao principal do sistema
- Links para todas as paginas
- Destaque visual da pagina ativa

### 3.2 Paginas

#### Home
- Dashboard com estatisticas
- Cards: total itens, categorias, estoque baixo
- Atalhos rapidos

#### Itens
- Tabela com todos os itens
- Busca por nome
- Botoes de editar/excluir
- Modal de confirmacao

#### CadastroItem
- Formulario de cadastro/edicao
- Validacao de campos
- Modo edicao (com ID) ou cadastro (sem ID)

#### Automacao
- Campo de texto para comando
- Botao enviar
- Historico de comandos
- Atualizacao automatica (5s)

### 3.3 Services

#### itemService
- Comunicacao com /api/itens
- Metodos CRUD
- Retorna Promises

#### comandoService
- Comunicacao com /api/comandos
- Envio e acompanhamento de comandos

---

## 4. Relacionamentos

```
┌─────────────┐         ┌──────────────────┐
│    Item     │         │ ComandoAutomacao │
├─────────────┤         ├──────────────────┤
│ id          │         │ id               │
│ nome        │         │ comando          │
│ quantidade  │         │ status ──────────┼──┐
│ categoria   │         │ resultado        │  │
│ ...         │         │ ...              │  │
└─────────────┘         └──────────────────┘  │
                                              │
                        ┌─────────────────────┘
                        │
                        v
                ┌───────────────┐
                │ StatusComando │
                ├───────────────┤
                │ PENDENTE      │
                │ PROCESSANDO   │
                │ CONCLUIDO     │
                │ ERRO          │
                └───────────────┘
```

---

## 5. Fluxo de Dados

### Cadastro Manual de Item

```
Usuario -> CadastroItem -> itemService.salvar()
    -> HTTP POST /api/itens
    -> ItemController.salvar()
    -> ItemService.salvar()
    -> ItemRepository.save()
    -> Database
```

### Cadastro via Automacao

```
Usuario -> Automacao -> comandoService.enviarComando()
    -> HTTP POST /api/comandos
    -> ComandoAutomacaoController.enviarComando()
    -> ComandoAutomacaoService.enviarComando()
    -> Database (status: PENDENTE)

n8n -> HTTP GET /api/comandos/pendentes
    -> Processa com IA
    -> HTTP POST /api/itens/adicionar-por-nome
    -> HTTP PATCH /api/comandos/{id}/concluido
    -> Database (status: CONCLUIDO)
```

---

## 6. Arquivos dos Diagramas

| Arquivo | Conteudo |
|---------|----------|
| diagrama-classes.puml | Diagrama completo com todas as camadas |
| diagrama-classes-entidades.puml | Apenas entidades do dominio |
| diagrama-classes-simplificado.puml | Visao geral da arquitetura |
| diagrama-classes-frontend.puml | Classes do frontend React |

Para visualizar os diagramas PlantUML:
- Online: https://www.plantuml.com/plantuml/uml
- VS Code: Extensao "PlantUML"
- IntelliJ: Plugin "PlantUML Integration"
