import React, { useState } from "react";
import "./Sidebar.css";
import { FaBars, FaUserAlt } from "react-icons/fa";
import { MdQuiz } from "react-icons/md";
import { NavLink } from "react-router-dom";
import { TbReport } from "react-icons/tb";

// Static items only — classes are browsed from "All Quizzes", not listed
// individually in the sidebar.
const MENU_ITEMS = [
  {
    path: "/profile",
    name: "Profile",
    icon: <FaUserAlt />,
  },
  {
    path: "/quizResults",
    name: "Report Card",
    icon: <TbReport />,
  },
  {
    path: "/quizzes",
    name: "All Quizzes",
    icon: <MdQuiz />,
  },
];

const SidebarUser = ({ children }) => {
  const menuItems = MENU_ITEMS;

  // Persist expanded/collapsed state across navigation.
  const [isOpen, setIsOpen] = useState(
    () => localStorage.getItem("sidebarOpen") === "true"
  );
  const toggle = () => {
    const next = !isOpen;
    setIsOpen(next);
    localStorage.setItem("sidebarOpen", String(next));
  };

  return (
    <div
      className="container"
      style={{ display: "flex", width: "auto", margin: "0px", padding: "0px" }}
    >
      <div style={{ width: isOpen ? "12em" : "3em" }} className="sidebar">
        <div className="top_section">
          {/* <h1 style={{ display: isOpen ? "block" : "none" }} className="logo">
            Logo
          </h1> */}
          <div style={{ marginLeft: isOpen ? "50px" : "0px" }} className="bars">
            <FaBars onClick={toggle} />
          </div>
        </div>
        {menuItems.map((item, index) => (
          <NavLink
            to={item.path}
            key={index}
            className="sidemenulink"
            activeclassname="sidemenulink-active"
          >
            <div className="icon">{item.icon}</div>
            <div
              style={{ display: isOpen ? "block" : "none" }}
              className="link_text"
            >
              {item.name}
            </div>
          </NavLink>
        ))}
      </div>
      <main>{children}</main>
    </div>
  );
};

export default SidebarUser;
