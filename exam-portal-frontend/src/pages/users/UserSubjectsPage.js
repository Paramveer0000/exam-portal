import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Card, Col, Row } from "react-bootstrap";
import SidebarUser from "../../components/SidebarUser";
import subjectsServices from "../../services/subjectsServices";
import "./UserQuizzesPage.css";

const UserSubjectsPage = () => {
  const navigate = useNavigate();
  const token = JSON.parse(localStorage.getItem("jwtToken"));
  const [subjects, setSubjects] = useState([]);

  useEffect(() => {
    if (!token) {
      navigate("/");
      return;
    }
    subjectsServices.fetchSubjects(token).then(({ data }) => {
      if (Array.isArray(data)) setSubjects(data);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="userQuizzesPage__container">
      <div className="userQuizzesPage__sidebar">
        <SidebarUser />
      </div>
      <div className="userQuizzesPage__content">
        <h2 className="mb-3">My Subjects</h2>
        {subjects.length === 0 ? (
          <p>No subjects in your class yet.</p>
        ) : (
          <Row>
            {subjects.map((s) => (
              <Col key={s.subjectId} xl={3} lg={4} md={6} sm={6} xs={12}>
                <Card
                  bg="light"
                  text="dark"
                  className="mb-2"
                  style={{ cursor: "pointer", height: "95%", marginTop: "5px" }}
                  onClick={() => navigate(`/quizzes?subjectId=${s.subjectId}`)}
                >
                  <Card.Body>
                    <Card.Title>{s.title}</Card.Title>
                    <Card.Text>{s.description}</Card.Text>
                    <div className="userQuizzesPage__content--Button" style={{ cursor: "pointer" }}>
                      View Quizzes
                    </div>
                  </Card.Body>
                </Card>
              </Col>
            ))}
          </Row>
        )}
      </div>
    </div>
  );
};

export default UserSubjectsPage;
