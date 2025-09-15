// src/components/meeting/AIAgentCard.jsx

import React from 'react';
import './AIAgentCard.css'; // AIAgentCard 전용 CSS
import { FaTimes } from 'react-icons/fa'; // 삭제 아이콘

function AIAgentCard({ agent, onUpdate, onRemove }) {
  return (
    <div className="ai-agent-card">
      <button className="remove-btn" onClick={() => onRemove(agent.id)}>
        <FaTimes />
      </button>
      <div className="form-group">
        <label htmlFor={`ai-type-${agent.id}`}>AI 모델</label>
        <select
          id={`ai-type-${agent.id}`}
          value={agent.type}
          onChange={(e) => onUpdate(agent.id, 'type', e.target.value)}
        >
          <option value="gemini">Gemini</option>
          <option value="gpt-4">GPT-4</option>
          <option value="claude">Claude</option>
        </select>
      </div>
      <div className="form-group">
        <label htmlFor={`ai-role-${agent.id}`}>역할</label>
        <input
          type="text"
          id={`ai-role-${agent.id}`}
          placeholder="예: 회의록 작성자, 아이디어 제안자"
          value={agent.role}
          onChange={(e) => onUpdate(agent.id, 'role', e.target.value)}
        />
      </div>
    </div>
  );
}

export default AIAgentCard;