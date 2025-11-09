import { useState, useEffect } from "react"
import { chatRoomAPI, gptAPI } from "../services/api"

const ChatRoomDetailPanel = ({ roomId }) => {
  const [chatRoom, setChatRoom] = useState(null)
  const [history, setHistory] = useState([])
  const [statistics, setStatistics] = useState(null)
  const [question, setQuestion] = useState("")
  const [promptKeys, setPromptKeys] = useState(["creator", "critic", "analyst", "optimizer"])
  const [conversationRounds, setConversationRounds] = useState(1)
  const [note, setNote] = useState("")
  const [loading, setLoading] = useState(false)
  const [pageLoading, setPageLoading] = useState(true)
  const [activeTab, setActiveTab] = useState("chat")

  const availableRoles = [
    { key: "creator", label: "창의적 아이디어 제시자", emoji: "💡" },
    { key: "critic", label: "비판적 분석가", emoji: "🔍" },
    { key: "analyst", label: "객관적 분석가", emoji: "📊" },
    { key: "optimizer", label: "최적화 전문가", emoji: "⚡" },
  ]

  const handleRoleToggle = (roleKey) => {
    setPromptKeys((prev) => {
      if (prev.includes(roleKey)) {
        if (prev.length === 1) return prev
        return prev.filter((key) => key !== roleKey)
      } else {
        return [...prev, roleKey]
      }
    })
  }

  useEffect(() => {
    if (roomId) {
      setPageLoading(true)
      setActiveTab("chat")
      Promise.all([
        loadChatRoom(),
        loadHistory(),
        loadStatistics()
      ]).finally(() => {
        setPageLoading(false)
      })
    }
  }, [roomId])

  const loadChatRoom = async () => {
    try {
      const res = await chatRoomAPI.getById(roomId)
      setChatRoom(res.data)
      setNote(res.data.note || "")
    } catch (err) {
      console.error("대화방 로드 실패:", err)
    }
  }

  const loadHistory = async () => {
    try {
      const res = await chatRoomAPI.getHistory(roomId)
      setHistory(res.data)
    } catch (err) {
      console.error("대화 기록 로드 실패:", err)
    }
  }

  const loadStatistics = async () => {
    try {
      const res = await chatRoomAPI.getStatistics(roomId)
      setStatistics(res.data)
    } catch (err) {
      console.error("통계 로드 실패:", err)
    }
  }

  const handleSendQuestion = async (e) => {
    e.preventDefault()
    if (!question.trim()) return

    setLoading(true)
    try {
      await gptAPI.sendQuestion({
        chatRoomId: Number.parseInt(roomId),
        question,
        promptKeys,
        conversationRounds,
      })
      setQuestion("")
      await loadHistory()
      await loadStatistics()
    } catch (err) {
      alert(err.response?.data?.error || "질문 전송에 실패했습니다.")
    } finally {
      setLoading(false)
    }
  }

  const handleSaveNote = async () => {
    try {
      await chatRoomAPI.updateNote(roomId, { note })
      alert("노트가 저장되었습니다.")
      setChatRoom(prev => ({...prev, note}))
    } catch (err) {
      alert("노트 저장에 실패했습니다.")
    }
  }

  if (pageLoading || !chatRoom) {
    return <div style={styles.center}>로딩 중...</div>
  }

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h1 style={styles.title}>{chatRoom.title || "대화방"}</h1>
      </div>

      <div style={styles.tabs}>
        {["chat", "note", "stats"].map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            style={{
              ...styles.tab,
              ...(activeTab === tab && styles.tabActive),
            }}
          >
            {tab === "chat" && "💬 대화"}
            {tab === "note" && "📝 노트"}
            {tab === "stats" && "📊 통계"}
          </button>
        ))}
      </div>

      {activeTab === "chat" && (
        <div style={styles.chatContainer}>
          <div style={styles.history}>
            {history.length === 0 ? (
              <div style={styles.emptyHistory}>
                <p>아직 대화가 없습니다</p>
                <p style={styles.emptyHistorySubtext}>질문을 입력하여 시작하세요</p>
              </div>
            ) : (
              history.map((msg) => (
                <div key={msg.id} style={styles.message}>
                  <strong style={styles.sender}>{msg.sender}:</strong>
                  <span style={styles.messageText}>{msg.message}</span>
                </div>
              ))
            )}
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
                {loading ? "전송 중..." : "전송"}
              </button>
            </div>
            <div style={styles.optionsContainer}>
              <div style={styles.roleSelection}>
                <div style={styles.roleSelectionLabel}>역할 선택:</div>
                <div style={styles.roleCheckboxes}>
                  {availableRoles.map((role) => (
                    <label key={role.key} style={styles.roleCheckbox}>
                      <input
                        type="checkbox"
                        checked={promptKeys.includes(role.key)}
                        onChange={() => handleRoleToggle(role.key)}
                        style={styles.checkbox}
                      />
                      <span style={styles.roleLabel}>
                        {role.emoji} {role.label}
                      </span>
                    </label>
                  ))}
                </div>
              </div>
              <div style={styles.options}>
                <label style={styles.optionLabel}>
                  라운드 수:
                  <input
                    type="number"
                    value={conversationRounds}
                    onChange={(e) => setConversationRounds(Number.parseInt(e.target.value))}
                    min="1"
                    max="5"
                    style={styles.numberInput}
                  />
                </label>
              </div>
            </div>
          </form>
        </div>
      )}

      {activeTab === "note" && (
        <div style={styles.noteContainer}>
          <textarea
            value={note}
            onChange={(e) => setNote(e.target.value)}
            placeholder="노트를 작성하세요..."
            style={styles.noteTextarea}
          />
          <button onClick={handleSaveNote} style={styles.saveBtn}>
            저장
          </button>
        </div>
      )}

      {activeTab === "stats" && statistics && (
        <div style={styles.statsContainer}>
          <div style={styles.statGrid}>
            <div style={styles.statCard}>
              <div style={styles.statLabel}>총 메시지 수</div>
              <div style={styles.statValue}>{statistics.totalMessages}</div>
            </div>
            <div style={styles.statCard}>
              <div style={styles.statLabel}>총 토큰 사용량</div>
              <div style={styles.statValue}>{statistics.totalTokensUsed?.toLocaleString() || 0}</div>
            </div>
            <div style={styles.statCard}>
              <div style={styles.statLabel}>생성일</div>
              <div style={styles.statValue}>{new Date(statistics.createdAt).toLocaleDateString()}</div>
            </div>
            <div style={styles.statCard}>
              <div style={styles.statLabel}>수정일</div>
              <div style={styles.statValue}>{new Date(statistics.updatedAt).toLocaleDateString()}</div>
            </div>
          </div>

          {statistics.roleParticipationCount && (
            <div style={styles.roleSection}>
              <h3 style={styles.roleTitle}>역할별 참여 횟수</h3>
              <div style={styles.roleList}>
                {Object.entries(statistics.roleParticipationCount).map(([role, count]) => (
                  <div key={role} style={styles.roleItem}>
                    <span style={styles.roleName}>{role}</span>
                    <span style={styles.roleCount}>{count}회</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

const styles = {
  container: {
    flex: 1,
    height: "100vh",
    display: "flex",
    flexDirection: "column",
    backgroundColor: "#374151", // gray-700
    padding: "20px",
    overflow: "hidden",
  },
  header: {
    display: "flex",
    alignItems: "center",
    gap: "20px",
    marginBottom: "20px",
  },
  title: {
    fontSize: "24px",
    fontWeight: "700",
    color: "#ffffff",
    margin: 0,
  },
  tabs: {
    display: "flex",
    gap: "10px",
    marginBottom: "20px",
    borderBottom: "1px solid rgba(59, 130, 246, 0.1)",
    paddingBottom: "0",
  },
  tab: {
    padding: "12px 20px",
    backgroundColor: "transparent",
    border: "none",
    cursor: "pointer",
    borderBottom: "2px solid transparent",
    color: "#d1d5db", // gray-300
    fontSize: "14px",
    fontWeight: "600",
    transition: "all 0.3s ease",
  },
  tabActive: {
    borderBottomColor: "#3b82f6",
    color: "#3b82f6",
  },
  chatContainer: {
    display: "flex",
    flexDirection: "column",
    flex: 1,
    gap: "20px",
    overflow: "hidden",
  },
  history: {
    flex: 1,
    overflowY: "auto",
    padding: "20px",
    backgroundColor: "rgba(15, 20, 25, 0.4)",
    borderRadius: "10px",
    border: "1px solid rgba(59, 130, 246, 0.1)",
  },
  emptyHistory: {
    textAlign: "center",
    color: "#a0a0a0",
    padding: "40px 20px",
  },
  emptyHistorySubtext: {
    fontSize: "13px",
    color: "#707070",
    margin: 0,
  },
  message: {
    marginBottom: "15px",
    padding: "12px",
    backgroundColor: "rgba(26, 31, 46, 0.6)",
    borderRadius: "8px",
    border: "1px solid rgba(59, 130, 246, 0.1)",
  },
  sender: {
    color: "#3b82f6",
  },
  messageText: {
    color: "#e0e0e0",
    marginLeft: "8px",
    whiteSpace: "pre-wrap",
  },
  form: {
    display: "flex",
    flexDirection: "column",
    gap: "12px",
    backgroundColor: "rgba(26, 31, 46, 0.4)",
    padding: "16px",
    borderRadius: "10px",
    border: "1px solid rgba(59, 130, 246, 0.1)",
  },
  formRow: {
    display: "flex",
    gap: "10px",
  },
  input: {
    flex: 1,
    padding: "12px 16px",
    border: "1px solid rgba(59, 130, 246, 0.3)",
    borderRadius: "8px",
    fontSize: "14px",
    backgroundColor: "rgba(15, 20, 25, 0.6)",
    color: "#e0e0e0",
    outline: "none",
    transition: "all 0.3s ease",
  },
  sendBtn: {
    padding: "12px 24px",
    background: "linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)",
    color: "white",
    border: "none",
    borderRadius: "8px",
    cursor: "pointer",
    fontSize: "14px",
    fontWeight: "600",
    transition: "all 0.3s ease",
  },
  optionsContainer: {
    display: "flex",
    flexDirection: "column",
    gap: "16px",
  },
  roleSelection: {
    display: "flex",
    flexDirection: "column",
    gap: "10px",
  },
  roleSelectionLabel: {
    fontSize: "13px",
    color: "#b0b0b0",
    fontWeight: "600",
  },
  roleCheckboxes: {
    display: "flex",
    flexWrap: "wrap",
    gap: "12px",
  },
  roleCheckbox: {
    display: "flex",
    alignItems: "center",
    gap: "8px",
    cursor: "pointer",
    userSelect: "none",
    padding: "8px 12px",
    backgroundColor: "rgba(15, 20, 25, 0.4)",
    borderRadius: "6px",
    border: "1px solid rgba(59, 130, 246, 0.2)",
    transition: "all 0.2s ease",
  },
  checkbox: {
    width: "16px",
    height: "16px",
    cursor: "pointer",
    accentColor: "#3b82f6",
  },
  roleLabel: {
    fontSize: "13px",
    color: "#e0e0e0",
    display: "flex",
    alignItems: "center",
    gap: "6px",
  },
  options: {
    display: "flex",
    gap: "20px",
    fontSize: "13px",
  },
  optionLabel: {
    color: "#b0b0b0",
    display: "flex",
    alignItems: "center",
    gap: "8px",
  },
  numberInput: {
    width: "60px",
    padding: "6px",
    backgroundColor: "rgba(15, 20, 25, 0.6)",
    border: "1px solid rgba(59, 130, 246, 0.3)",
    borderRadius: "6px",
    color: "#e0e0e0",
    fontSize: "13px",
    outline: "none",
  },
  noteContainer: {
    display: "flex",
    flexDirection: "column",
    gap: "16px",
    flex: 1,
    overflow: "hidden",
  },
  noteTextarea: {
    flex: 1,
    width: "100%",
    padding: "16px",
    border: "1px solid rgba(59, 130, 246, 0.3)",
    borderRadius: "10px",
    fontSize: "14px",
    fontFamily: "inherit",
    backgroundColor: "rgba(26, 31, 46, 0.6)",
    color: "#e0e0e0",
    outline: "none",
    resize: "none",
  },
  saveBtn: {
    padding: "12px 24px",
    background: "linear-gradient(135deg, #10b981 0%, #059669 100%)",
    color: "white",
    border: "none",
    borderRadius: "8px",
    cursor: "pointer",
    alignSelf: "flex-end",
    fontSize: "14px",
    fontWeight: "600",
    transition: "all 0.3s ease",
  },
  statsContainer: {
    display: "flex",
    flexDirection: "column",
    gap: "24px",
    flex: 1,
    overflowY: "auto",
  },
  statGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
    gap: "16px",
  },
  statCard: {
    padding: "20px",
    backgroundColor: "rgba(26, 31, 46, 0.6)",
    borderRadius: "10px",
    border: "1px solid rgba(59, 130, 246, 0.2)",
  },
  statLabel: {
    fontSize: "13px",
    color: "#a0a0a0",
    textTransform: "uppercase",
    letterSpacing: "0.5px",
    marginBottom: "8px",
  },
  statValue: {
    fontSize: "24px",
    fontWeight: "700",
    color: "#3b82f6",
  },
  roleSection: {
    padding: "20px",
    backgroundColor: "rgba(26, 31, 46, 0.6)",
    borderRadius: "10px",
    border: "1px solid rgba(59, 130, 246, 0.2)",
  },
  roleTitle: {
    fontSize: "16px",
    fontWeight: "600",
    color: "#ffffff",
    margin: "0 0 16px 0",
  },
  roleList: {
    display: "flex",
    flexDirection: "column",
    gap: "12px",
  },
  roleItem: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    padding: "12px",
    backgroundColor: "rgba(15, 20, 25, 0.4)",
    borderRadius: "8px",
  },
  roleName: {
    color: "#e0e0e0",
    fontWeight: "600",
  },
  roleCount: {
    color: "#3b82f6",
    fontWeight: "700",
  },
  center: {
    textAlign: "center",
    padding: "40px",
    color: "#d1d5db",
    fontSize: "16px",
    flex: 1,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
  },
}

export default ChatRoomDetailPanel

