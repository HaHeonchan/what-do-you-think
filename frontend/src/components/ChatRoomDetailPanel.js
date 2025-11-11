import { useState, useEffect } from "react"
import { chatRoomAPI, gptAPI } from "../services/api"
import { FileText, MessageSquare, BarChart3, Lightbulb, Search, Zap, Globe, Edit2, Check, X, Trash2 } from "lucide-react"
import { formatModeratorMessage } from "../utils/moderatorFormatter"
import { useChatProcessing } from "../contexts/ChatProcessingContext"

const ChatRoomDetailPanel = ({ roomId, onRoomDeleted }) => {
  const [chatRoom, setChatRoom] = useState(null)
  const [history, setHistory] = useState([])
  const [statistics, setStatistics] = useState(null)
  const [question, setQuestion] = useState("")
  const [promptKeys, setPromptKeys] = useState(["creator", "critic", "analyst"])
  const [conversationRounds, setConversationRounds] = useState(1)
  const [note, setNote] = useState("")
  const [loading, setLoading] = useState(false)
  const [pageLoading, setPageLoading] = useState(true)
  const [activeTab, setActiveTab] = useState("note")
  const [isEditingTitle, setIsEditingTitle] = useState(false)
  const [editingTitle, setEditingTitle] = useState("")
  const { isProcessing, setIsProcessing } = useChatProcessing() // 전역 처리 상태
  const [roomProcessing, setRoomProcessing] = useState(false) // 현재 채팅방이 처리 중인지

  const availableRoles = [
    { key: "creator", label: "생성자", icon: Lightbulb, color: "#fbbf24" },
    { key: "critic", label: "비판자", icon: Search, color: "#60a5fa" },
    { key: "analyst", label: "분석가", icon: BarChart3, color: "#34d399" },
    { key: "researcher", label: "웹 검색", icon: Globe, color: "#8b5cf6" },
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
      setActiveTab("note")
      Promise.all([
        loadChatRoom(),
        loadHistory(),
        loadStatistics()
      ]).then(() => {
        // 채팅방 로드 후 처리 상태 확인
        checkRoomProcessingStatus()
      }).finally(() => {
        setPageLoading(false)
      })
    }
  }, [roomId])

  // 현재 채팅방의 처리 상태 확인
  const checkRoomProcessingStatus = async () => {
    if (!roomId) return
    try {
      const roomRes = await chatRoomAPI.getById(roomId)
      const room = roomRes.data
      setRoomProcessing(room.isProcessing === true)
    } catch (err) {
      console.error("채팅방 처리 상태 확인 실패:", err)
      setRoomProcessing(false)
    }
  }

  // 전역 처리 중일 때 주기적으로 상태 확인 (모든 채팅방에서 확인)
  useEffect(() => {
    if (!isProcessing) return

    const interval = setInterval(async () => {
      try {
        // 현재 채팅방의 처리 상태 확인
        if (roomId) {
          const roomRes = await chatRoomAPI.getById(roomId)
          const room = roomRes.data
          
          // DB의 isProcessing 필드를 확인
          const isRoomProcessing = room.isProcessing === true
          setRoomProcessing(isRoomProcessing)
          
          if (!isRoomProcessing) {
            // 현재 채팅방이 처리 완료되었지만, 다른 채팅방이 처리 중일 수 있으므로
            // 모든 채팅방을 확인해야 하지만, 일단 현재 채팅방만 확인
            // 전역 상태는 다른 채팅방에서도 확인하므로 여기서는 false로 설정
            setIsProcessing(false)
            // 데이터 새로고침
            loadHistory()
            loadChatRoom()
            loadStatistics()
          } else {
            // 아직 처리 중 - 히스토리만 업데이트
            loadHistory()
          }
        }
      } catch (err) {
        console.error("처리 상태 확인 실패:", err)
      }
    }, 3000) // 3초마다 확인
    
    return () => clearInterval(interval)
  }, [isProcessing, roomId])

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
    if (!question.trim() || isProcessing) {
      return
    }

    setLoading(true)
    setRoomProcessing(true) // 현재 채팅방 처리 중
    setIsProcessing(true) // 전역 상태: 요청 시작 시 처리 중으로 표시
    try {
      // 질문 전송 및 응답 받기 (응답에는 업데이트된 노트 내용이 포함됨)
      const response = await gptAPI.sendQuestion({
        chatRoomId: Number.parseInt(roomId),
        question,
        promptKeys,
        conversationRounds,
      })
      
      // 응답으로 받은 요약 내용으로 노트 업데이트
      if (response.data && response.data.message) {
        setNote(response.data.message)
      }
      
      setQuestion("")
      
      // 대화 기록과 통계 새로고침
      await Promise.all([
        loadChatRoom(),
        loadHistory(),
        loadStatistics()
      ])
      
    } catch (err) {
      // ChatResponseDTO는 answer 필드를 사용하므로 answer 또는 error 모두 확인
      const errorMessage = err.response?.data?.answer || err.response?.data?.error || err.response?.data?.message || err.message || "질문 전송에 실패했습니다."
      alert(errorMessage)
      
      // "이미 처리 중" 에러인 경우 전역 상태 유지
      if (err.response?.status === 409 || errorMessage.includes("이미 처리 중") || errorMessage.includes("처리 중인 요청")) {
        // 이미 전역 상태가 true이므로 유지
        setRoomProcessing(true) // 현재 채팅방도 처리 중으로 표시
      } else {
        setIsProcessing(false) // 다른 에러 시 전역 처리 중 상태 해제
        setRoomProcessing(false) // 현재 채팅방도 처리 완료
      }
    } finally {
      setLoading(false)
      // 성공 시 DB의 isProcessing이 false가 될 때까지 상태 유지
      // 주기적 확인으로 자동 해제됨
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

  const handleStartEditTitle = () => {
    setEditingTitle(chatRoom.title || "")
    setIsEditingTitle(true)
  }

  const handleCancelEditTitle = () => {
    setIsEditingTitle(false)
    setEditingTitle("")
  }

  const handleSaveTitle = async () => {
    try {
      const res = await chatRoomAPI.updateTitle(roomId, { title: editingTitle })
      setChatRoom(res.data)
      setIsEditingTitle(false)
      setEditingTitle("")
    } catch (err) {
      alert("제목 저장에 실패했습니다.")
    }
  }

  const handleDeleteRoom = async () => {
    if (!window.confirm("정말 이 채팅방을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.")) {
      return
    }

    try {
      await chatRoomAPI.delete(roomId)
      if (onRoomDeleted) {
        onRoomDeleted()
      }
    } catch (err) {
      alert(err.response?.data?.error || "채팅방 삭제에 실패했습니다.")
    }
  }

  if (pageLoading || !chatRoom) {
    return <div style={styles.center}>로딩 중...</div>
  }

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        {isEditingTitle ? (
          <div style={styles.titleEditContainer}>
            <input
              type="text"
              value={editingTitle}
              onChange={(e) => setEditingTitle(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  handleSaveTitle()
                } else if (e.key === "Escape") {
                  handleCancelEditTitle()
                }
              }}
              autoFocus
              style={styles.titleInput}
            />
            <button onClick={handleSaveTitle} style={styles.titleEditBtn}>
              <Check size={18} />
            </button>
            <button onClick={handleCancelEditTitle} style={styles.titleEditBtn}>
              <X size={18} />
            </button>
          </div>
        ) : (
          <div style={styles.titleContainer}>
            <h1 style={styles.title}>{chatRoom.title || "세션"}</h1>
            <div style={styles.titleButtons}>
              <button 
                onClick={handleStartEditTitle} 
                style={styles.editTitleBtn} 
                title="제목 편집"
                onMouseEnter={(e) => e.target.style.color = "#3b82f6"}
                onMouseLeave={(e) => e.target.style.color = "#9ca3af"}
              >
                <Edit2 size={18} />
              </button>
              <button 
                onClick={handleDeleteRoom} 
                style={styles.deleteTitleBtn} 
                title="채팅방 삭제"
                onMouseEnter={(e) => e.target.style.color = "#ef4444"}
                onMouseLeave={(e) => e.target.style.color = "#9ca3af"}
              >
                <Trash2 size={18} />
              </button>
            </div>
          </div>
        )}
      </div>

      <div style={styles.tabs}>
        {[
          { key: "note", label: "노트", icon: FileText },
          { key: "chat", label: "대화", icon: MessageSquare },
          { key: "stats", label: "통계", icon: BarChart3 },
        ].map((tab) => {
          const Icon = tab.icon
          return (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              style={{
                ...styles.tab,
                ...(activeTab === tab.key && styles.tabActive),
              }}
            >
              <Icon size={18} style={{ marginRight: "8px" }} />
              {tab.label}
            </button>
          )
        })}
      </div>

      {activeTab === "chat" && (
        <div style={styles.chatContainer}>
          <div style={styles.history}>
            {(loading || roomProcessing) ? (
              <div style={styles.loadingOverlay}>
                <div style={styles.loadingSpinner}></div>
                <p style={styles.loadingText}>응답을 생성하고 있습니다...</p>
              </div>
            ) : history.length === 0 ? (
              <div style={styles.emptyHistory}>
                <p>아직 대화가 없습니다</p>
                <p style={styles.emptyHistorySubtext}>질문을 입력하여 시작하세요</p>
              </div>
            ) : (
              history.map((msg) => {
                const displayMessage = msg.sender === "moderator" 
                  ? formatModeratorMessage(msg.message)
                  : msg.message;
                return (
                  <div key={msg.id} style={styles.message}>
                    <strong style={styles.sender}>{msg.sender}:</strong>
                    <span style={styles.messageText}>{displayMessage}</span>
                  </div>
                );
              })
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
                disabled={loading || isProcessing}
              />
              <button 
                type="submit" 
                disabled={loading || isProcessing} 
                style={{
                  ...styles.sendBtn,
                  ...(isProcessing && !loading ? styles.sendBtnDisabled : {})
                }}
                title={isProcessing && !loading ? "다른 채팅방에서 요청을 처리 중입니다. 완료 후 다시 시도해주세요." : ""}
              >
                {loading ? "전송 중..." : isProcessing ? "처리 중..." : "전송"}
              </button>
            </div>
            <div style={styles.optionsContainer}>
              <div style={styles.roleSelection}>
                <div style={styles.roleSelectionLabel}>역할:</div>
                <div style={styles.roleCheckboxes}>
                  {availableRoles.map((role) => {
                    const Icon = role.icon
                    return (
                      <label key={role.key} style={styles.roleCheckbox}>
                        <input
                          type="checkbox"
                          checked={promptKeys.includes(role.key)}
                          onChange={() => handleRoleToggle(role.key)}
                          style={styles.checkbox}
                        />
                        <span style={styles.roleLabel}>
                          <Icon size={16} color={role.color} style={{ marginRight: "6px" }} />
                          {role.label}
                        </span>
                      </label>
                    )
                  })}
                </div>
              </div>
              <div style={styles.options}>
                  <label style={styles.optionLabel}>
                    최대 대화 횟수:
                    <input
                      type="number"
                      value={conversationRounds}
                      onChange={(e) => setConversationRounds(Number.parseInt(e.target.value))}
                      min="1"
                      max="20"
                      style={styles.numberInput}
                    />
                  </label>
                  <div style={{ fontSize: "12px", color: "#666", marginTop: "4px" }}>
                    모더레이터가 충분히 논의되었다고 판단하면 그 전에 종료될 수 있습니다.
                  </div>
              </div>
            </div>
          </form>
        </div>
      )}

      {activeTab === "note" && (
        <div style={styles.noteContainer}>
          <div style={styles.noteContent}>
            {(loading || roomProcessing) ? (
              <div style={styles.noteLoadingOverlay}>
                <div style={styles.loadingSpinner}></div>
                <p style={styles.loadingText}>응답을 생성하고 있습니다...</p>
              </div>
            ) : (
              <>
                <textarea
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  placeholder="노트를 작성하세요..."
                  style={styles.noteTextarea}
                />
                <button onClick={handleSaveNote} style={styles.saveBtn}>
                  저장
                </button>
              </>
            )}
          </div>
          
          <div style={styles.questionSection}>
            <form onSubmit={handleSendQuestion} style={styles.form}>
              <div style={styles.formRow}>
                <input
                  type="text"
                  value={question}
                  onChange={(e) => setQuestion(e.target.value)}
                  placeholder="질문을 입력하세요..."
                  style={styles.input}
                  disabled={loading || isProcessing}
                />
                <button 
                  type="submit" 
                  disabled={loading || isProcessing} 
                  style={{
                    ...styles.sendBtn,
                    ...(isProcessing && !loading ? styles.sendBtnDisabled : {})
                  }}
                  title={isProcessing && !loading ? "다른 채팅방에서 요청을 처리 중입니다. 완료 후 다시 시도해주세요." : ""}
                >
                  {loading ? "전송 중..." : isProcessing ? "처리 중..." : "전송"}
                </button>
              </div>
              <div style={styles.optionsContainer}>
                <div style={styles.roleSelection}>
                  <div style={styles.roleSelectionLabel}>역할 선택:</div>
                  <div style={styles.roleCheckboxes}>
                    {availableRoles.map((role) => {
                      const Icon = role.icon
                      return (
                        <label key={role.key} style={styles.roleCheckbox}>
                          <input
                            type="checkbox"
                            checked={promptKeys.includes(role.key)}
                            onChange={() => handleRoleToggle(role.key)}
                            style={styles.checkbox}
                          />
                          <span style={styles.roleLabel}>
                            <Icon size={16} color={role.color} style={{ marginRight: "6px" }} />
                            {role.label}
                          </span>
                        </label>
                      )
                    })}
                  </div>
                </div>
                <div style={styles.options}>
                  <label style={styles.optionLabel}>
                    대화 횟수:
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
  titleButtons: {
    display: "flex",
    alignItems: "center",
    gap: "8px",
  },
  titleContainer: {
    display: "flex",
    alignItems: "center",
    gap: "12px",
    flex: 1,
  },
  title: {
    fontSize: "24px",
    fontWeight: "700",
    color: "#ffffff",
    margin: 0,
  },
  editTitleBtn: {
    background: "none",
    border: "none",
    color: "#9ca3af",
    cursor: "pointer",
    padding: "4px",
    display: "flex",
    alignItems: "center",
    transition: "color 0.2s",
  },
  deleteTitleBtn: {
    background: "none",
    border: "none",
    color: "#9ca3af",
    cursor: "pointer",
    padding: "4px",
    display: "flex",
    alignItems: "center",
    transition: "color 0.2s",
  },
  titleEditContainer: {
    display: "flex",
    alignItems: "center",
    gap: "8px",
    flex: 1,
  },
  titleInput: {
    flex: 1,
    padding: "8px 12px",
    border: "1px solid rgba(59, 130, 246, 0.3)",
    borderRadius: "6px",
    fontSize: "24px",
    fontWeight: "700",
    backgroundColor: "rgba(15, 20, 25, 0.6)",
    color: "#ffffff",
    outline: "none",
  },
  titleEditBtn: {
    background: "rgba(59, 130, 246, 0.2)",
    border: "1px solid rgba(59, 130, 246, 0.3)",
    borderRadius: "6px",
    color: "#3b82f6",
    cursor: "pointer",
    padding: "8px",
    display: "flex",
    alignItems: "center",
    transition: "all 0.2s",
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
    display: "flex",
    alignItems: "center",
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
  sendBtnDisabled: {
    background: "rgba(59, 130, 246, 0.3)",
    cursor: "not-allowed",
    opacity: 0.6,
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
    flex: 1,
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
    gap: "20px",
    flex: 1,
    overflow: "hidden",
  },
  noteContent: {
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
    minHeight: "200px",
  },
  questionSection: {
    marginTop: "auto",
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
  loadingOverlay: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    justifyContent: "center",
    padding: "60px 20px",
    color: "#d1d5db",
  },
  noteLoadingOverlay: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    justifyContent: "center",
    flex: 1,
    padding: "60px 20px",
    color: "#d1d5db",
  },
  loadingSpinner: {
    width: "48px",
    height: "48px",
    border: "4px solid rgba(59, 130, 246, 0.2)",
    borderTop: "4px solid #3b82f6",
    borderRadius: "50%",
    animation: "spin 1s linear infinite",
    marginBottom: "16px",
  },
  loadingText: {
    fontSize: "14px",
    color: "#9ca3af",
    margin: 0,
  },
}

export default ChatRoomDetailPanel

