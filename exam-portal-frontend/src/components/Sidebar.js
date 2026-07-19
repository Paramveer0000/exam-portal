import React, { useState } from "react";
import { FaBars, FaUserAlt, FaRegChartBar, FaUserGraduate } from "react-icons/fa";
import { TbLayoutGrid, TbReport } from "react-icons/tb";
import { MdQuiz } from "react-icons/md";
import { NavLink } from "react-router-dom";
import "./Sidebar.css";

const Sidebar = ({ children }) => {
  // Persist expanded/collapsed state so it survives navigation (each page mounts
  // its own Sidebar instance, which would otherwise reset to collapsed).
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
      path: "/adminProfile",
      name: "Profile",
      icon: <FaUserAlt />,
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
      path: "/adminStudents",
      name: "Students",
      icon: <FaUserGraduate />,
    },
    {
      path: "/adminallResult",
      name: "All Result",
      icon: <TbReport/>,
    },
    {
      path: "/adminReports",
      name: "Reports",
      icon: <FaRegChartBar />,
    },
  ];

  // Show a link back to the Super Admin dashboard for super admins.
  const roleNames = (() => {
    try {
      const user = JSON.parse(localStorage.getItem("user"));
      return user && user.roles ? user.roles.map((r) => r.roleName) : [];
    } catch (e) {
      return [];
    }
  })();
  if (roleNames.includes("SUPER_ADMIN")) {
    menuItem.push({
      path: "/superadmin",
      name: "Super Admin",
      icon: <FaUserAlt />,
    });
  }
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

export default Sidebar;
