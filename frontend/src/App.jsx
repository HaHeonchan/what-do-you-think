import React, { useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Sidebar from './components/layout/Sidebar.jsx';
import Notes from './pages/Notes.jsx';
import NewNote from './pages/NewNote.jsx';
import Feature2 from './pages/Feature2.jsx';
import AnotherNote from './pages/AnotherNote.jsx';
import Feature3 from './pages/Feature3.jsx';
import MeetingSetup from './pages/MeetingSetup.jsx';
import Settings from './pages/Settings.jsx';

import './App.css';

function App() {
  const [isSidebarOpen, setSidebarOpen] = useState(true);

  const toggleSidebar = () => {
    setSidebarOpen(!isSidebarOpen);
  };

  return (
    <Router>
      <div className="app-container">
        <Sidebar isOpen={isSidebarOpen} toggle={toggleSidebar} />
        <main className={`main-content ${isSidebarOpen ? 'sidebar-open' : 'sidebar-closed'}`}>
          <Routes>
            {/* 기본 접속 시 /notes 로 자동 이동 */}
            <Route path="/" element={<Navigate to="/notes" replace />} />
            <Route path="/notes" element={<Notes />} />
            <Route path="/notes/new" element={<NewNote />} />
            <Route path="/meeting/setup" element={<MeetingSetup />} />
            <Route path="/feature2" element={<Feature2 />} />
            <Route path="/another-note" element={<AnotherNote />} />
            <Route path="/feature3" element={<Feature3 />} />
            <Route path="/settings" element={<Settings />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;