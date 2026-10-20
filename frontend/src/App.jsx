import { Routes, Route } from 'react-router-dom'
import Navbar from './components/Navbar'
import Home from './pages/Home'
import Itens from './pages/Itens'
import CadastroItem from './pages/CadastroItem'
import Automacao from './pages/Automacao'

function App() {
  return (
    <div className="app">
      <Navbar />
      <main className="container">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/itens" element={<Itens />} />
          <Route path="/cadastro" element={<CadastroItem />} />
          <Route path="/editar/:id" element={<CadastroItem />} />
          <Route path="/automacao" element={<Automacao />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
