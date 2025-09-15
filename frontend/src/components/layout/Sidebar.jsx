import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { FaBars, FaStickyNote, FaCog, FaChevronDown, FaRegClone, FaBook } from 'react-icons/fa';

function Sidebar({ isOpen, toggle }) {
  // 1. State를 객체 형태로 변경합니다.
  // 각 메뉴의 이름을 key로, 열림 여부를 boolean 값으로 관리합니다.
  const [openMenus, setOpenMenus] = useState({
    notes: true, // 'notes' 메뉴는 기본적으로 열어둡니다.
    'another-note': false,
  });

  // 2. 메뉴 클릭 핸들러 로직을 수정합니다.
  const handleMenuClick = (menuName) => {
    // 사이드바가 닫혀있을 경우, 사이드바와 해당 메뉴를 엽니다.
    if (!isOpen) {
      toggle();
      setOpenMenus(prevOpenMenus => ({
        ...prevOpenMenus,
        [menuName]: true,
      }));
    } else {
      // 사이드바가 열려있을 경우, 다른 메뉴의 상태는 유지하고 클릭된 메뉴의 상태만 변경합니다.
      setOpenMenus(prevOpenMenus => ({
        ...prevOpenMenus, // 기존의 열림 상태를 모두 복사하고
        [menuName]: !prevOpenMenus[menuName], // 클릭된 메뉴의 boolean 값만 반전시킵니다.
      }));
    }
  };

  return (
    <nav className={`sidebar ${isOpen ? 'open' : 'closed'}`}>
      <ul className="sidebar-menu">
        <li className="menu-item">
          <button className="menu-button" onClick={toggle}>
            <span className="menu-icon"><FaBars /></span>
            <span className="menu-text">메뉴</span>
          </button>
        </li>

        {/* 노트 기능 */}
        <li className="menu-item">
          <div onClick={() => handleMenuClick('notes')}>
            <span className="menu-icon"><FaStickyNote /></span>
            <span className="menu-text">회의</span>
            {isOpen && <span className="menu-icon" style={{ marginLeft: 'auto' }}><FaChevronDown /></span>}
          </div>
          {/* 3. className 조건을 객체의 키 값으로 확인하도록 변경합니다. */}
          <ul className={`sub-menu ${openMenus.notes && isOpen ? 'open' : ''}`}>
            <li><Link to="/meeting/setup"><span className="menu-text">새 회의 시작</span></Link></li>
            <li><Link to="/meeting"><span className="menu-text">회의 목록</span></Link></li>
          </ul>
        </li>

        {/* 다른 노트 기능 */}
        {/* <li className="menu-item">
          <div onClick={() => handleMenuClick('another-note')}>
            <span className="menu-icon"><FaBook /></span>
            <span className="menu-text">다른 노트 기능</span>
            {isOpen && <span className="menu-icon" style={{ marginLeft: 'auto' }}><FaChevronDown /></span>}
          </div>
          
          <ul className={`sub-menu ${openMenus['another-note'] && isOpen ? 'open' : ''}`}>
            <li><Link to="/another-note"><span className="menu-text">하위 메뉴 1</span></Link></li>
          </ul>
        </li> */}

        {/* 기능 3 */}
        <li className="menu-item">
          <Link to="/feature3">
            <span className="menu-icon"><FaRegClone /></span>
            <span className="menu-text">기능 3</span>
          </Link>
        </li>

        {/* 설정 버튼 */}
        <li className="menu-item settings-item">
          <Link to="/settings">
            <span className="menu-icon"><FaCog /></span>
            <span className="menu-text">설정</span>
          </Link>
        </li>
      </ul>
    </nav>
  );
}

export default Sidebar;