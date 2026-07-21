import React from "react";
import { Navigate } from "react-router-dom";
import { isAcademicsComplete, isBasicComplete } from "../pages/users/OnboardingPage";

/**
 * Hard gate for student routes: until basic info AND academic details are
 * filled in, every student route redirects to /onboarding. Wraps userRoute()
 * in App.js — never wraps /onboarding itself (that would loop).
 */
const RequireStudentOnboarding = ({ children }) => {
  let user = null;
  try {
    user = JSON.parse(localStorage.getItem("user"));
  } catch (e) {
    // ignore malformed storage; treated as incomplete below
  }

  if (!isBasicComplete(user) || !isAcademicsComplete(user)) {
    return <Navigate to="/onboarding" replace />;
  }
  return children;
};

export default RequireStudentOnboarding;
