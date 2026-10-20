import axios from 'axios'

const API_URL = 'http://localhost:8080/api'

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Item Service
export const itemService = {
  listarTodos: () => api.get('/itens'),
  listarInativos: () => api.get('/itens/inativos'),
  buscarPorId: (id) => api.get(`/itens/${id}`),
  buscarPorNome: (nome) => api.get('/itens/buscar', { params: { nome } }),
  buscarPorCategoria: (categoria) => api.get(`/itens/categoria/${encodeURIComponent(categoria)}`),
  listarEstoqueBaixo: () => api.get('/itens/estoque-baixo'),
  listarCategorias: () => api.get('/itens/categorias'),
  salvar: (item) => api.post('/itens', item),
  atualizar: (id, item) => api.put(`/itens/${id}`, item),
  adicionarQuantidade: (id, quantidade) => api.patch(`/itens/${id}/adicionar`, null, { params: { quantidade } }),
  inativar: (id) => api.patch(`/itens/${id}/inativar`),
  reativar: (id) => api.patch(`/itens/${id}/reativar`),
  // DELETE agora faz exclusao logica (inativa) no backend
  deletar: (id) => api.delete(`/itens/${id}`)
}

// Embalagens (P/M/G -> unidades)
export const embalagemService = {
  listarTodas: () => api.get('/embalagens'),
  listarPorProduto: (nome) => api.get('/embalagens/produto', { params: { nome } }),
  definir: (nome, codigo, unidades) =>
    api.post('/embalagens/definir-por-nome', null, { params: { nome, codigo, unidades } }),
  remover: (id) => api.delete(`/embalagens/${id}`)
}

// Chat com o agente (backend -> n8n -> OpenAI)
export const chatService = {
  // o agente pode demorar (varias tools + OpenAI), por isso timeout maior
  enviar: (sessaoId, mensagem) => api.post('/chat', { sessaoId, mensagem }, { timeout: 130000 }),
  mensagens: (sessaoId) => api.get(`/chat/${sessaoId}/mensagens`),
  limpar: (sessaoId) => api.delete(`/chat/${sessaoId}`)
}

// Comandos: cada mensagem do chat vira um comando com status (painel da tela de Automacao)
export const comandoService = {
  listarTodos: () => api.get('/comandos'),
  listarUltimos: () => api.get('/comandos/ultimos'),
  // periodo: 'hoje' | 'tudo'
  listarRecentes: (limite = 30, periodo = 'tudo') => api.get('/comandos/recentes', { params: { limite, periodo } }),
  resumo: (periodo = 'tudo') => api.get('/comandos/resumo', { params: { periodo } }),
  // POST em vez de DELETE: o CorsConfig do backend nao libera DELETE
  zerar: () => api.post('/comandos/zerar'),
  buscarPorId: (id) => api.get(`/comandos/${id}`),
  listarPendentes: () => api.get('/comandos/pendentes'),
  enviarComando: (comando) => api.post('/comandos', { comando }),
  marcarComoConcluido: (id, resultado) => api.patch(`/comandos/${id}/concluido`, { resultado }),
  marcarComoErro: (id, erro) => api.patch(`/comandos/${id}/erro`, { erro })
}

export default api