// src/pages/MeetingSetup.jsx

import React, { useState } from 'react';
import AIAgentCard from '../components/meeting/AIAgentCard.jsx';
import './MeetingSetup.css';

// 고유 ID 생성을 위한 간단한 함수
let nextId = 2;

function MeetingSetup() {
  const [topic, setTopic] = useState('');
  const [aiAgents, setAiAgents] = useState([
    { id: 1, type: 'gemini', role: '회의록 요약 담당' },
  ]);

  const addAgent = () => {
    const newAgent = { id: nextId++, type: 'gpt-4', role: '' };
    setAiAgents([...aiAgents, newAgent]);
  };

  const removeAgent = (idToRemove) => {
    setAiAgents(aiAgents.filter(agent => agent.id !== idToRemove));
  };

  const updateAgent = (id, field, value) => {
    setAiAgents(
      aiAgents.map(agent =>
        agent.id === id ? { ...agent, [field]: value } : agent
      )
    );
  };

  return (
    <div className="meeting-setup-page">
      <header className="setup-header">
        <h1>회의 설정</h1>
        <p>AI와 함께할 회의를 준비하세요. 주제와 참여할 AI를 설정할 수 있습니다.</p>
      </header>

      {/* 1. 회의 주제 카드 */}
      <div className="setup-card">
        <h2>회의 주제</h2>
        <textarea
          className="topic-input"
          placeholder="오늘 회의에서 논의할 주요 주제를 입력하세요..."
          value={topic}
          onChange={(e) => setTopic(e.target.value)}
        />
      </div>

      {/* 2. AI 어시스턴트 설정 카드 */}
      <div className="setup-card">
        <div className="card-header">
          <h2>AI 어시스턴트</h2>
          <button className="add-ai-btn" onClick={addAgent}>+ AI 추가</button>
        </div>
        <div className="ai-agents-list">
          {aiAgents.map(agent => (
            <AIAgentCard
              key={agent.id}
              agent={agent}
              onUpdate={updateAgent}
              onRemove={removeAgent}
            />
          ))}
        </div>
      </div>
      
      {/* 3. 추가 설정 (확장성 고려) */}
      <div className="setup-card">
        <h2>추가 설정</h2>
        <p className="placeholder-text">이곳에 녹음, 언어 등 추가 설정을 넣을 수 있습니다.</p>
      </div>

      <div className="start-meeting-container">
        <button className="start-meeting-btn">회의 시작하기</button>
      </div>
    </div>
  );
}

export default MeetingSetup;