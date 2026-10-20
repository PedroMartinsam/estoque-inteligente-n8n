import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  FaArrowRight,
  FaBoxes,
  FaCheckCircle,
  FaCoins,
  FaCubes,
  FaExclamationTriangle,
  FaList,
  FaPlus,
  FaRobot,
  FaSpinner,
  FaTimesCircle
} from 'react-icons/fa'
import { comandoService, itemService } from '../services/api'
import { capitalizar, moeda, numero, percentualEstoque, situacaoEstoque } from '../utils/estoque'
import '../styles/premium.css'

const STATUS_CMD = {
  CONCLUIDO: { rotulo: 'Sucesso', classe: 'ok', Icone: FaCheckCircle },
  PROCESSANDO: { rotulo: 'Processando', classe: 'baixo', Icone: FaSpinner },
  ERRO: { rotulo: 'Erro', classe: 'zerado', Icone: FaTimesCircle },
  PENDENTE: { rotulo: 'Pendente', classe: 'neutro', Icone: FaSpinner }
}

function saudacao() {
  const h = new Date().getHours()
  if (h < 12) return 'Bom dia'
  if (h < 18) return 'Boa tarde'
  return 'Boa noite'
}

function Home() {
  const [itens, setItens] = useState([])
  const [comandos, setComandos] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const carregar = async () => {
      try {
        const res = await itemService.listarTodos()
        setItens(res.data)
      } catch (error) {
        console.error('Erro ao carregar itens:', error)
      }
      try {
        const res = await comandoService.listarRecentes(6)
        setComandos(res.data)
      } catch (error) {
        console.error('Erro ao carregar comandos:', error)
      }
      setLoading(false)
    }
    carregar()
  }, [])

  const dados = useMemo(() => {
    const unidades = itens.reduce((s, i) => s + (i.quantidade || 0), 0)
    const comPreco = itens.filter((i) => i.preco != null)
    const valor = comPreco.reduce((s, i) => s + (i.quantidade || 0) * Number(i.preco), 0)

    const reposicao = itens
      .filter((i) => situacaoEstoque(i) !== 'ok')
      .sort((a, b) => (a.quantidade || 0) - (b.quantidade || 0))

    // categorias sem diferenciar maiusculas ("fruta" e "Frutas" continuam separadas se forem palavras diferentes)
    const porCategoria = new Map()
    for (const i of itens) {
      const chave = (i.categoria || 'Sem categoria').trim().toLowerCase()
      const atual = porCategoria.get(chave) || { nome: capitalizar(chave), itens: 0, unidades: 0 }
      atual.itens += 1
      atual.unidades += i.quantidade || 0
      porCategoria.set(chave, atual)
    }
    const categorias = [...porCategoria.values()].sort((a, b) => b.unidades - a.unidades)
    const maxCat = Math.max(1, ...categorias.map((c) => c.unidades))

    return {
      unidades,
      valor,
      semPreco: itens.length - comPreco.length,
      reposicao,
      categorias,
      maxCat
    }
  }, [itens])

  if (loading) {
    return (
      <div className="ep-loading">
        <div className="ep-spinner" />
      </div>
    )
  }

  const dataHoje = new Date().toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })

  return (
    <div className="ep-page">
      <header className="ep-page-header">
        <div>
          <span className="ep-eyebrow">{dataHoje}</span>
          <h1 className="ep-title">{saudacao()}! Este é o resumo do seu estoque.</h1>
          <p className="ep-subtitle">Acompanhe saldos, reposições e o que o assistente fez hoje.</p>
        </div>
        <div className="ep-actions">
          <Link to="/automacao" className="ep-btn">
            <FaRobot /> Assistente IA
          </Link>
          <Link to="/cadastro" className="ep-btn ep-btn-primary">
            <FaPlus /> Novo item
          </Link>
        </div>
      </header>

      {/* ---------------------------------------------------------- KPIs */}
      <section className="ep-kpis">
        <div className="ep-card ep-kpi">
          <div className="ep-kpi-top">
            <span className="ep-kpi-label">Produtos ativos</span>
            <span className="ep-kpi-icon"><FaBoxes /></span>
          </div>
          <div className="ep-kpi-value">{numero.format(itens.length)}</div>
          <div className="ep-kpi-foot">{dados.categorias.length} categorias</div>
        </div>

        <div className="ep-card ep-kpi">
          <div className="ep-kpi-top">
            <span className="ep-kpi-label">Unidades em estoque</span>
            <span className="ep-kpi-icon info"><FaCubes /></span>
          </div>
          <div className="ep-kpi-value">{numero.format(dados.unidades)}</div>
          <div className="ep-kpi-foot">Somando todos os produtos</div>
        </div>

        <div className="ep-card ep-kpi">
          <div className="ep-kpi-top">
            <span className="ep-kpi-label">Valor em estoque</span>
            <span className="ep-kpi-icon gold"><FaCoins /></span>
          </div>
          <div className="ep-kpi-value">{moeda.format(dados.valor)}</div>
          <div className="ep-kpi-foot">
            {dados.semPreco > 0 ? `${dados.semPreco} produto(s) sem preço fora da conta` : 'Quantidade × preço unitário'}
          </div>
        </div>

        <div className={`ep-card ep-kpi ${dados.reposicao.length > 0 ? 'alerta' : ''}`}>
          <div className="ep-kpi-top">
            <span className="ep-kpi-label">Precisam de reposição</span>
            <span className="ep-kpi-icon warn"><FaExclamationTriangle /></span>
          </div>
          <div className="ep-kpi-value">{dados.reposicao.length}</div>
          <div className="ep-kpi-foot">
            {dados.reposicao.filter((i) => situacaoEstoque(i) === 'zerado').length} zerado(s)
          </div>
        </div>
      </section>

      {/* ---------------------------------------------------------- paineis */}
      <section className="ep-grid-2">
        <div className="ep-card">
          <div className="ep-card-header">
            <h2 className="ep-card-title"><FaExclamationTriangle /> Reposição necessária</h2>
            <Link to="/itens" className="ep-btn ep-btn-ghost">Ver estoque <FaArrowRight /></Link>
          </div>
          <div className="ep-card-body">
            {dados.reposicao.length === 0 ? (
              <div className="ep-empty">
                <span className="ep-empty-icon"><FaCheckCircle /></span>
                Tudo em ordem: nenhum produto abaixo do mínimo.
              </div>
            ) : (
              <ul className="ep-list">
                {dados.reposicao.slice(0, 7).map((i) => {
                  const sit = situacaoEstoque(i)
                  const pct = percentualEstoque(i)
                  return (
                    <li key={i.id} className="ep-list-item">
                      <span className="ep-avatar">{i.nome?.charAt(0)}</span>
                      <div className="ep-list-main">
                        <div className="ep-list-row">
                          <span className="ep-list-title">{i.nome}</span>
                          <span className="ep-list-meta">
                            <strong style={{ color: 'var(--ep-ink)' }}>{i.quantidade}</strong>
                            {i.quantidadeMinima != null ? ` / mín. ${i.quantidadeMinima}` : ''} {i.unidade || ''}
                          </span>
                        </div>
                        <div className={`ep-bar ${sit}`}><i style={{ width: `${Math.max(pct, 3)}%` }} /></div>
                      </div>
                      <span className={`ep-badge ${sit}`}>{sit === 'zerado' ? 'Zerado' : 'Baixo'}</span>
                    </li>
                  )
                })}
              </ul>
            )}
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 24, minWidth: 0 }}>
          <div className="ep-card">
            <div className="ep-card-header">
              <h2 className="ep-card-title"><FaCubes /> Estoque por categoria</h2>
              <span className="ep-card-sub">em unidades</span>
            </div>
            <div className="ep-card-body">
              {dados.categorias.length === 0 ? (
                <div className="ep-empty">Nenhum produto cadastrado.</div>
              ) : (
                <ul className="ep-list">
                  {dados.categorias.slice(0, 6).map((c) => (
                    <li key={c.nome} className="ep-list-item" style={{ padding: '10px 0' }}>
                      <div className="ep-list-main">
                        <div className="ep-list-row">
                          <span className="ep-list-title">{c.nome}</span>
                          <span className="ep-list-meta">
                            {numero.format(c.unidades)} un · {c.itens} {c.itens === 1 ? 'produto' : 'produtos'}
                          </span>
                        </div>
                        <div className="ep-bar"><i style={{ width: `${Math.max((c.unidades / dados.maxCat) * 100, 2)}%` }} /></div>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>

          <div className="ep-card">
            <div className="ep-card-header">
              <h2 className="ep-card-title"><FaRobot /> Atividade do assistente</h2>
              <Link to="/automacao" className="ep-btn ep-btn-ghost">Abrir <FaArrowRight /></Link>
            </div>
            <div className="ep-card-body">
              {comandos.length === 0 ? (
                <div className="ep-empty">Nenhum comando enviado ainda.</div>
              ) : (
                <ul className="ep-list">
                  {comandos.slice(0, 5).map((c) => {
                    const s = STATUS_CMD[c.status] || STATUS_CMD.PENDENTE
                    return (
                      <li key={c.id} className="ep-list-item" style={{ padding: '10px 0' }}>
                        <div className="ep-list-main" style={{ gap: 2 }}>
                          <span className="ep-list-title" style={{ textTransform: 'none' }} title={c.comando}>
                            {c.comando}
                          </span>
                          <span className="ep-list-meta">
                            {c.dataEnvio
                              ? new Date(c.dataEnvio).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
                              : ''}
                          </span>
                        </div>
                        <span className={`ep-badge ${s.classe}`}>{s.rotulo}</span>
                      </li>
                    )
                  })}
                </ul>
              )}
            </div>
          </div>
        </div>
      </section>

      {/* ---------------------------------------------------------- atalhos */}
      <section className="ep-kpis">
        <Link to="/cadastro" className="ep-card ep-shortcut">
          <span className="ep-shortcut-icon"><FaPlus /></span>
          <div>
            <h3>Cadastrar item</h3>
            <p>Adicione um produto novo ao estoque</p>
          </div>
          <FaArrowRight className="ep-shortcut-arrow" />
        </Link>
        <Link to="/itens" className="ep-card ep-shortcut">
          <span className="ep-shortcut-icon dark"><FaList /></span>
          <div>
            <h3>Consultar estoque</h3>
            <p>Busque, filtre e edite os produtos</p>
          </div>
          <FaArrowRight className="ep-shortcut-arrow" />
        </Link>
        <Link to="/automacao" className="ep-card ep-shortcut">
          <span className="ep-shortcut-icon gold"><FaRobot /></span>
          <div>
            <h3>Assistente IA</h3>
            <p>Dê entrada e saída conversando</p>
          </div>
          <FaArrowRight className="ep-shortcut-arrow" />
        </Link>
      </section>
    </div>
  )
}

export default Home
