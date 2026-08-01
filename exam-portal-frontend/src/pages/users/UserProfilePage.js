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
  useEffect(() => {
    fetchCategories(dispatch);
    fetchQuizzes(dispatch);
  }, [dispatch]);

  useEffect(() => {
    if (!localStorage.getItem("user")) navigate("/");
  }, []);

  return (
    <div className="userProfilePage__container">
      <div className="userProfilePage__sidebar">
        <SidebarUser />
      </div>
      <div className="userProfilePage__content" style={{ width: "100%" }}>
        {/* Students don't see their role, but do see/change their school. */}
        <ProfilePanel showRole={false} showSchool={true} />
      </div>
    </div>
  );
};

export default UserProfilePage;
