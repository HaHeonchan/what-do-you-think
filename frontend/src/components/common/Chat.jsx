import React, { useState } from 'react';
import axios from 'axios';

function Chat() {
  // 1. 상태 변수 설정
  // 사용자가 입력할 질문, 서버로부터 받은 답변, 로딩 상태를 관리합니다.
  const [question, setQuestion] = useState(''); // 기본 질문 예시
  const [answer, setAnswer] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  // 2. '질문하기' 버튼 클릭 시 실행될 함수
  const handleSendQuestion = async () => {
    if (!question.trim()) {
      alert('질문을 입력해주세요!');
      return;
    }

    // 요청 시작: 로딩 상태 활성화, 이전 답변/에러 초기화
    setIsLoading(true);
    setAnswer('');
    setError(null);

    // 3. 백엔드에 보낼 데이터 객체 생성
    const requestData = {
      question: question,
      sender: "user",
      receiver: "assistant"
    };

    try {
      // 4. axios.post로 백엔드 API에 POST 요청 보내기
      const response = await axios.post('http://localhost:8080/api/gpt/question', requestData);

      // 5. 성공적으로 응답을 받으면 answer 상태 업데이트
      setAnswer(response.data.answer);

    } catch (err) {
      // 6. 요청 중 에러 발생 시 에러 상태 업데이트
      setError('데이터를 불러오는 데 실패했습니다. 서버 상태를 확인해주세요.');
      console.error('API Error:', err);
    } finally {
      // 7. 요청 완료 (성공/실패 무관) 후 로딩 상태 비활성화
      setIsLoading(false);
    }
  };

  return (
    <div style={{ padding: '20px', fontFamily: 'sans-serif' }}>
      <h1>AI 챗봇에게 질문하기 🤖</h1>
      <div style={{ marginBottom: '10px' }}>
        <input
          type="text"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="여기에 질문을 입력하세요"
          style={{ width: '300px', padding: '10px', marginRight: '10px' }}
        />
        <button onClick={handleSendQuestion} disabled={isLoading} style={{ padding: '10px' }}>
          {isLoading ? '전송 중...' : '질문하기'}
        </button>
      </div>

      {/* 답변 또는 에러 메시지 표시 */}
      {answer && (
        <div style={{ marginTop: '20px', padding: '10px', border: '1px solid lightgreen', borderRadius: '5px' }}>
          <h3>답변:</h3>
          <p>{answer}</p>
        </div>
      )}
      {error && (
        <div style={{ marginTop: '20px', padding: '10px', border: '1px solid red', borderRadius: '5px', color: 'red' }}>
          <h3>오류:</h3>
          <p>{error}</p>
        </div>
      )}
    </div>
  );
}

export default Chat;