import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { chatRoomAPI, gptAPI } from '../services/api';

const CreateChatRoom = () => {
  const [title, setTitle] = useState('');
  const [question, setQuestion] = useState('');
  const [promptKeys, setPromptKeys] = useState(['engineer', 'philosopher', 'economist']);
  const [conversationRounds, setConversationRounds] = useState(1);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!question.trim()) {
      alert('질문을 입력해주세요.');
      return;
    }

    setLoading(true);
    try {
      // 질문 전송 (chatRoomId 없으면 자동 생성)
      await gptAPI.sendQuestion({
        question,
        promptKeys,
        conversationRounds,
      });
      
      // 대화방 목록 새로고침 후 첫 번째 방으로 이동
      const res = await chatRoomAPI.getAll();
      if (res.data.length > 0) {
        navigate(`/chat-rooms/${res.data[0].id}`);
      } else {
        navigate('/chat-rooms');
      }
    } catch (err) {
      alert(err.response?.data?.error || '대화방 생성에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <h1>새 대화방 만들기</h1>
        <form onSubmit={handleSubmit} style={styles.form}>
          <label>
            제목 (선택)
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="대화방 제목"
              style={styles.input}
            />
          </label>
          
          <label>
            질문 *
            <textarea
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              placeholder="질문을 입력하세요..."
              required
              style={styles.textarea}
              rows={5}
            />
          </label>

          <label>
            대화 라운드 수
            <input
              type="number"
              value={conversationRounds}
              onChange={(e) => setConversationRounds(parseInt(e.target.value))}
              min="1"
              max="5"
              style={styles.numberInput}
            />
          </label>

          <div style={styles.buttons}>
            <button type="button" onClick={() => navigate('/chat-rooms')} style={styles.cancelBtn}>
              취소
            </button>
            <button type="submit" disabled={loading} style={styles.submitBtn}>
              {loading ? '생성 중...' : '대화 시작'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

const styles = {
  container: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    minHeight: '100vh',
    backgroundColor: '#f5f5f5',
    padding: '20px',
  },
  card: {
    backgroundColor: 'white',
    padding: '40px',
    borderRadius: '8px',
    boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
    width: '100%',
    maxWidth: '600px',
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: '20px',
  },
  input: {
    width: '100%',
    padding: '12px',
    border: '1px solid #ddd',
    borderRadius: '4px',
    fontSize: '16px',
    marginTop: '5px',
  },
  textarea: {
    width: '100%',
    padding: '12px',
    border: '1px solid #ddd',
    borderRadius: '4px',
    fontSize: '16px',
    marginTop: '5px',
    fontFamily: 'inherit',
    resize: 'vertical',
  },
  numberInput: {
    width: '80px',
    padding: '8px',
    border: '1px solid #ddd',
    borderRadius: '4px',
    marginLeft: '10px',
  },
  buttons: {
    display: 'flex',
    gap: '10px',
    justifyContent: 'flex-end',
    marginTop: '20px',
  },
  cancelBtn: {
    padding: '12px 24px',
    backgroundColor: '#6c757d',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
  },
  submitBtn: {
    padding: '12px 24px',
    backgroundColor: '#007bff',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
  },
};

export default CreateChatRoom;

