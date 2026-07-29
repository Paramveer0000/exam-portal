import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import SidebarUser from "../../components/SidebarUser";
import "./UserQuizzesPage.css";
import { fetchQuizzes } from "../../actions/quizzesActions";
import { Badge, Card, Col, Row } from "react-bootstrap";

const UserQuizzesPage = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const urlParams = new URLSearchParams(window.location.search);
  const subjectId = urlParams.get("subjectId");
  const token = JSON.parse(localStorage.getItem("jwtToken"));

  const quizzesReducer = useSelector((state) => state.quizzesReducer);
  const [quizzes, setQuizzes] = useState(quizzesReducer.quizzes);

  // Quizzes this student has already opened at least once (marked in UserQuestionsPage).
  const user = JSON.parse(localStorage.getItem("user"));
  const userId = user ? user.userId : null;
  const openedQuizIds =
    JSON.parse(localStorage.getItem(`openedQuizzes_${userId}`)) || [];
  const isOpened = (quizId) => openedQuizIds.includes(String(quizId));

  useEffect(() => {
    if (quizzes.length === 0) {
      fetchQuizzes(dispatch, token).then((data) => {
        setQuizzes(data.payload);
      });
    }
  }, []);

  useEffect(() => {
    if (!localStorage.getItem("jwtToken")) navigate("/");
  }, []);

  return (
    <div className="userQuizzesPage__container">
      <div className="userQuizzesPage__sidebar">
        <SidebarUser />
      </div>

      <div className="userQuizzesPage__content">
        {quizzes ? (
          <Row>
            {quizzes.map((q, index) => {
              // Backend already scopes to the student's class; optional subjectId narrows further.
              if ((subjectId && q.subject && String(q.subject.subjectId) === subjectId) || subjectId == null)
                return (
                  <Col
                    key={index}
                    xl={3}
                    lg={4}
                    md={6}
                    sm={6}
                    xs={12}
                    style={{}}
                  >
                    <Card
                      bg="light"
                      text="dark"
                      style={{
                        width: "100%",
                        height: "95%",
                        padding: "5px",
                        margin: "auto",
                        marginTop: "5px",
                        minWidth: "0px",
                        wordWrap: "break-word",
                        border: isOpened(q.quizId)
                          ? "2px solid #1E7A6F"
                          : undefined,
                        boxShadow: isOpened(q.quizId)
                          ? "0 0 6px rgba(68,177,49,0.5)"
                          : undefined,
                      }}
                      className="mb-2 mt-card mt-card-hover"
                    >
                      <Card.Body>
                        <Card.Title>
                          {q.title}
                          {isOpened(q.quizId) && (
                            <Badge bg="info" className="ms-2">
                              Opened
                            </Badge>
                          )}
                        </Card.Title>
                        <Card.Subtitle className="mb-2 text-muted">
                          {q.subject ? q.subject.title : ""}
                        </Card.Subtitle>
                        <Card.Text>{q.description}</Card.Text>
                        <div className="userQuizzesPage__content--ButtonsList">
                          <div
                            className="userQuizzesPage__content--Button"
                            onClick={() =>
                              navigate(`/quizManual?quizId=${q.quizId}`)
                            }
                            style={{cursor: "pointer"}}
                          >
                            {`Start`}
                          </div>

                          <div
                            className="userQuizzesPage__content--Button"
                            style={{ color: "black", backgroundColor: "white" }}
                          >{`${q.numOfQuestions * 2} Minutes`}</div>

                          <div
                            className="userQuizzesPage__content--Button"
                            style={{ color: "black", backgroundColor: "white" }}
                          >{`${q.numOfQuestions} Questions`}</div>

                          <div
                            className="userQuizzesPage__content--Button"
                            style={{ color: "black", backgroundColor: "white" }}
                          >{`Marks : ${q.numOfQuestions * 5}`}</div>
                        </div>
                      </Card.Body>
                    </Card>
                  </Col>
                );
            })}
          </Row>
        ) : (
          <p>No Quizzes Available</p>
        )}
      </div>
    </div>
  );
};

export default UserQuizzesPage;
