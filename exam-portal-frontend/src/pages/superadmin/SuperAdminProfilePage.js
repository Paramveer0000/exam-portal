import React from "react";
import SuperAdminSidebar from "../../components/SuperAdminSidebar";
import ProfilePanel from "../../components/ProfilePanel";

const SuperAdminProfilePage = () => {
  return (
    <div style={{ display: "flex" }}>
      <SuperAdminSidebar />
      <div className="mt-page">
        <h2 style={{ color: "var(--mt-primary)" }}>Profile</h2>
        <ProfilePanel showRole showLogo />
      </div>
    </div>
  );
};

export default SuperAdminProfilePage;
