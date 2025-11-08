import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { chatRoomAPI, gptAPI } from '../services/api';

const ChatRoomDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [chatRoom, setChatRoom] = useState(null);
  const [history, setHistory] = useState([]);
  const [statistics, setStatistics] = useState(null);
  const [question, setQuestion] = useState('');
  const [promptKeys, setPromptKeys] = useState(['engineer', 'philosopher', 'economist']);
  const [conversationRounds, setConversationRounds] = useState(1);
  const [note, setNote] = useState('');
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('chat');

  useEffect(() => {
    loadChatRoom();
    loadHistory();
    loadStatistics();
  }, [id]);

  const loadChatRoom = async () => {
    try {
      const res = await chatRoomAPI.getById(id);
      setChatRoom(res.data);
      setNote(res.data.note || '');
    } catch (err) {
      alert('대화방을 불러오는데 실패했습니다.');
      navigate('/chat-rooms');
    }
  };

  const loadHistory = async () => {
    try {
      const res = await chatRoomAPI.getHistory(id);
      setHistory(res.data);
    } catch (err) {
      console.error('대화 기록 로드 실패:', err);
    }
  };

  const loadStatistics = async () => {
    try {
      const res = await chatRoomAPI.getStatistics(id);
      setStatistics(res.data);
    } catch (err) {
      console.error('통계 로드 실패:', err);
    }
  };

  const handleSendQuestion = async (e) => {
    e.preventDefault();
    if (!question.trim()) return;

    setLoading(true);
    try {
      await gptAPI.sendQuestion({
        chatRoomId: parseInt(id),
        question,
        promptKeys,
        conversationRounds,
      });
      setQuestion('');
      await loadChatRoom();
      await loadHistory();
      await loadStatistics();
    } catch (err) {
      alert(err.response?.data?.error || '질문 전송에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleSaveNote = async () => {
    try {
      await chatRoomAPI.updateNote(id, { note });
      alert('노트가 저장되었습니다.');
    } catch (err) {
      alert('노트 저장에 실패했습니다.');
    }
  };

  if (!chatRoom) {
    return <div style={styles.center}>로딩 중...</div>;
  }

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <button onClick={() => navigate('/chat-rooms')} style={styles.backBtn}>
          ← 목록으로
        </button>
        <h1>{chatRoom.title || '대화방'}</h1>
      </div>

      <div style={styles.tabs}>
        <button
          onClick={() => setActiveTab('chat')}
          style={{ ...styles.tab, ...(activeTab === 'chat' && styles.tabActive) }}
        >
          대화
        </button>
        <button
          onClick={() => setActiveTab('note')}
          style={{ ...styles.tab, ...(activeTab === 'note' && styles.tabActive) }}
        >
          노트
        </button>
        <button
          onClick={() => setActiveTab('stats')}
          style={{ ...styles.tab, ...(activeTab === 'stats' && styles.tabActive) }}
        >
          통계
        </button>
      </div>

      {activeTab === 'chat' && (
        <div style={styles.chatContainer}>
          <div style={styles.history}>
            {history.map((msg) => (
              <div key={msg.id} style={styles.message}>
                <strong>{msg.sender}:</strong> {msg.message}
              </div>
            ))}
          </div>

          <form onSubmit={handleSendQuestion} style={styles.form}>
            <div style={styles.formRow}>
              <input
                type="text"
                value={question}
                onChange={(e) => setQuestion(e.target.value)}
                placeholder="질문을 입력하세요..."
                style={styles.input}
                disabled={loading}
              />
              <button type="submit" disabled={loading} style={styles.sendBtn}>
                {loading ? '전송 중...' : '전송'}
              </button>
            </div>
            <div style={styles.options}>
              <label>
                라운드 수:
                <input
                  type="number"
                  value={conversationRounds}
                  onChange={(e) => setConversationRounds(parseInt(e.target.value))}
                  min="1"
                  max="5"
                  style={styles.numberInput}
                />
              </label>
            </div>
          </form>
        </div>
      )}

      {activeTab === 'note' && (
        <div style={styles.noteContainer}>
          <textarea
            value={note}
            onChange={(e) => setNote(e.target.value)}
            placeholder="노트를 작성하세요..."
            style={styles.noteTextarea}
            rows={20}
          />
          <button onClick={handleSaveNote} style={styles.saveBtn}>
            저장
          </button>
        </div>
      )}

      {activeTab === 'stats' && statistics && (
        <div style={styles.statsContainer}>
          <h2>통계</h2>
          <div style={styles.statItem}>
            <strong>총 메시지 수:</strong> {statistics.totalMessages}
          </div>
          <div style={styles.statItem}>
            <strong>총 토큰 사용량:</strong> {statistics.totalTokensUsed?.toLocaleString() || 0}
          </div>
          <div style={styles.statItem}>
            <strong>생성일:</strong> {new Date(statistics.createdAt).toLocaleString()}
          </div>
          <div style={styles.statItem}>
            <strong>수정일:</strong> {new Date(statistics.updatedAt).toLocaleString()}
          </div>
          {statistics.roleParticipationCount && (
            <div style={styles.statItem}>
              <strong>역할별 참여 횟수:</strong>
              <ul>
                {Object.entries(statistics.roleParticipationCount).map(([role, count]) => (
                  <li key={role}>{role}: {count}회</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
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
    alignItems: 'center',
    gap: '20px',
    marginBottom: '20px',
  },
  backBtn: {
    padding: '8px 16px',
    backgroundColor: '#6c757d',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
  },
  tabs: {
    display: 'flex',
    gap: '10px',
    marginBottom: '20px',
    borderBottom: '1px solid #ddd',
  },
  tab: {
    padding: '10px 20px',
    backgroundColor: 'transparent',
    border: 'none',
    cursor: 'pointer',
    borderBottom: '2px solid transparent',
  },
  tabActive: {
    borderBottomColor: '#007bff',
    color: '#007bff',
  },
  chatContainer: {
    display: 'flex',
    flexDirection: 'column',
    height: '70vh',
  },
  history: {
    flex: 1,
    overflowY: 'auto',
    padding: '20px',
    backgroundColor: '#f9f9f9',
    borderRadius: '8px',
    marginBottom: '20px',
  },
  message: {
    marginBottom: '15px',
    padding: '10px',
    backgroundColor: 'white',
    borderRadius: '4px',
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: '10px',
  },
  formRow: {
    display: 'flex',
    gap: '10px',
  },
  input: {
    flex: 1,
    padding: '12px',
    border: '1px solid #ddd',
    borderRadius: '4px',
    fontSize: '16px',
  },
  sendBtn: {
    padding: '12px 24px',
    backgroundColor: '#007bff',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
  },
  options: {
    display: 'flex',
    gap: '20px',
    fontSize: '14px',
  },
  numberInput: {
    width: '60px',
    marginLeft: '10px',
    padding: '5px',
  },
  noteContainer: {
    display: 'flex',
    flexDirection: 'column',
    gap: '10px',
  },
  noteTextarea: {
    width: '100%',
    padding: '15px',
    border: '1px solid #ddd',
    borderRadius: '4px',
    fontSize: '16px',
    fontFamily: 'inherit',
  },
  saveBtn: {
    padding: '12px 24px',
    backgroundColor: '#28a745',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    alignSelf: 'flex-end',
  },
  statsContainer: {
    padding: '20px',
    backgroundColor: '#f9f9f9',
    borderRadius: '8px',
  },
  statItem: {
    marginBottom: '15px',
    padding: '10px',
    backgroundColor: 'white',
    borderRadius: '4px',
  },
  center: {
    textAlign: 'center',
    padding: '40px',
  },
};

export default ChatRoomDetail;

