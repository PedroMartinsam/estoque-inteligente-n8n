// Funcoes compartilhadas pelas telas de estoque

export const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
export const numero = new Intl.NumberFormat('pt-BR')

/** Situacao do estoque de um item: 'ok' | 'baixo' | 'zerado' */
export function situacaoEstoque(item) {
  const qtd = Number(item.quantidade) || 0
  if (qtd <= 0) return 'zerado'
  if (item.quantidadeMinima != null && item.quantidadeMinima !== '' && qtd <= Number(item.quantidadeMinima)) return 'baixo'
  return 'ok'
}

export const ROTULO_SITUACAO = { ok: 'Em dia', baixo: 'Baixo', zerado: 'Zerado' }

export const capitalizar = (t) => (t ? t.charAt(0).toUpperCase() + t.slice(1) : t)

/** Percentual da barra de estoque: cheia quando o saldo e o dobro do minimo. */
export function percentualEstoque(item) {
  const qtd = Number(item.quantidade) || 0
  const min = Number(item.quantidadeMinima) || 0
  if (min > 0) return Math.min(100, (qtd / (min * 2)) * 100)
  return qtd > 0 ? 100 : 0
}
