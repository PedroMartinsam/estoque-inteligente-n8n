import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { FaBoxes, FaEdit, FaEyeSlash, FaPlus, FaSearch, FaTimes } from 'react-icons/fa'
import { toast } from 'react-toastify'
import { itemService } from '../services/api'
import {
  capitalizar,
  moeda,
  numero,
  percentualEstoque,
  ROTULO_SITUACAO,
  situacaoEstoque
} from '../utils/estoque'
import '../styles/premium.css'

const COLUNAS = [
  { chave: 'nome', rotulo: 'Produto' },
  { chave: 'categoria', rotulo: 'Categoria' },
  { chave: 'quantidade', rotulo: 'Estoque' },
  { chave: 'situacao', rotulo: 'Situação' },
  { chave: 'preco', rotulo: 'Preço unit.', num: true },
  { chave: 'valor', rotulo: 'Valor em estoque', num: true },
  { chave: 'dataCadastro', rotulo: 'Cadastro' }
]

const ORDEM_SITUACAO = { zerado: 0, baixo: 1, ok: 2 }

function valorOrdenacao(item, chave) {
  switch (chave) {
    case 'nome':
    case 'categoria':
      return (item[chave] || '').toLowerCase()
    case 'situacao':
      return ORDEM_SITUACAO[situacaoEstoque(item)]
    case 'valor':
      return item.preco != null ? (item.quantidade || 0) * Number(item.preco) : -1
    case 'preco':
      return item.preco != null ? Number(item.preco) : -1
    case 'dataCadastro':
      return item.dataCadastro ? new Date(item.dataCadastro).getTime() : 0
    default:
      return Number(item[chave]) || 0
  }
}

function Itens() {
  const [itens, setItens] = useState([])
  const [loading, setLoading] = useState(true)
  const [busca, setBusca] = useState('')
  const [situacao, setSituacao] = useState('todos')
  const [categoria, setCategoria] = useState('')
  const [ordem, setOrdem] = useState({ chave: 'nome', asc: true })
  const [itemParaInativar, setItemParaInativar] = useState(null)
  const [inativando, setInativando] = useState(false)

  useEffect(() => {
    carregarItens()
  }, [])

  const carregarItens = async () => {
    try {
      const response = await itemService.listarTodos()
      setItens(response.data)
    } catch (error) {
      toast.error('Erro ao carregar itens')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }

  const inativarItem = async () => {
    if (!itemParaInativar) return
    try {
      setInativando(true)
      await itemService.deletar(itemParaInativar.id)
      toast.success(`"${itemParaInativar.nome}" foi inativado`)
      setItemParaInativar(null)
      carregarItens()
    } catch (error) {
      toast.error(error.response?.data?.erro || 'Erro ao inativar item')
      console.error(error)
    } finally {
      setInativando(false)
    }
  }

  // categorias existentes (sem repetir por maiusculas/minusculas)
  const categorias = useMemo(() => {
    const mapa = new Map()
    for (const i of itens) {
      if (i.categoria) mapa.set(i.categoria.trim().toLowerCase(), capitalizar(i.categoria.trim()))
    }
    return [...mapa.entries()].sort((a, b) => a[1].localeCompare(b[1], 'pt-BR'))
  }, [itens])

  const contagem = useMemo(() => {
    const c = { todos: itens.length, ok: 0, baixo: 0, zerado: 0 }
    for (const i of itens) c[situacaoEstoque(i)] += 1
    return c
  }, [itens])

  const visiveis = useMemo(() => {
    const termo = busca.trim().toLowerCase()
    const lista = itens.filter((i) => {
      if (situacao !== 'todos' && situacaoEstoque(i) !== situacao) return false
      if (categoria && (i.categoria || '').trim().toLowerCase() !== categoria) return false
      if (termo) {
        const alvo = `${i.nome || ''} ${i.categoria || ''} ${i.id}`.toLowerCase()
        if (!alvo.includes(termo)) return false
      }
      return true
    })
    return lista.sort((a, b) => {
      const va = valorOrdenacao(a, ordem.chave)
      const vb = valorOrdenacao(b, ordem.chave)
      const cmp = typeof va === 'string' ? va.localeCompare(vb, 'pt-BR') : va - vb
      return ordem.asc ? cmp : -cmp
    })
  }, [itens, busca, situacao, categoria, ordem])

  const valorVisivel = visiveis.reduce(
    (s, i) => s + (i.preco != null ? (i.quantidade || 0) * Number(i.preco) : 0),
    0
  )

  const ordenarPor = (chave) =>
    setOrdem((atual) => ({ chave, asc: atual.chave === chave ? !atual.asc : true }))

  const temFiltro = busca || situacao !== 'todos' || categoria
  const limparFiltros = () => {
    setBusca('')
    setSituacao('todos')
    setCategoria('')
  }

  const formatarData = (data) => (data ? new Date(data).toLocaleDateString('pt-BR') : '-')

  if (loading) {
    return (
      <div className="ep-loading">
        <div className="ep-spinner" />
      </div>
    )
  }

  return (
    <div className="ep-page">
      <header className="ep-page-header">
        <div>
          <span className="ep-eyebrow">Estoque</span>
          <h1 className="ep-title">Produtos</h1>
          <p className="ep-subtitle">
            {numero.format(itens.length)} produtos ativos · clique no título de uma coluna para ordenar.
          </p>
        </div>
        <div className="ep-actions">
          <Link to="/cadastro" className="ep-btn ep-btn-primary">
            <FaPlus /> Novo item
          </Link>
        </div>
      </header>

      <div className="ep-card">
        <div className="ep-card-header" style={{ flexWrap: 'wrap' }}>
          <div className="ep-chips" role="group" aria-label="Filtrar por situação">
            {[
              ['todos', 'Todos'],
              ['ok', 'Em dia'],
              ['baixo', 'Baixo'],
              ['zerado', 'Zerado']
            ].map(([chave, rotulo]) => (
              <button
                key={chave}
                type="button"
                className={`ep-chip ${situacao === chave ? 'ativo' : ''}`}
                onClick={() => setSituacao(chave)}
                aria-pressed={situacao === chave}
              >
                {rotulo} <span>{contagem[chave]}</span>
              </button>
            ))}
          </div>
          {temFiltro && (
            <button type="button" className="ep-btn ep-btn-ghost" onClick={limparFiltros}>
              <FaTimes /> Limpar filtros
            </button>
          )}
        </div>

        <div className="ep-toolbar">
          <div className="ep-input-group">
            <FaSearch />
            <input
              id="busca-itens"
              type="search"
              className="ep-input"
              placeholder="Buscar por nome, categoria ou código..."
              value={busca}
              onChange={(e) => setBusca(e.target.value)}
            />
          </div>
          <select
            id="filtro-categoria"
            className="ep-input"
            style={{ width: 'auto', minWidth: 190 }}
            value={categoria}
            onChange={(e) => setCategoria(e.target.value)}
            aria-label="Filtrar por categoria"
          >
            <option value="">Todas as categorias</option>
            {categorias.map(([chave, nome]) => (
              <option key={chave} value={chave}>
                {nome}
              </option>
            ))}
          </select>
        </div>

        {visiveis.length === 0 ? (
          <div className="ep-empty" style={{ borderTop: '1px solid var(--ep-line)' }}>
            <span className="ep-empty-icon">
              <FaBoxes />
            </span>
            {itens.length === 0 ? 'Nenhum produto cadastrado ainda.' : 'Nenhum produto encontrado com esses filtros.'}
            {itens.length === 0 ? (
              <Link to="/cadastro" className="ep-btn ep-btn-primary">
                <FaPlus /> Cadastrar primeiro item
              </Link>
            ) : (
              <button type="button" className="ep-btn" onClick={limparFiltros}>
                Limpar filtros
              </button>
            )}
          </div>
        ) : (
          <div className="ep-table-wrap">
            <table className="ep-table">
              <thead>
                <tr>
                  {COLUNAS.map((col) => (
                    <th
                      key={col.chave}
                      className={`ordenavel ${col.num ? 'num' : ''}`}
                      onClick={() => ordenarPor(col.chave)}
                      tabIndex={0}
                      onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && ordenarPor(col.chave)}
                      aria-sort={ordem.chave === col.chave ? (ordem.asc ? 'ascending' : 'descending') : 'none'}
                    >
                      {col.rotulo}
                      {ordem.chave === col.chave && <span className="seta">{ordem.asc ? '▲' : '▼'}</span>}
                    </th>
                  ))}
                  <th aria-label="Ações" />
                </tr>
              </thead>
              <tbody>
                {visiveis.map((item) => {
                  const sit = situacaoEstoque(item)
                  const valor = item.preco != null ? (item.quantidade || 0) * Number(item.preco) : null
                  return (
                    <tr key={item.id}>
                      <td>
                        <div className="ep-produto">
                          <span className="ep-avatar">{item.nome?.charAt(0)}</span>
                          <div style={{ minWidth: 0 }}>
                            <div className="ep-produto-nome">{item.nome}</div>
                            <div className="ep-produto-sub">#{String(item.id).padStart(4, '0')}</div>
                          </div>
                        </div>
                      </td>
                      <td>{item.categoria ? <span className="ep-tag">{item.categoria}</span> : <span className="ep-hint">—</span>}</td>
                      <td>
                        <div className="ep-qtd">
                          <span className="ep-qtd-num">
                            {numero.format(item.quantidade || 0)}
                            <small>{item.unidade || 'un'}</small>
                          </span>
                          <div className={`ep-bar ${sit}`}>
                            <i style={{ width: `${Math.max(percentualEstoque(item), sit === 'zerado' ? 0 : 3)}%` }} />
                          </div>
                          {item.quantidadeMinima != null && (
                            <span className="ep-hint">mín. {item.quantidadeMinima}</span>
                          )}
                        </div>
                      </td>
                      <td>
                        <span className={`ep-badge ${sit}`}>{ROTULO_SITUACAO[sit]}</span>
                      </td>
                      <td className="num">{item.preco != null ? moeda.format(item.preco) : <span className="ep-hint">sem preço</span>}</td>
                      <td className="num" style={{ fontWeight: 700 }}>
                        {valor != null ? moeda.format(valor) : <span className="ep-hint">—</span>}
                      </td>
                      <td style={{ color: 'var(--ep-muted)', fontVariantNumeric: 'tabular-nums' }}>
                        {formatarData(item.dataCadastro)}
                      </td>
                      <td>
                        <div className="ep-row-actions">
                          <Link to={`/editar/${item.id}`} className="ep-icon-btn" title="Editar" aria-label={`Editar ${item.nome}`}>
                            <FaEdit />
                          </Link>
                          <button
                            type="button"
                            className="ep-icon-btn danger"
                            title="Inativar"
                            aria-label={`Inativar ${item.nome}`}
                            onClick={() => setItemParaInativar(item)}
                          >
                            <FaEyeSlash />
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}

        {visiveis.length > 0 && (
          <div
            className="ep-card-header"
            style={{ borderTop: '1px solid var(--ep-line)', padding: '14px 22px', fontSize: '0.84rem', color: 'var(--ep-muted)' }}
          >
            <span>
              Mostrando <strong style={{ color: 'var(--ep-ink)' }}>{visiveis.length}</strong> de {itens.length} produtos
            </span>
            <span>
              Valor total: <strong style={{ color: 'var(--ep-ink)' }}>{moeda.format(valorVisivel)}</strong>
            </span>
          </div>
        )}
      </div>

      {itemParaInativar && (
        <div className="ep-modal-overlay" onClick={() => !inativando && setItemParaInativar(null)}>
          <div className="ep-modal" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
            <span className="ep-modal-icon">
              <FaEyeSlash />
            </span>
            <h3>Inativar “{itemParaInativar.nome}”?</h3>
            <p>
              O produto sai das listagens e das buscas, mas o histórico continua guardado no banco. Se ele voltar a ser
              vendido, basta cadastrá-lo de novo com o mesmo nome que ele é reativado.
            </p>
            <div className="ep-modal-footer">
              <button type="button" className="ep-btn" onClick={() => setItemParaInativar(null)} disabled={inativando}>
                Cancelar
              </button>
              <button type="button" className="ep-btn ep-btn-danger" onClick={inativarItem} disabled={inativando}>
                {inativando ? 'Inativando...' : 'Inativar produto'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default Itens
