import React from "react";
import Sidebar from "./Sidebar";
import SuperAdminSidebar from "./SuperAdminSidebar";

// Picks the sidebar shell by role so a super admin always sees the same nav
// (SuperAdminSidebar) on shared content pages, instead of the admin Sidebar.
const isSuperAdmin = () => {
  try {
    const user = JSON.parse(localStorage.getItem("user"));
    return !!(user && user.roles && user.roles.some((r) => r.roleName === "SUPER_ADMIN"));
  } catch (e) {
    return false;
  }
};

const RoleSidebar = ({ children }) =>
  isSuperAdmin() ? (
    <SuperAdminSidebar>{children}</SuperAdminSidebar>
  ) : (
    <Sidebar>{children}</Sidebar>
  );

export default RoleSidebar;
