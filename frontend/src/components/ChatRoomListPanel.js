import { useState, useEffect } from "react"
import { useAuth } from "../contexts/AuthContext"
import { chatRoomAPI } from "../services/api"

const ChatRoomListPanel = ({ onRoomSelect, selectedRoomId }) => {
  const [chatRooms, setChatRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState("")
  const { user, logout } = useAuth()

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

  const handleCreateRoom = async () => {
    setCreating(true)
    setError("")
    try {
      // 빈 대화방 생성
      const res = await chatRoomAPI.create({ title: null })
      const newRoom = res.data
      
      // 목록 새로고침
      await loadChatRooms()
      
      // 새로 생성된 대화방 선택
      if (onRoomSelect) {
        onRoomSelect(newRoom.id)
      }
    } catch (err) {
      setError(err.response?.data?.error || "대화방 생성에 실패했습니다.")
    } finally {
      setCreating(false)
    }
  }

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <div style={styles.headerTop}>
          <span style={styles.username}>@{user?.username}</span>
          <button onClick={logout} style={styles.logoutBtn}>
            로그아웃
          </button>
        </div>
        <button 
          onClick={handleCreateRoom} 
          disabled={creating}
          style={{
            ...styles.createBtn,
            ...(creating ? styles.createBtnDisabled : {}),
          }}
        >
          {creating ? "생성 중..." : "+ 새 대화방"}
        </button>
      </div>

      {error && (
        <div style={styles.error}>
          {error}
        </div>
      )}

      <div style={styles.roomList}>
        {loading && <div style={styles.loading}>로딩 중...</div>}

        {!loading && chatRooms.length === 0 && (
          <div style={styles.empty}>
            <div style={styles.emptyIcon}>📭</div>
            <p style={styles.emptyText}>대화방이 없습니다</p>
          </div>
        )}

        {!loading && chatRooms.map((room) => {
          const isSelected = room.id === selectedRoomId
          return (
            <div
              key={room.id}
              style={{
                ...styles.roomCard,
                ...(isSelected ? styles.roomCardSelected : {}),
              }}
              onClick={() => onRoomSelect(room.id)}
            >
              <h3 style={styles.roomTitle}>
                {room.title || "제목 없음"}
              </h3>
              <p style={styles.roomMeta}>
                📅 {new Date(room.updatedAt).toLocaleDateString()}
                <span style={styles.separator}>•</span>
                💬 {room.chats?.length || 0}개
              </p>
            </div>
          )
        })}
      </div>
    </div>
  )
}

const styles = {
  container: {
    width: "320px", // w-80
    backgroundColor: "#1f2937", // gray-800
    height: "100vh",
    display: "flex",
    flexDirection: "column",
    overflow: "hidden",
  },
  header: {
    padding: "16px",
    borderBottom: "1px solid #374151", // gray-700
  },
  headerTop: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: "16px",
  },
  username: {
    fontSize: "14px",
    fontWeight: "600",
    color: "#d1d5db", // gray-300
  },
  logoutBtn: {
    fontSize: "12px",
    color: "#f87171", // red-400
    background: "none",
    border: "none",
    cursor: "pointer",
    padding: 0,
  },
  createBtn: {
    width: "100%",
    backgroundColor: "#2563eb", // blue-600
    color: "white",
    padding: "8px",
    borderRadius: "6px",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
    border: "none",
    transition: "background-color 0.2s",
  },
  createBtnDisabled: {
    backgroundColor: "#1e40af", // blue-800
    cursor: "not-allowed",
    opacity: 0.7,
  },
  error: {
    padding: "16px",
    margin: "16px",
    backgroundColor: "#991b1b", // red-800
    border: "1px solid #dc2626", // red-600
    color: "#fecaca", // red-100
    borderRadius: "6px",
    fontSize: "14px",
  },
  roomList: {
    flex: 1,
    overflowY: "auto",
    padding: "8px",
    display: "flex",
    flexDirection: "column",
    gap: "4px",
  },
  loading: {
    padding: "16px",
    textAlign: "center",
    color: "#9ca3af", // gray-400
  },
  empty: {
    padding: "16px",
    textAlign: "center",
    color: "#9ca3af", // gray-400
  },
  emptyIcon: {
    fontSize: "48px",
    marginBottom: "8px",
  },
  emptyText: {
    fontSize: "14px",
  },
  roomCard: {
    padding: "12px",
    borderRadius: "6px",
    cursor: "pointer",
    transition: "all 0.15s",
    color: "#d1d5db", // gray-300
  },
  roomCardSelected: {
    backgroundColor: "#2563eb", // blue-600
    color: "white",
  },
  roomTitle: {
    fontSize: "14px",
    fontWeight: "600",
    margin: "0 0 4px 0",
    overflow: "hidden",
    textOverflow: "ellipsis",
    whiteSpace: "nowrap",
  },
  roomMeta: {
    fontSize: "12px",
    margin: 0,
    opacity: 0.7,
  },
  separator: {
    margin: "0 4px",
  },
}

export default ChatRoomListPanel

