import React, { useEffect } from "react";
import { useDispatch } from "react-redux";
import { useNavigate } from "react-router-dom";
import { fetchCategories } from "../../actions/categoriesActions";
import { fetchQuizzes } from "../../actions/quizzesActions";
import SidebarUser from "../../components/SidebarUser";
import ProfilePanel from "../../components/ProfilePanel";
import "./UserProfilePage.css";

const UserProfilePage = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const token = JSON.parse(localStorage.getItem("jwtToken"));

  useEffect(() => {
    fetchCategories(dispatch, token);
    fetchQuizzes(dispatch, token);
  }, [dispatch]);

  useEffect(() => {
    if (!localStorage.getItem("jwtToken")) navigate("/");
  }, []);

  return (
    <div className="userProfilePage__container">
      <div className="userProfilePage__sidebar">
        <SidebarUser />
      </div>
      <div className="userProfilePage__content" style={{ width: "100%" }}>
        {/* Students don't see their role. */}
        <ProfilePanel showRole={false} />
      </div>
    </div>
  );
};

export default UserProfilePage;
