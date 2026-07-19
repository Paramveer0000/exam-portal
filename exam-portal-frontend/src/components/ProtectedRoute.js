import React from "react";
import { Navigate } from "react-router-dom";

// Reads the persisted user and returns their role names.
const getRoleNames = () => {
  try {
    const user = JSON.parse(localStorage.getItem("user"));
    if (!user || !user.roles) return [];
    return user.roles.map((r) => r.roleName);
  } catch (e) {
    return [];
  }
};

// Where a logged-in user belongs when they hit a route they're not allowed on.
export const homePathForRoles = (roles) => {
  if (roles.includes("SUPER_ADMIN")) return "/superadmin";
  if (roles.includes("ADMIN")) return "/adminProfile";
  return "/profile";
};

/**
 * Guards a route by authentication and (optionally) role. Enforcement is still
 * done on the backend; this only keeps the UI honest.
 */
const ProtectedRoute = ({ allowedRoles, children }) => {
  const token = localStorage.getItem("jwtToken");
  const roles = getRoleNames();

  if (!token) {
    return <Navigate to="/login" replace />;
  }
  if (allowedRoles && !allowedRoles.some((role) => roles.includes(role))) {
    return <Navigate to={homePathForRoles(roles)} replace />;
  }
  return children;
};

export default ProtectedRoute;
