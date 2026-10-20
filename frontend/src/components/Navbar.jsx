import { Link, useLocation } from 'react-router-dom'
import { FaBoxes, FaHome, FaList, FaPlus, FaRobot } from 'react-icons/fa'
import '../styles/premium.css'

const LINKS = [
  { to: '/', rotulo: 'Início', Icone: FaHome },
  { to: '/itens', rotulo: 'Itens', Icone: FaList },
  { to: '/cadastro', rotulo: 'Cadastrar', Icone: FaPlus },
  { to: '/automacao', rotulo: 'Assistente IA', Icone: FaRobot }
]

function Navbar() {
  const { pathname } = useLocation()

  const dataTexto = new Date().toLocaleDateString('pt-BR', {
    weekday: 'short',
    day: '2-digit',
    month: 'short'
  })
  const hoje = dataTexto.charAt(0).toUpperCase() + dataTexto.slice(1)

  return (
    <nav className="ep-nav">
      <div className="ep-nav-inner">
        <Link to="/" className="ep-brand">
          <span className="ep-brand-mark">
            <FaBoxes />
          </span>
          <span className="ep-brand-text">
            <strong>Sistema de Estoque</strong>
            <span>Hortifruti</span>
          </span>
        </Link>

        <ul className="ep-nav-links">
          {LINKS.map(({ to, rotulo, Icone }) => {
            const ativo = to === '/' ? pathname === '/' : pathname.startsWith(to)
            return (
              <li key={to}>
                <Link to={to} className={`ep-nav-link ${ativo ? 'ativo' : ''}`} aria-current={ativo ? 'page' : undefined}>
                  <Icone />
                  <span className="ep-nav-rotulo">{rotulo}</span>
                </Link>
              </li>
            )
          })}
        </ul>

        <div className="ep-nav-meta">
          <span>{hoje}</span>
          <span className="ep-nav-ia">
            <FaRobot /> IA integrada
          </span>
        </div>
      </div>
    </nav>
  )
}

export default Navbar
