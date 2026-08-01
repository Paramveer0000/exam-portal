import React from "react";
import { Navigate } from "react-router-dom";

const getRoleNames = () => {
  try {
    const user = JSON.parse(localStorage.getItem("user"));
    if (!user || !user.roles) return [];
    return user.roles.map((r) => r.roleName);
  } catch (e) {
    return [];
  }
};

export const homePathForRoles = (roles) => {
  if (roles.includes("SUPER_ADMIN")) return "/superadmin";
  if (roles.includes("ADMIN")) return "/adminDashboard";
  return "/profile";
};

const ProtectedRoute = ({ allowedRoles, children }) => {
  const user = (() => {
    try {
      return JSON.parse(localStorage.getItem("user"));
    } catch (e) {
      return null;
    }
  })();
  const roles = getRoleNames();

  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (allowedRoles && !allowedRoles.some((role) => roles.includes(role))) {
    return <Navigate to={homePathForRoles(roles)} replace />;
  }
  return children;
};

export default ProtectedRoute;
