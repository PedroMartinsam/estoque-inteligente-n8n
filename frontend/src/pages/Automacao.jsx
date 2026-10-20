import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  FaBoxOpen,
  FaCheckCircle,
  FaExclamationTriangle,
  FaPaperPlane,
  FaRedo,
  FaRobot,
  FaSyncAlt,
  FaUser
} from 'react-icons/fa'
import { toast } from 'react-toastify'
import { chatService, embalagemService } from '../services/api'
import PainelComandos from './PainelComandos'
import '../styles/premium.css'
import './Automacao.css'

const CHAVE_SESSAO = 'estoque-chat-sessao'

const MENSAGEM_BOAS_VINDAS = {
  id: 'boas-vindas',
  papel: 'ASSISTENTE',
  conteudo:
    'Oi! Sou o assistente do estoque. Posso cadastrar, consultar, alterar e inativar produtos, ' +
    'dar entrada e saida de estoque e cuidar das embalagens. O que voce precisa?'
}

const EXEMPLOS = [
  'Quanto tem de manga?',
  'Chegou uma caixa grande de manga',
  'Vendi 12 macas',
  'Quais produtos estao com estoque baixo?',
  'Cadastre abacaxi, 20 unidades, R$ 6,00 cada',
  'A caixa G de laranja tem 60 unidades',
  'Muda o preco da batata para 5,50',
  'Pode excluir o chuchu'
]

function gerarId() {
  if (window.crypto?.randomUUID) return window.crypto.randomUUID()
  return `s-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

function obterSessao() {
  let id = null
  try {
    id = localStorage.getItem(CHAVE_SESSAO)
    if (!id) {
      id = gerarId()
      localStorage.setItem(CHAVE_SESSAO, id)
    }
  } catch {
    id = id || gerarId()
  }
  return id
}

const formatarHora = (data) =>
  data ? new Date(data).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }) : ''

function mensagemDeErro(error) {
  const status = error.response?.status
  const msgBackend = error.response?.data?.erro || error.response?.data?.message
  if (error.code === 'ECONNABORTED') return 'O assistente demorou demais para responder.'
  if (!error.response) return 'Nao foi possivel falar com o servidor (sem resposta). O backend esta rodando na porta 8080?'
  if (status === 404) return 'Erro 404: a rota /api/chat nao existe no backend. Confira se o ChatController foi copiado e rode Build > Rebuild Project.'
  return `Erro ${status} no servidor${msgBackend ? `: ${msgBackend}` : ''}. Veja o log do backend.`
}

// ======================================================================= painel: embalagens

function PainelEmbalagens({ embalagens, carregando, onAtualizar }) {
  const porProduto = useMemo(() => {
    const grupos = new Map()
    for (const e of embalagens) {
      if (!grupos.has(e.produto)) grupos.set(e.produto, [])
      grupos.get(e.produto).push(e)
    }
    return [...grupos.entries()].sort((a, b) => a[0].localeCompare(b[0], 'pt-BR'))
  }, [embalagens])

  return (
    <section className="painel painel-embalagens" aria-label="Caixas cadastradas">
      <header className="painel-topo">
        <h3>
          <FaBoxOpen /> Caixas cadastradas
        </h3>
        <button type="button" className="painel-icone" onClick={onAtualizar} title="Atualizar" aria-label="Atualizar caixas">
          <FaSyncAlt className={carregando ? 'girando' : ''} />
        </button>
      </header>

      <ul className="lista-embalagens">
        {porProduto.length === 0 && (
          <li className="painel-vazio">
            {carregando ? 'Carregando...' : 'Nenhuma caixa cadastrada. Ex: "a caixa M de banana tem 40 unidades".'}
          </li>
        )}
        {porProduto.map(([produto, caixas]) => (
          <li key={produto} className="grupo-embalagem">
            <span className="grupo-produto">{produto}</span>
            <div className="grupo-caixas">
              {caixas.map((e) => (
                <span key={e.id} className="caixa" title={e.nome || e.codigo}>
                  <span className="caixa-codigo">{e.codigo}</span>
                  <span className="caixa-unid">{e.unidades} un</span>
                </span>
              ))}
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}

// ======================================================================= tela

function Automacao() {
  const [sessaoId, setSessaoId] = useState(obterSessao)
  const [mensagens, setMensagens] = useState([])
  const [texto, setTexto] = useState('')
  const [enviando, setEnviando] = useState(false)

  // incrementado para o painel de comandos se atualizar na hora
  const [sinalComandos, setSinalComandos] = useState(0)
  const avisarPainel = useCallback(() => setSinalComandos((n) => n + 1), [])

  const [embalagens, setEmbalagens] = useState([])
  const [carregandoEmbalagens, setCarregandoEmbalagens] = useState(true)

  const fimRef = useRef(null)
  const inputRef = useRef(null)

  const carregarEmbalagens = useCallback(async () => {
    setCarregandoEmbalagens(true)
    try {
      const { data } = await embalagemService.listarTodas()
      setEmbalagens(data)
    } catch (error) {
      console.error('Erro ao carregar embalagens:', error)
    } finally {
      setCarregandoEmbalagens(false)
    }
  }, [])

  useEffect(() => {
    carregarEmbalagens()
  }, [carregarEmbalagens])

  // conversa salva no backend (sobrevive a F5)
  useEffect(() => {
    let ativo = true
    chatService
      .mensagens(sessaoId)
      .then((res) => ativo && setMensagens(res.data))
      .catch(() => ativo && setMensagens([]))
    return () => {
      ativo = false
    }
  }, [sessaoId])

  useEffect(() => {
    fimRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [mensagens, enviando])

  const enviar = async (conteudo) => {
    const msg = (conteudo ?? texto).trim()
    if (!msg || enviando) return

    setMensagens((atual) => [...atual, { id: `tmp-${Date.now()}`, papel: 'USUARIO', conteudo: msg }])
    setTexto('')
    setEnviando(true)
    // o backend ja criou o comando em PROCESSANDO; puxa o painel logo
    setTimeout(avisarPainel, 400)

    try {
      const { data } = await chatService.enviar(sessaoId, msg)
      setMensagens((atual) => [
        ...atual,
        {
          id: `r-${Date.now()}`,
          papel: 'ASSISTENTE',
          conteudo: data.resposta,
          concluido: data.concluido,
          erro: data.erro,
          dataHora: new Date().toISOString()
        }
      ])
      if (data.concluido) {
        toast.success('Operacao realizada no estoque!')
        carregarEmbalagens()
      }
    } catch (error) {
      console.error(error)
      setMensagens((atual) => [
        ...atual,
        { id: `e-${Date.now()}`, papel: 'ASSISTENTE', conteudo: mensagemDeErro(error), erro: true }
      ])
    } finally {
      setEnviando(false)
      avisarPainel()
      inputRef.current?.focus()
    }
  }

  const aoSubmeter = (e) => {
    e.preventDefault()
    enviar()
  }

  // Enter envia, Shift+Enter quebra linha
  const aoTeclar = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      enviar()
    }
  }

  const novaConversa = () => {
    if (enviando) return
    const novo = gerarId()
    try {
      localStorage.setItem(CHAVE_SESSAO, novo)
    } catch {
      /* sem localStorage: a sessao vale ate recarregar a pagina */
    }
    setSessaoId(novo)
    setMensagens([])
  }

  const lista = [MENSAGEM_BOAS_VINDAS, ...mensagens]

  return (
    <div className="automacao-layout">
      {/* ------------------------------------------------------------ chat */}
      <div className="chat-container">
        <div className="chat-header">
          <div className="chat-header-id">
            <span className="chat-header-mark">
              <FaRobot />
            </span>
            <div>
              <h2>Assistente de Estoque</h2>
              <p>Converse em linguagem natural. Ele pergunta o que faltar antes de mexer no estoque.</p>
            </div>
          </div>
          <button type="button" className="ep-btn chat-btn-nova" onClick={novaConversa} disabled={enviando}>
            <FaRedo /> Nova conversa
          </button>
        </div>

        <div className="chat-mensagens">
          {lista.map((m) => {
            const doUsuario = m.papel === 'USUARIO'
            return (
              <div key={m.id} className={`chat-linha ${doUsuario ? 'usuario' : 'assistente'}`}>
                <div className="chat-avatar">{doUsuario ? <FaUser /> : <FaRobot />}</div>
                <div className={`chat-balao ${m.erro ? 'erro' : ''} ${m.concluido ? 'concluido' : ''}`}>
                  <div className="chat-texto">{m.conteudo}</div>
                  <div className="chat-meta">
                    {m.concluido && (
                      <span className="chat-tag ok">
                        <FaCheckCircle /> executado
                      </span>
                    )}
                    {m.erro && (
                      <span className="chat-tag falha">
                        <FaExclamationTriangle /> falha
                      </span>
                    )}
                    {formatarHora(m.dataHora)}
                  </div>
                </div>
              </div>
            )
          })}

          {enviando && (
            <div className="chat-linha assistente">
              <div className="chat-avatar">
                <FaRobot />
              </div>
              <div className="chat-balao digitando">
                <span />
                <span />
                <span />
              </div>
            </div>
          )}
          <div ref={fimRef} />
        </div>

        {mensagens.length === 0 && (
          <div className="chat-exemplos">
            {EXEMPLOS.map((ex) => (
              <button key={ex} type="button" onClick={() => enviar(ex)} disabled={enviando}>
                {ex}
              </button>
            ))}
          </div>
        )}

        <form className="chat-form" onSubmit={aoSubmeter}>
          <textarea
            id="chat-input"
            ref={inputRef}
            className="chat-input"
            placeholder="Digite sua mensagem... (Enter envia, Shift+Enter quebra linha)"
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            onKeyDown={aoTeclar}
            maxLength={2000}
            rows={2}
            disabled={enviando}
          />
          <button type="submit" className="ep-btn ep-btn-primary chat-btn-enviar" disabled={enviando || !texto.trim()}>
            <FaPaperPlane />
            {enviando ? 'Aguarde...' : 'Enviar'}
          </button>
        </form>
      </div>

      {/* ------------------------------------------------------------ painel lateral fixo */}
      <aside className="automacao-lateral">
        <PainelComandos sinal={sinalComandos} />
        <PainelEmbalagens
          embalagens={embalagens}
          carregando={carregandoEmbalagens}
          onAtualizar={carregarEmbalagens}
        />
      </aside>
    </div>
  )
}

export default Automacao