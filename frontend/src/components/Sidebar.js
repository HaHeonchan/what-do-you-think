import React, { useState } from "react"

const Sidebar = () => {
  const [hoveredItem, setHoveredItem] = useState(null)

  const NavItem = ({ title, icon }) => (
    <div
      style={{
        ...styles.navItem,
        ...(hoveredItem === title ? styles.navItemHover : {}),
      }}
      title={title}
      onMouseEnter={() => setHoveredItem(title)}
      onMouseLeave={() => setHoveredItem(null)}
    >
      {icon}
    </div>
  )

  const NoteIcon = (
    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"></path>
      <path d="M14 2v5h5"></path>
      <path d="M16 13H8"></path>
      <path d="M16 17H8"></path>
      <path d="M10 9H8"></path>
    </svg>
  )

  const SettingsIcon = (
    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.08a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73v.18a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.38a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.72v-.51a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"></path>
      <circle cx="12" cy="12" r="3"></circle>
    </svg>
  )

  return (
    <div style={styles.container}>
      <div style={styles.logo}>💬</div>
      <div style={styles.divider}></div>
      <NavItem title="Note" icon={NoteIcon} />
      <NavItem title="Setting" icon={SettingsIcon} />
    </div>
  )
}

const styles = {
  container: {
    width: "80px",
    backgroundColor: "#111827", // gray-900
    height: "100vh",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    padding: "16px 0",
    gap: "16px",
  },
  logo: {
    backgroundColor: "#4b5563", // gray-600
    height: "48px",
    width: "48px",
    borderRadius: "50%",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    fontSize: "20px",
    fontWeight: "bold",
    color: "#ffffff",
  },
  divider: {
    width: "40px",
    borderTop: "1px solid #374151", // gray-700
  },
  navItem: {
    backgroundColor: "#374151", // gray-700
    height: "48px",
    width: "48px",
    borderRadius: "50%",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    color: "#d1d5db", // gray-300
    cursor: "pointer",
    transition: "all 0.2s",
  },
  navItemHover: {
    borderRadius: "12px",
    backgroundColor: "#2563eb", // blue-500
    color: "#ffffff",
  },
}

export default Sidebar

