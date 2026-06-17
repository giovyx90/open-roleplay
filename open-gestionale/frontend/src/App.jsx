import { useAuth } from './auth/AuthContext.jsx';
import Login from './pages/Login.jsx';
import DemoPicker from './pages/DemoPicker.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Splash from './components/Splash.jsx';

/*
 * Top-level view switch. No router dependency: the session status alone decides
 * what to show. In demo mode the login screen is the profile picker.
 */
export default function App() {
  const { status, server, error } = useAuth();

  if (status === 'boot' || status === 'loading') return <Splash label="Caricamento…" />;
  if (status === 'error') {
    return (
      <Splash
        label="Impossibile contattare l'API"
        detail={(error && error.message) || 'Verifica VITE_API_URL e che il bridge sia attivo.'}
        tone="error"
      />
    );
  }
  if (status === 'auth') return <Dashboard />;
  return server && server.mode === 'demo' ? <DemoPicker /> : <Login />;
}
