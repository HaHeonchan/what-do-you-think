import { useState } from "react"
import { useNavigate } from "react-router-dom"
import Sidebar from "../components/Sidebar"
import ChatRoomListPanel from "../components/ChatRoomListPanel"
import ChatRoomDetailPanel from "../components/ChatRoomDetailPanel"
import CreateChatRoom from "./CreateChatRoom"

const MainLayout = () => {
  const [selectedRoomId, setSelectedRoomId] = useState(null)
  const [showCreateRoom, setShowCreateRoom] = useState(false)
  const navigate = useNavigate()

  const handleRoomSelect = (roomId) => {
    setSelectedRoomId(roomId)
    setShowCreateRoom(false)
  }

  const handleCreateRoom = () => {
    setShowCreateRoom(true)
    setSelectedRoomId(null)
  }

  const handleBackToList = () => {
    setShowCreateRoom(false)
    setSelectedRoomId(null)
  }

  return (
    <div style={styles.container}>
      <Sidebar />
      <ChatRoomListPanel
        onRoomSelect={handleRoomSelect}
        selectedRoomId={selectedRoomId}
        onCreateRoom={handleCreateRoom}
      />
      <div style={styles.detailArea}>
        {showCreateRoom ? (
          <div style={styles.createRoomWrapper}>
            <CreateChatRoom onSuccess={(roomId) => {
              if (roomId) {
                setShowCreateRoom(false)
                setSelectedRoomId(roomId)
              } else {
                setShowCreateRoom(false)
              }
            }} />
          </div>
        ) : selectedRoomId ? (
          <ChatRoomDetailPanel key={selectedRoomId} roomId={selectedRoomId} />
        ) : (
          <div style={styles.emptyState}>
            <div style={styles.emptyIcon}>💬</div>
            <h2 style={styles.emptyTitle}>Nothing here</h2>
            <p style={styles.emptyText}>
              Please select or create the new channels.
            </p>
          </div>
        )}
      </div>
    </div>
  )
}

const styles = {
  container: {
    display: "flex",
    height: "100vh",
    width: "100vw",
    backgroundColor: "#374151", // gray-700
    overflow: "hidden",
  },
  detailArea: {
    flex: 1,
    height: "100vh",
    overflow: "hidden",
  },
  createRoomWrapper: {
    height: "100%",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    padding: "20px",
  },
  emptyState: {
    flex: 1,
    height: "100%",
    backgroundColor: "#374151", // gray-700
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    justifyContent: "center",
  },
  emptyIcon: {
    fontSize: "80px",
    marginBottom: "16px",
  },
  emptyTitle: {
    fontSize: "24px",
    fontWeight: "bold",
    color: "#ffffff",
    marginBottom: "8px",
  },
  emptyText: {
    color: "#d1d5db", // gray-300
  },
}

export default MainLayout

