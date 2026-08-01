import React, { useEffect } from "react";
import { useDispatch } from "react-redux";
import { useNavigate } from "react-router-dom";
import Sidebar from "../../components/Sidebar";
import ProfilePanel from "../../components/ProfilePanel";
import "./AdminProfilePage.css";
import { fetchCategories } from "../../actions/categoriesActions";
import { fetchQuizzes } from "../../actions/quizzesActions";

const AdminProfilePage = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();

  useEffect(() => {
    if (!localStorage.getItem("user")) navigate("/");
  }, []);

  useEffect(() => {
    fetchCategories(dispatch);
    fetchQuizzes(dispatch);
  }, [dispatch]);

  return (
    <div className="adminProfilePage__container">
      <div className="adminProfilePage__sidebar">
        <Sidebar />
      </div>
      <div className="adminProfilePage__content" style={{ width: "100%" }}>
        <ProfilePanel showRole showLogo />
      </div>
    </div>
  );
};

export default AdminProfilePage;
