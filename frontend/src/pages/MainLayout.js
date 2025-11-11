import { useState, useRef } from "react"
import Sidebar from "../components/Sidebar"
import ChatRoomListPanel from "../components/ChatRoomListPanel"
import ChatRoomDetailPanel from "../components/ChatRoomDetailPanel"

const MainLayout = () => {
  const [selectedRoomId, setSelectedRoomId] = useState(null)
  const chatRoomListRef = useRef(null)

  const handleRoomSelect = (roomId) => {
    setSelectedRoomId(roomId)
  }

  const handleRoomDeleted = () => {
    setSelectedRoomId(null)
    // ChatRoomListPanel의 loadChatRooms 호출
    if (chatRoomListRef.current) {
      chatRoomListRef.current.refreshRooms()
    }
  }

  return (
    <div style={styles.container}>
      <Sidebar />
      <ChatRoomListPanel
        ref={chatRoomListRef}
        onRoomSelect={handleRoomSelect}
        selectedRoomId={selectedRoomId}
      />
      <div style={styles.detailArea}>
        {selectedRoomId ? (
          <ChatRoomDetailPanel key={selectedRoomId} roomId={selectedRoomId} onRoomDeleted={handleRoomDeleted} />
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

