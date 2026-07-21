import React, { useState } from "react";
import { FaBars, FaUsers, FaRegChartBar, FaUserAlt, FaRobot } from "react-icons/fa";
import { TbLayoutGrid, TbReport } from "react-icons/tb";
import { MdQuiz } from "react-icons/md";
import { NavLink } from "react-router-dom";
import "./Sidebar.css";

const SuperAdminSidebar = ({ children }) => {
  // Persist expanded/collapsed state across navigation (each page mounts its own).
  const [isOpen, setIsOpen] = useState(
    () => localStorage.getItem("sidebarOpen") === "true"
  );
  const toggle = () => {
    const next = !isOpen;
    setIsOpen(next);
    localStorage.setItem("sidebarOpen", String(next));
  };

  const menuItem = [
    {
      path: "/superadmin",
      name: "Dashboard",
      icon: <FaRegChartBar />,
      end: true,
    },
    {
      path: "/superadmin/admins",
      name: "Schools",
      icon: <FaUsers />,
    },
    {
      path: "/adminCategories",
      name: "Classes",
      icon: <TbLayoutGrid />,
    },
    {
      path: "/adminQuizzes",
      name: "Subjects",
      icon: <MdQuiz />,
    },
    {
      path: "/superadmin/results",
      name: "All Results",
      icon: <TbReport />,
    },
    {
      path: "/superadmin/ai-settings",
      name: "Setting",
      icon: <FaRobot />,
    },
    {
      path: "/superadmin/profile",
      name: "Profile",
      icon: <FaUserAlt />,
    },
  ];

  return (
    <div
      className="container"
      style={{ display: "flex", width: "auto", margin: "0px", padding: "0px" }}
    >
      <div style={{ width: isOpen ? "12em" : "3em" }} className="sidebar">
        <div className="top_section">
          <h1 style={{ display: isOpen ? "block" : "none" }} className="logo">
            Logo
          </h1>
          <div style={{ marginLeft: isOpen ? "50px" : "0px" }} className="bars">
            <FaBars onClick={toggle} />
          </div>
        </div>
        {menuItem.map((item, index) => (
          <NavLink
            to={item.path}
            key={index}
            end={item.end}
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

export default SuperAdminSidebar;
