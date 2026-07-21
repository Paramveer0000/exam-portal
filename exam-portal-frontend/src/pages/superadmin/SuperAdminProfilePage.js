import React from "react";
import SuperAdminSidebar from "../../components/SuperAdminSidebar";
import ProfilePanel from "../../components/ProfilePanel";

const SuperAdminProfilePage = () => {
  return (
    <div style={{ display: "flex" }}>
      <SuperAdminSidebar />
      <div style={{ padding: "1.5rem", flexGrow: 1 }}>
        <h2>Profile</h2>
        <ProfilePanel showRole showLogo />
      </div>
    </div>
  );
};

export default SuperAdminProfilePage;
