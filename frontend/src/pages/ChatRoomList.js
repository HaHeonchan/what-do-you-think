import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { chatRoomAPI } from '../services/api';

const ChatRoomList = () => {
  const [chatRooms, setChatRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    loadChatRooms();
  }, []);

  const loadChatRooms = async () => {
    try {
      const res = await chatRoomAPI.getAll();
      setChatRooms(res.data);
    } catch (err) {
      setError('대화방 목록을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateRoom = () => {
    navigate('/chat-rooms/new');
  };

  if (loading) {
    return <div style={styles.center}>로딩 중...</div>;
  }

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h1>대화방 목록</h1>
        <div style={styles.userInfo}>
          <span>{user?.username}님</span>
          <button onClick={logout} style={styles.logoutBtn}>로그아웃</button>
        </div>
      </div>

      {error && <div style={styles.error}>{error}</div>}

      <button onClick={handleCreateRoom} style={styles.createBtn}>
        새 대화방 만들기
      </button>

      <div style={styles.roomList}>
        {chatRooms.length === 0 ? (
          <div style={styles.empty}>대화방이 없습니다. 새 대화방을 만들어보세요!</div>
        ) : (
          chatRooms.map((room) => (
            <div
              key={room.id}
              style={styles.roomCard}
              onClick={() => navigate(`/chat-rooms/${room.id}`)}
            >
              <h3>{room.title || '제목 없음'}</h3>
              <p style={styles.meta}>
                {new Date(room.updatedAt).toLocaleDateString()} | 
                메시지 {room.chats?.length || 0}개
              </p>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

const styles = {
  container: {
    maxWidth: '1200px',
    margin: '0 auto',
    padding: '20px',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '30px',
  },
  userInfo: {
    display: 'flex',
    gap: '15px',
    alignItems: 'center',
  },
  logoutBtn: {
    padding: '8px 16px',
    backgroundColor: '#dc3545',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
  },
  createBtn: {
    padding: '12px 24px',
    backgroundColor: '#007bff',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    fontSize: '16px',
    cursor: 'pointer',
    marginBottom: '20px',
  },
  roomList: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
    gap: '20px',
  },
  roomCard: {
    padding: '20px',
    backgroundColor: 'white',
    borderRadius: '8px',
    boxShadow: '0 2px 5px rgba(0,0,0,0.1)',
    cursor: 'pointer',
    transition: 'transform 0.2s',
  },
  roomCardHover: {
    transform: 'translateY(-2px)',
  },
  meta: {
    color: '#666',
    fontSize: '14px',
    marginTop: '10px',
  },
  empty: {
    textAlign: 'center',
    padding: '40px',
    color: '#999',
  },
  error: {
    color: 'red',
    padding: '10px',
    backgroundColor: '#ffe6e6',
    borderRadius: '4px',
    marginBottom: '20px',
  },
  center: {
    textAlign: 'center',
    padding: '40px',
  },
};

export default ChatRoomList;

