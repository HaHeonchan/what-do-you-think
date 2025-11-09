import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import MainLayout from './pages/MainLayout';
import './App.css';

// 이미 로그인한 사용자를 대화방 목록으로 리다이렉트
const PublicRoute = ({ children }) => {
  const { user, loading } = useAuth();
  
  if (loading) {
    return <div style={{ padding: '20px', textAlign: 'center' }}>로딩 중...</div>;
  }
  
  return user ? <Navigate to="/chat-rooms" /> : children;
};

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<PublicRoute><Login /></PublicRoute>} />
          <Route path="/register" element={<PublicRoute><Register /></PublicRoute>} />
          <Route 
            path="/chat-rooms" 
            element={<ProtectedRoute><MainLayout /></ProtectedRoute>} 
          />
          <Route path="/" element={<Navigate to="/chat-rooms" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
