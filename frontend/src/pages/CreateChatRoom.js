import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { chatRoomAPI, gptAPI } from "../services/api"

const CreateChatRoom = ({ onSuccess }) => {
  const [title, setTitle] = useState("")
  const [question, setQuestion] = useState("")
  const [promptKeys, setPromptKeys] = useState(["creator", "critic", "analyst"])
  const [conversationRounds, setConversationRounds] = useState(1)
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  // 사용 가능한 역할 목록
  const availableRoles = [
    { key: "creator", label: "생성자", emoji: "💡" },
    { key: "critic", label: "비판자", emoji: "🔍" },
    { key: "analyst", label: "분석가", emoji: "📊" },
    { key: "researcher", label: "웹 검색", emoji: "🌐" },
  ]

  const handleRoleToggle = (roleKey) => {
    setPromptKeys((prev) => {
      if (prev.includes(roleKey)) {
        // 최소 1개는 선택되어야 함
        if (prev.length === 1) return prev
        return prev.filter((key) => key !== roleKey)
      } else {
        return [...prev, roleKey]
      }
    })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!question.trim()) {
      alert("질문을 입력해주세요.")
      return
    }

    setLoading(true)
    try {
      // 질문 전송 (chatRoomId 없으면 자동 생성)
      await gptAPI.sendQuestion({
        question,
        promptKeys,
        conversationRounds,
      })

      // 대화방 목록 새로고침 후 첫 번째 방으로 이동
      const res = await chatRoomAPI.getAll()
      if (res.data.length > 0) {
        const newRoomId = res.data[0].id
        if (onSuccess) {
          onSuccess(newRoomId)
        } else {
          navigate(`/chat-rooms/${newRoomId}`)
        }
      } else {
        if (!onSuccess) {
          navigate("/chat-rooms")
        }
      }
    } catch (err) {
      alert(err.response?.data?.error || "세션 생성에 실패했습니다.")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={styles.container}>
      <div style={styles.content}>
        <div style={styles.logo}>
          <div style={styles.logoIcon}>💬</div>
        </div>
        <div style={styles.card}>
          <h1 style={styles.title}>새 세션 만들기</h1>
          <p style={styles.subtitle}>질문을 입력하여 대화를 시작하세요</p>

          <form onSubmit={handleSubmit} style={styles.form}>
            <div style={styles.formGroup}>
              <label style={styles.label}>제목 (선택)</label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="대화방 제목"
                style={styles.input}
              />
            </div>

            <div style={styles.formGroup}>
              <label style={styles.label}>질문 *</label>
              <textarea
                value={question}
                onChange={(e) => setQuestion(e.target.value)}
                placeholder="질문을 입력하세요..."
                required
                style={styles.textarea}
                rows={5}
              />
            </div>

            <div style={styles.formGroup}>
              <label style={styles.label}>역할 *</label>
              <div style={styles.roleSelection}>
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

            <div style={styles.formGroup}>
              <label style={styles.label}>대화 횟수</label>
              <input
                type="number"
                value={conversationRounds}
                onChange={(e) => setConversationRounds(Number.parseInt(e.target.value))}
                min="1"
                max="5"
                style={styles.numberInput}
              />
            </div>

            <div style={styles.buttons}>
              <button type="button" onClick={() => {
                if (onSuccess) {
                  onSuccess(null) // 취소 시 null 전달
                } else {
                  navigate("/chat-rooms")
                }
              }} style={styles.cancelBtn}>
                취소
              </button>
              <button type="submit" disabled={loading} style={styles.submitBtn}>
                {loading ? "생성 중..." : "대화 시작"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}

const styles = {
  container: {
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    minHeight: "100vh",
    padding: "20px",
  },
  content: {
    width: "100%",
    maxWidth: "600px",
  },
  logo: {
    textAlign: "center",
    marginBottom: "30px",
  },
  logoIcon: {
    fontSize: "48px",
    display: "inline-block",
  },
  card: {
    backgroundColor: "rgba(26, 31, 46, 0.8)",
    backdropFilter: "blur(10px)",
    padding: "40px",
    borderRadius: "12px",
    border: "1px solid rgba(59, 130, 246, 0.2)",
    boxShadow: "0 20px 60px rgba(0, 0, 0, 0.3)",
  },
  title: {
    fontSize: "28px",
    fontWeight: "700",
    marginBottom: "10px",
    color: "#ffffff",
    textAlign: "center",
  },
  subtitle: {
    fontSize: "14px",
    color: "#a0a0a0",
    textAlign: "center",
    marginBottom: "25px",
  },
  form: {
    display: "flex",
    flexDirection: "column",
    gap: "20px",
  },
  formGroup: {
    display: "flex",
    flexDirection: "column",
    gap: "8px",
  },
  label: {
    fontSize: "13px",
    fontWeight: "600",
    color: "#b0b0b0",
    textTransform: "uppercase",
    letterSpacing: "0.5px",
  },
  input: {
    padding: "12px 16px",
    border: "1px solid rgba(59, 130, 246, 0.3)",
    borderRadius: "8px",
    fontSize: "14px",
    backgroundColor: "rgba(15, 20, 25, 0.6)",
    color: "#e0e0e0",
    transition: "all 0.3s ease",
    outline: "none",
  },
  textarea: {
    padding: "12px 16px",
    border: "1px solid rgba(59, 130, 246, 0.3)",
    borderRadius: "8px",
    fontSize: "14px",
    backgroundColor: "rgba(15, 20, 25, 0.6)",
    color: "#e0e0e0",
    fontFamily: "inherit",
    resize: "vertical",
    outline: "none",
    transition: "all 0.3s ease",
  },
  numberInput: {
    width: "80px",
    padding: "8px",
    border: "1px solid rgba(59, 130, 246, 0.3)",
    borderRadius: "6px",
    backgroundColor: "rgba(15, 20, 25, 0.6)",
    color: "#e0e0e0",
    fontSize: "14px",
    outline: "none",
  },
  roleSelection: {
    display: "flex",
    flexDirection: "column",
    gap: "12px",
    padding: "16px",
    backgroundColor: "rgba(15, 20, 25, 0.4)",
    borderRadius: "8px",
    border: "1px solid rgba(59, 130, 246, 0.2)",
  },
  roleCheckbox: {
    display: "flex",
    alignItems: "center",
    gap: "10px",
    cursor: "pointer",
    userSelect: "none",
  },
  checkbox: {
    width: "18px",
    height: "18px",
    cursor: "pointer",
    accentColor: "#3b82f6",
  },
  roleLabel: {
    fontSize: "14px",
    color: "#e0e0e0",
    display: "flex",
    alignItems: "center",
    gap: "6px",
  },
  buttons: {
    display: "flex",
    gap: "10px",
    justifyContent: "flex-end",
    marginTop: "20px",
  },
  cancelBtn: {
    padding: "12px 24px",
    backgroundColor: "rgba(107, 114, 128, 0.2)",
    color: "#b0b0b0",
    border: "1px solid rgba(107, 114, 128, 0.3)",
    borderRadius: "8px",
    cursor: "pointer",
    fontSize: "14px",
    fontWeight: "600",
    transition: "all 0.3s ease",
  },
  submitBtn: {
    padding: "12px 24px",
    backgroundColor: "linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)",
    color: "white",
    border: "none",
    borderRadius: "8px",
    cursor: "pointer",
    fontSize: "14px",
    fontWeight: "600",
    transition: "all 0.3s ease",
    background: "linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)",
  },
}

export default CreateChatRoom
