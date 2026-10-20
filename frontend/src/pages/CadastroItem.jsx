import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { FaArrowLeft, FaBoxOpen, FaLightbulb, FaSave, FaTag } from 'react-icons/fa'
import { toast } from 'react-toastify'
import { itemService } from '../services/api'
import { moeda, numero, percentualEstoque, ROTULO_SITUACAO, situacaoEstoque } from '../utils/estoque'
import '../styles/premium.css'

const UNIDADES = [
  ['unidade', 'Unidade'],
  ['kg', 'Quilograma (kg)'],
  ['g', 'Grama (g)'],
  ['l', 'Litro (L)'],
  ['ml', 'Mililitro (ml)'],
  ['caixa', 'Caixa'],
  ['pacote', 'Pacote'],
  ['maco', 'Maço'],
  ['bandeja', 'Bandeja']
]

const VAZIO = {
  nome: '',
  descricao: '',
  quantidade: '',
  preco: '',
  categoria: '',
  unidade: 'unidade',
  quantidadeMinima: ''
}

function CadastroItem() {
  const { id } = useParams()
  const navigate = useNavigate()
  const isEditing = Boolean(id)

  const [carregando, setCarregando] = useState(isEditing)
  const [salvando, setSalvando] = useState(false)
  const [formData, setFormData] = useState(VAZIO)
  const [categorias, setCategorias] = useState([])

  useEffect(() => {
    itemService
      .listarCategorias()
      .then((res) => setCategorias(res.data || []))
      .catch(() => setCategorias([]))
  }, [])

  useEffect(() => {
    if (!isEditing) {
      setFormData(VAZIO)
      return
    }
    const carregarItem = async () => {
      try {
        setCarregando(true)
        const { data: item } = await itemService.buscarPorId(id)
        setFormData({
          nome: item.nome ?? '',
          descricao: item.descricao ?? '',
          quantidade: item.quantidade ?? '',
          preco: item.preco ?? '',
          categoria: item.categoria ?? '',
          unidade: item.unidade || 'unidade',
          quantidadeMinima: item.quantidadeMinima ?? ''
        })
      } catch (error) {
        toast.error('Erro ao carregar item')
        navigate('/itens')
      } finally {
        setCarregando(false)
      }
    }
    carregarItem()
  }, [id, isEditing, navigate])

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()

    if (!formData.nome.trim()) {
      toast.error('Informe o nome do produto')
      return
    }
    if (formData.quantidade === '' || Number(formData.quantidade) < 0) {
      toast.error('Informe uma quantidade de zero para cima')
      return
    }

    const payload = {
      ...formData,
      nome: formData.nome.trim(),
      categoria: formData.categoria.trim() || null,
      quantidade: parseInt(formData.quantidade, 10),
      preco: formData.preco !== '' ? parseFloat(formData.preco) : null,
      quantidadeMinima: formData.quantidadeMinima !== '' ? parseInt(formData.quantidadeMinima, 10) : null
    }

    try {
      setSalvando(true)
      if (isEditing) {
        await itemService.atualizar(id, payload)
        toast.success('Produto atualizado!')
      } else {
        await itemService.salvar(payload)
        toast.success('Produto cadastrado!')
      }
      navigate('/itens')
    } catch (error) {
      // o backend manda o motivo em "erro" (ex: nome ja cadastrado)
      const motivo = error.response?.data?.erro || error.response?.data?.message
      toast.error(motivo || (isEditing ? 'Erro ao atualizar produto' : 'Erro ao cadastrar produto'))
      console.error(error)
    } finally {
      setSalvando(false)
    }
  }

  // pre-visualizacao ao vivo
  const previa = useMemo(() => {
    const item = {
      quantidade: formData.quantidade === '' ? 0 : Number(formData.quantidade),
      quantidadeMinima: formData.quantidadeMinima === '' ? null : Number(formData.quantidadeMinima)
    }
    const preco = formData.preco === '' ? null : Number(formData.preco)
    return {
      sit: situacaoEstoque(item),
      pct: percentualEstoque(item),
      qtd: item.quantidade,
      preco,
      valor: preco != null ? item.quantidade * preco : null
    }
  }, [formData.quantidade, formData.quantidadeMinima, formData.preco])

  if (carregando) {
    return (
      <div className="ep-loading">
        <div className="ep-spinner" />
      </div>
    )
  }

  const unidadeRotulo = formData.unidade || 'un'

  return (
    <div className="ep-page">
      <header className="ep-page-header">
        <div>
          <span className="ep-eyebrow">{isEditing ? `Produto #${String(id).padStart(4, '0')}` : 'Novo produto'}</span>
          <h1 className="ep-title">{isEditing ? 'Editar produto' : 'Cadastrar produto'}</h1>
          <p className="ep-subtitle">
            {isEditing
              ? 'Altere os dados e salve. Para dar entrada ou saída, você também pode usar o Assistente IA.'
              : 'Campos com * são obrigatórios. O nome precisa ser único no estoque.'}
          </p>
        </div>
        <div className="ep-actions">
          <button type="button" className="ep-btn" onClick={() => navigate('/itens')}>
            <FaArrowLeft /> Voltar
          </button>
        </div>
      </header>

      <form onSubmit={handleSubmit} className="ep-grid-2" style={{ gridTemplateColumns: 'minmax(0, 1.6fr) minmax(0, 1fr)', alignItems: 'start' }}>
        {/* ------------------------------------------------------ formulario */}
        <div className="ep-card">
          <div className="ep-card-body" style={{ paddingTop: 22 }}>
            <h2 className="ep-section-title">Identificação</h2>
            <div className="ep-form-grid">
              <div className="ep-field">
                <label className="ep-label" htmlFor="nome">
                  Nome <em>*</em>
                </label>
                <input id="nome" name="nome" className="ep-input" placeholder="Ex: Banana prata" value={formData.nome} onChange={handleChange} required autoFocus={!isEditing} />
              </div>
              <div className="ep-field">
                <label className="ep-label" htmlFor="categoria">
                  Categoria
                </label>
                <input
                  id="categoria"
                  name="categoria"
                  className="ep-input"
                  placeholder="Ex: Frutas"
                  list="lista-categorias"
                  value={formData.categoria}
                  onChange={handleChange}
                />
                <datalist id="lista-categorias">
                  {categorias.map((c) => (
                    <option key={c} value={c} />
                  ))}
                </datalist>
                <span className="ep-hint">Escolha uma existente para não duplicar ("Frutas" x "fruta").</span>
              </div>
              <div className="ep-field full">
                <label className="ep-label" htmlFor="descricao">
                  Descrição
                </label>
                <textarea id="descricao" name="descricao" className="ep-input" rows="3" placeholder="Origem, fornecedor, observações..." value={formData.descricao} onChange={handleChange} />
              </div>
            </div>

            <h2 className="ep-section-title">Estoque</h2>
            <div className="ep-form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))' }}>
              <div className="ep-field">
                <label className="ep-label" htmlFor="quantidade">
                  Quantidade <em>*</em>
                </label>
                <input id="quantidade" type="number" name="quantidade" className="ep-input" placeholder="0" min="0" value={formData.quantidade} onChange={handleChange} required />
              </div>
              <div className="ep-field">
                <label className="ep-label" htmlFor="unidade">
                  Unidade
                </label>
                <select id="unidade" name="unidade" className="ep-input" value={formData.unidade} onChange={handleChange}>
                  {UNIDADES.map(([v, r]) => (
                    <option key={v} value={v}>
                      {r}
                    </option>
                  ))}
                  {!UNIDADES.some(([v]) => v === formData.unidade) && formData.unidade && (
                    <option value={formData.unidade}>{formData.unidade}</option>
                  )}
                </select>
              </div>
              <div className="ep-field">
                <label className="ep-label" htmlFor="quantidadeMinima">
                  Estoque mínimo
                </label>
                <input id="quantidadeMinima" type="number" name="quantidadeMinima" className="ep-input" placeholder="Ex: 10" min="0" value={formData.quantidadeMinima} onChange={handleChange} />
                <span className="ep-hint">Abaixo disso o produto aparece como "Baixo".</span>
              </div>
            </div>

            <h2 className="ep-section-title">Preço</h2>
            <div className="ep-form-grid">
              <div className="ep-field">
                <label className="ep-label" htmlFor="preco">
                  Preço unitário
                </label>
                <div className="ep-input-group">
                  <span className="ep-prefix">R$</span>
                  <input id="preco" type="number" name="preco" className="ep-input" placeholder="0,00" min="0" step="0.01" value={formData.preco} onChange={handleChange} />
                </div>
              </div>
            </div>
          </div>

          <div
            className="ep-card-header"
            style={{ borderTop: '1px solid var(--ep-line)', justifyContent: 'flex-end', padding: '16px 22px' }}
          >
            <button type="button" className="ep-btn" onClick={() => navigate('/itens')} disabled={salvando}>
              Cancelar
            </button>
            <button type="submit" className="ep-btn ep-btn-primary" disabled={salvando}>
              <FaSave /> {salvando ? 'Salvando...' : isEditing ? 'Salvar alterações' : 'Cadastrar produto'}
            </button>
          </div>
        </div>

        {/* ------------------------------------------------------ pre-visualizacao */}
        <aside style={{ position: 'sticky', top: 88, display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div className="ep-card" style={{ overflow: 'hidden' }}>
            <div
              style={{
                padding: '22px',
                background: 'radial-gradient(140% 140% at 0% 0%, #1d3a30 0%, var(--ep-ink) 60%)',
                color: '#fff',
                display: 'flex',
                gap: 14,
                alignItems: 'center'
              }}
            >
              <span className="ep-avatar" style={{ width: 48, height: 48, fontSize: '1.2rem', background: 'rgba(255,255,255,0.12)', color: '#fff' }}>
                {formData.nome ? formData.nome.charAt(0) : <FaBoxOpen />}
              </span>
              <div style={{ minWidth: 0 }}>
                <div style={{ fontSize: '0.7rem', fontWeight: 700, letterSpacing: '0.12em', textTransform: 'uppercase', color: '#9fc2b1' }}>
                  Pré-visualização
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 800, textTransform: 'capitalize', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {formData.nome || 'Nome do produto'}
                </div>
                {formData.categoria && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.8rem', color: '#cfe3d9', marginTop: 2 }}>
                    <FaTag /> {formData.categoria}
                  </div>
                )}
              </div>
            </div>

            <div className="ep-card-body" style={{ paddingTop: 20, display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
                <div>
                  <div className="ep-kpi-label">Em estoque</div>
                  <div className="ep-kpi-value" style={{ marginTop: 6 }}>
                    {numero.format(previa.qtd)}
                    <small>{unidadeRotulo}</small>
                  </div>
                </div>
                <span className={`ep-badge ${previa.sit}`}>{ROTULO_SITUACAO[previa.sit]}</span>
              </div>
              <div className={`ep-bar ${previa.sit}`} style={{ height: 8 }}>
                <i style={{ width: `${previa.pct}%` }} />
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div style={{ background: 'var(--ep-surface-2)', border: '1px solid var(--ep-line)', borderRadius: 10, padding: '12px 14px' }}>
                  <div className="ep-kpi-label" style={{ fontSize: '0.68rem' }}>Preço unit.</div>
                  <div style={{ fontWeight: 800, marginTop: 4, fontVariantNumeric: 'tabular-nums' }}>
                    {previa.preco != null ? moeda.format(previa.preco) : '—'}
                  </div>
                </div>
                <div style={{ background: 'var(--ep-gold-soft)', border: '1px solid #ecdcb9', borderRadius: 10, padding: '12px 14px' }}>
                  <div className="ep-kpi-label" style={{ fontSize: '0.68rem', color: 'var(--ep-gold)' }}>Valor em estoque</div>
                  <div style={{ fontWeight: 800, marginTop: 4, fontVariantNumeric: 'tabular-nums' }}>
                    {previa.valor != null ? moeda.format(previa.valor) : '—'}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className="ep-card" style={{ padding: '16px 18px', display: 'flex', gap: 12, alignItems: 'flex-start' }}>
            <span className="ep-kpi-icon gold" style={{ flexShrink: 0 }}>
              <FaLightbulb />
            </span>
            <p style={{ fontSize: '0.84rem', color: 'var(--ep-muted)', lineHeight: 1.5 }}>
              Dica: depois de cadastrar, cadastre as caixas do produto no Assistente IA ("a caixa M de banana tem 30
              unidades"). Assim você dá entrada dizendo só "chegou uma caixa M".
            </p>
          </div>
        </aside>
      </form>
    </div>
  )
}

export default CadastroItem
