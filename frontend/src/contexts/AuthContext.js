import { createContext, useContext, useState, useEffect } from 'react';
import { authAPI, memberAPI } from '../services/api';

const AuthContext = createContext(null);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // 토큰이 있으면 사용자 정보 가져오기
    const token = localStorage.getItem('token');
    if (token) {
      memberAPI.getCurrent()
        .then((res) => {
          setUser(res.data);
        })
        .catch(() => {
          localStorage.removeItem('token');
          localStorage.removeItem('userId');
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, []);

  const login = async (username, password) => {
    const res = await authAPI.login({ username, password });
    const { token, userId, username: userUsername, email } = res.data;
    localStorage.setItem('token', token);
    localStorage.setItem('userId', userId);
    setUser({ id: userId, username: userUsername, email });
    return res.data;
  };

  const register = async (username, email, password) => {
    const res = await authAPI.register({ username, email, password });
    const { token, userId, username: userUsername, email: userEmail } = res.data;
    localStorage.setItem('token', token);
    localStorage.setItem('userId', userId);
    setUser({ id: userId, username: userUsername, email: userEmail });
    return res.data;
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

