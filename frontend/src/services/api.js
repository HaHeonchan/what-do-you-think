import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

// axios 인스턴스 생성
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터: 토큰 자동 추가
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 응답 인터셉터: 401 에러 시 로그아웃
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('userId');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// 인증 API
export const authAPI = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  guest: () => api.post('/auth/guest'),
};

// 멤버 API
export const memberAPI = {
  getCurrent: () => api.get('/members/me'),
};

// 대화방 API
export const chatRoomAPI = {
  create: (data) => api.post('/chat-rooms', data),
  getAll: () => api.get('/chat-rooms'),
  getById: (id) => api.get(`/chat-rooms/${id}`),
  updateNote: (id, data) => api.put(`/chat-rooms/${id}/note`, data),
  updateTitle: (id, data) => api.put(`/chat-rooms/${id}/title`, data),
  delete: (id) => api.delete(`/chat-rooms/${id}`),
  getHistory: (id) => api.get(`/chat-rooms/${id}/history`),
  getStatistics: (id) => api.get(`/chat-rooms/${id}/statistics`),
};

// GPT 대화 API
export const gptAPI = {
  sendQuestion: (data) => api.post('/gpt/question', data),
  summarize: (chatRoomId) => api.get(`/gpt/summarize?chatRoomId=${chatRoomId}`),
};

export default api;

