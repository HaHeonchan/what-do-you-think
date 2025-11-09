import { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import { useAuth } from "../contexts/AuthContext"
import { chatRoomAPI } from "../services/api"

const ChatRoomList = () => {
  const [chatRooms, setChatRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    loadChatRooms()
  }, [])

  const loadChatRooms = async () => {
    try {
      const res = await chatRoomAPI.getAll()
      setChatRooms(res.data)
    } catch (err) {
      setError("대화방 목록을 불러오는데 실패했습니다.")
    } finally {
      setLoading(false)
    }
  }

  const handleCreateRoom = () => {
    navigate("/chat-rooms/new")
  }

  if (loading) {
    return <div style={styles.center}>로딩 중...</div>
  }

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <div style={styles.headerContent}>
          <div style={styles.titleSection}>
            <h1 style={styles.title}>💬 대화방</h1>
            <p style={styles.subtitle}>다양한 관점의 대화를 나누세요</p>
          </div>
          <div style={styles.userSection}>
            <span style={styles.username}>{user?.username}</span>
            <button onClick={logout} style={styles.logoutBtn}>
              로그아웃
            </button>
          </div>
        </div>
      </div>

      {error && <div style={styles.error}>{error}</div>}

      <button onClick={handleCreateRoom} style={styles.createBtn}>
        + 새 대화방
      </button>

      <div style={styles.roomList}>
        {chatRooms.length === 0 ? (
          <div style={styles.empty}>
            <div style={styles.emptyIcon}>📭</div>
            <p>대화방이 없습니다</p>
            <p style={styles.emptySubtext}>새 대화방을 만들어 시작해보세요</p>
          </div>
        ) : (
          chatRooms.map((room) => (
            <div
              key={room.id}
              style={styles.roomCard}
              onClick={() => navigate(`/chat-rooms/${room.id}`)}
              onMouseEnter={(e) => (e.currentTarget.style.transform = "translateY(-4px)")}
              onMouseLeave={(e) => (e.currentTarget.style.transform = "translateY(0)")}
            >
              <div style={styles.roomHeader}>
                <h3 style={styles.roomTitle}>{room.title || "제목 없음"}</h3>
              </div>
              <p style={styles.roomMeta}>
                📅 {new Date(room.updatedAt).toLocaleDateString()}
                <span style={styles.separator}>•</span>💬 {room.chats?.length || 0}개
              </p>
            </div>
          ))
        )}
      </div>
    </div>
  )
}

const styles = {
  container: {
    maxWidth: "1200px",
    margin: "0 auto",
    padding: "40px 20px",
  },
  header: {
    marginBottom: "40px",
  },
  headerContent: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
  },
  titleSection: {
    flex: 1,
  },
  title: {
    fontSize: "32px",
    fontWeight: "700",
    margin: "0 0 8px 0",
    color: "#ffffff",
  },
  subtitle: {
    fontSize: "14px",
    color: "#a0a0a0",
    margin: 0,
  },
  userSection: {
    display: "flex",
    gap: "12px",
    alignItems: "center",
  },
  username: {
    fontSize: "14px",
    color: "#b0b0b0",
    paddingRight: "12px",
    borderRight: "1px solid rgba(59, 130, 246, 0.2)",
  },
  logoutBtn: {
    padding: "8px 16px",
    backgroundColor: "rgba(239, 68, 68, 0.1)",
    color: "#ef4444",
    border: "1px solid rgba(239, 68, 68, 0.3)",
    borderRadius: "6px",
    cursor: "pointer",
    fontSize: "13px",
    fontWeight: "600",
    transition: "all 0.3s ease",
  },
  createBtn: {
    padding: "12px 24px",
    backgroundColor: "linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)",
    color: "white",
    border: "none",
    borderRadius: "8px",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
    marginBottom: "30px",
    transition: "all 0.3s ease",
    background: "linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)",
  },
  roomList: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))",
    gap: "20px",
  },
  roomCard: {
    padding: "20px",
    backgroundColor: "rgba(26, 31, 46, 0.6)",
    borderRadius: "10px",
    border: "1px solid rgba(59, 130, 246, 0.2)",
    cursor: "pointer",
    transition: "all 0.3s ease",
  },
  roomHeader: {
    marginBottom: "12px",
  },
  roomTitle: {
    margin: 0,
    fontSize: "16px",
    fontWeight: "600",
    color: "#ffffff",
  },
  roomMeta: {
    color: "#808080",
    fontSize: "13px",
    margin: 0,
  },
  separator: {
    margin: "0 6px",
  },
  empty: {
    textAlign: "center",
    padding: "60px 20px",
    color: "#a0a0a0",
  },
  emptyIcon: {
    fontSize: "48px",
    marginBottom: "16px",
  },
  emptySubtext: {
    fontSize: "13px",
    color: "#707070",
    margin: "8px 0 0 0",
  },
  error: {
    color: "#ff6b6b",
    padding: "12px 16px",
    backgroundColor: "rgba(255, 107, 107, 0.1)",
    borderRadius: "8px",
    marginBottom: "20px",
    fontSize: "13px",
    border: "1px solid rgba(255, 107, 107, 0.3)",
  },
  center: {
    textAlign: "center",
    padding: "40px",
  },
}

export default ChatRoomList
