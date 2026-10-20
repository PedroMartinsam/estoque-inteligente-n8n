import { useCallback, useEffect, useRef, useState } from 'react'
import {
  FaCheckCircle,
  FaClipboardList,
  FaSpinner,
  FaSyncAlt,
  FaTimesCircle,
  FaTrashAlt
} from 'react-icons/fa'
import { toast } from 'react-toastify'
import { comandoService } from '../services/api'

const INTERVALO_MS = 3000

const STATUS = {
  CONCLUIDO: { rotulo: 'Sucesso', classe: 'sucesso', Icone: FaCheckCircle },
  PROCESSANDO: { rotulo: 'Processando', classe: 'processando', Icone: FaSpinner },
  ERRO: { rotulo: 'Erro', classe: 'erro', Icone: FaTimesCircle },
  PENDENTE: { rotulo: 'Pendente', classe: 'pendente', Icone: FaClipboardList }
}

const TILES = ['CONCLUIDO', 'PROCESSANDO', 'ERRO']

function formatarQuando(data) {
  if (!data) return ''
  const d = new Date(data)
  const hora = d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
  if (d.toDateString() === new Date().toDateString()) return hora
  return `${d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })} ${hora}`
}

/**
 * Painel de comandos da tela de Automacao.
 * Se atualiza sozinho a cada 3 s (pausa quando a aba esta escondida).
 * A tela avisa quando algo mudou incrementando a prop "sinal".
 */
function PainelComandos({ sinal }) {
  const [periodo, setPeriodo] = useState('hoje')
  const [filtro, setFiltro] = useState(null)
  const [comandos, setComandos] = useState([])
  const [resumo, setResumo] = useState({})
  const [carregando, setCarregando] = useState(true)
  const [online, setOnline] = useState(true)
  const [atualizadoEm, setAtualizadoEm] = useState(null)
  const [aberto, setAberto] = useState(null)
  const [confirmarZerar, setConfirmarZerar] = useState(false)
  const [zerando, setZerando] = useState(false)

  // descarta respostas atrasadas (uma consulta lenta nao sobrescreve uma mais nova)
  const seqRef = useRef(0)

  const carregar = useCallback(async () => {
    const seq = ++seqRef.current
    try {
      const [lista, contagem] = await Promise.all([
        comandoService.listarRecentes(50, periodo),
        comandoService.resumo(periodo)
      ])
      if (seq !== seqRef.current) return
      setComandos(lista.data)
      setResumo(contagem.data)
      setOnline(true)
      setAtualizadoEm(new Date())
    } catch (error) {
      if (seq !== seqRef.current) return
      console.error('Erro ao carregar comandos:', error)
      setOnline(false)
    } finally {
      if (seq === seqRef.current) setCarregando(false)
    }
  }, [periodo])

  // carga inicial, troca de periodo e atualizacao automatica
  useEffect(() => {
    setCarregando(true)
    carregar()
    const intervalo = setInterval(() => {
      if (!document.hidden) carregar()
    }, INTERVALO_MS)
    const aoVoltar = () => !document.hidden && carregar()
    document.addEventListener('visibilitychange', aoVoltar)
    return () => {
      clearInterval(intervalo)
      document.removeEventListener('visibilitychange', aoVoltar)
    }
  }, [carregar])

  // a tela pediu atualizacao (mensagem enviada / resposta chegou)
  useEffect(() => {
    if (sinal > 0) carregar()
  }, [sinal, carregar])

  const zerar = async () => {
    try {
      setZerando(true)
      const { data } = await comandoService.zerar()
      toast.success(`Histórico zerado (${data?.apagados ?? 0} comandos apagados)`)
      setConfirmarZerar(false)
      setFiltro(null)
      setAberto(null)
      await carregar()
    } catch (error) {
      const status = error.response?.status
      const motivo = error.response?.data?.erro || error.response?.data?.message
      if (status === 404 || status === 405) {
        toast.error('O backend ainda está na versão antiga (sem a rota de zerar). Rode Build > Rebuild Project e reinicie o backend.')
      } else if (!error.response) {
        toast.error('Não foi possível falar com o backend. Ele está rodando?')
      } else {
        toast.error(`Não foi possível zerar os comandos (erro ${status}${motivo ? `: ${motivo}` : ''})`)
      }
      console.error(error)
    } finally {
      setZerando(false)
    }
  }

  const total = TILES.reduce((s, k) => s + (resumo[k] || 0), 0)
  const visiveis = filtro ? comandos.filter((c) => c.status === filtro) : comandos

  return (
    <section className="painel painel-comandos pc" aria-label="Comandos enviados">
      {/* ------------------------------------------------ topo */}
      <header className="pc-topo">
        <div className="pc-titulo">
          <h3>
            <FaClipboardList /> Comandos
          </h3>
          <span className={`pc-vivo ${online ? '' : 'off'}`} title={atualizadoEm ? `Atualizado às ${atualizadoEm.toLocaleTimeString('pt-BR')}` : ''}>
            <i /> {online ? 'Ao vivo' : 'Sem conexão'}
          </span>
        </div>
        <div className="pc-acoes">
          <button type="button" className="painel-icone" onClick={carregar} title="Atualizar agora" aria-label="Atualizar comandos">
            <FaSyncAlt />
          </button>
          <button
            type="button"
            className="painel-icone perigo"
            onClick={() => setConfirmarZerar(true)}
            title="Zerar comandos"
            aria-label="Zerar comandos"
            disabled={total === 0 && comandos.length === 0}
          >
            <FaTrashAlt />
          </button>
        </div>
      </header>

      {/* ------------------------------------------------ periodo */}
      <div className="pc-periodo" role="tablist" aria-label="Período">
        {[
          ['hoje', 'Hoje'],
          ['tudo', 'Tudo']
        ].map(([valor, rotulo]) => (
          <button
            key={valor}
            type="button"
            role="tab"
            aria-selected={periodo === valor}
            className={periodo === valor ? 'ativo' : ''}
            onClick={() => {
              setPeriodo(valor)
              setFiltro(null)
            }}
          >
            {rotulo}
          </button>
        ))}
      </div>

      {/* ------------------------------------------------ contadores */}
      <div className="status-tiles">
        {TILES.map((chave) => {
          const { rotulo, classe, Icone } = STATUS[chave]
          const n = resumo[chave] || 0
          return (
            <button
              key={chave}
              type="button"
              className={`status-tile ${classe} ${filtro === chave ? 'ativo' : ''}`}
              onClick={() => setFiltro(filtro === chave ? null : chave)}
              aria-pressed={filtro === chave}
              title={filtro === chave ? 'Mostrar todos' : `Mostrar só ${rotulo.toLowerCase()}`}
            >
              <span className="status-tile-num">
                {n}
                <Icone className={`status-tile-icone ${chave === 'PROCESSANDO' && n > 0 ? 'girando' : ''}`} />
              </span>
              <span className="status-tile-rotulo">{rotulo}</span>
            </button>
          )
        })}
      </div>

      <div className="pc-barra" aria-hidden="true">
        {total > 0 &&
          TILES.map((k) =>
            resumo[k] ? <i key={k} className={STATUS[k].classe} style={{ flexGrow: resumo[k] }} /> : null
          )}
      </div>

      {filtro && (
        <div className="pc-filtro">
          Mostrando só <strong>{STATUS[filtro].rotulo.toLowerCase()}</strong>
          <button type="button" onClick={() => setFiltro(null)}>
            ver todos
          </button>
        </div>
      )}

      {/* ------------------------------------------------ lista */}
      <ul className="pc-lista">
        {visiveis.length === 0 && (
          <li className="painel-vazio">
            {carregando
              ? 'Carregando...'
              : filtro
                ? `Nenhum comando com status ${STATUS[filtro].rotulo.toLowerCase()}.`
                : periodo === 'hoje'
                  ? 'Nenhum comando hoje. Mande uma mensagem no chat.'
                  : 'Nenhum comando ainda.'}
          </li>
        )}
        {visiveis.map((c) => {
          const s = STATUS[c.status] || STATUS.PENDENTE
          const expandido = aberto === c.id
          return (
            <li key={c.id}>
              <button
                type="button"
                className={`pc-item ${s.classe} ${expandido ? 'aberto' : ''}`}
                onClick={() => setAberto(expandido ? null : c.id)}
                aria-expanded={expandido}
              >
                <span className={`pc-icone ${s.classe}`}>
                  <s.Icone className={c.status === 'PROCESSANDO' ? 'girando' : ''} />
                </span>
                <span className="pc-corpo">
                  <span className="pc-linha">
                    <span className="pc-comando">{c.comando}</span>
                    <span className="pc-hora">{formatarQuando(c.dataEnvio)}</span>
                  </span>
                  {c.resultado ? (
                    <span className="pc-resultado">{c.resultado}</span>
                  ) : (
                    <span className="pc-resultado vazio">
                      {c.status === 'PROCESSANDO' ? 'O assistente está trabalhando...' : 'Sem resposta registrada'}
                    </span>
                  )}
                </span>
              </button>
            </li>
          )
        })}
      </ul>

      {/* ------------------------------------------------ confirmar zerar */}
      {confirmarZerar && (
        <div className="ep-modal-overlay" onClick={() => !zerando && setConfirmarZerar(false)}>
          <div className="ep-modal" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
            <span className="ep-modal-icon">
              <FaTrashAlt />
            </span>
            <h3>Zerar os comandos?</h3>
            <p>
              Todo o histórico de comandos é apagado e os contadores voltam a zero. O estoque, os produtos e as conversas
              do chat não são alterados.
            </p>
            <div className="ep-modal-footer">
              <button type="button" className="ep-btn" onClick={() => setConfirmarZerar(false)} disabled={zerando}>
                Cancelar
              </button>
              <button type="button" className="ep-btn ep-btn-danger" onClick={zerar} disabled={zerando}>
                {zerando ? 'Zerando...' : 'Zerar comandos'}
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  )
}

export default PainelComandos