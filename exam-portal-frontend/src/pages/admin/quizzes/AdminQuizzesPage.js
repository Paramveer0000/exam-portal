import React, { useEffect, useState } from "react";
import "./AdminQuizzesPage.css";
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { Badge, Button, Card, ListGroup } from "react-bootstrap";
import { BsJournalBookmark } from "react-icons/bs";
import Message from "../../../components/Message";
import RoleSidebar from "../../../components/RoleSidebar";
import Loader from "../../../components/Loader";
import { deleteQuiz, fetchQuizzes } from "../../../actions/quizzesActions";
import * as quizzesConstants from "../../../constants/quizzesConstants";
import categoriesServices from "../../../services/categoriesServices";
import swal from "sweetalert";

const AdminQuizzesPage = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const urlParams = new URLSearchParams(window.location.search);
  const catId = urlParams.get("catId");
  const token = JSON.parse(localStorage.getItem("jwtToken"));

  const quizzesReducer = useSelector((state) => state.quizzesReducer);
  const [quizzes, setQuizzes] = useState(quizzesReducer.quizzes);
  const [classesById, setClassesById] = useState({});

  const addNewQuizHandler = () => {
    navigate("/adminAddQuiz");
  };
  const deleteQuizHandler = (quiz) => {
    swal({
      title: "Are you sure?",
      text: "Once deleted, you will not be able to recover this quiz!",
      icon: "warning",
      buttons: true,
      dangerMode: true,
    }).then((willDelete) => {
      if (willDelete) {
        deleteQuiz(dispatch, quiz.quizId, token).then((data) => {
          if (data.type === quizzesConstants.DELETE_QUIZ_SUCCESS) {
            swal(
              "Quiz Deleted!",
              `${quiz.title} succesfully deleted`,
              "success"
            );
          } else {
            swal("Quiz Not Deleted!", `${quiz.title} not deleted`, "error");
          }
        });
      } else {
        swal(`${quiz.title} is safe`);
      }
    });
  };
  const updateQuizHandler = (quizTitle, quizId) => {
    navigate(`/adminUpdateQuiz/${quizId}`);
  };

  const questionsHandler = (quizTitle, quizId) => {
    navigate(`/adminQuestions/?quizId=${quizId}&quizTitle=${quizTitle}`);
  };

  useEffect(() => {
    if (quizzes.length === 0) {
      fetchQuizzes(dispatch, token).then((data) => {
        setQuizzes(data.payload);
      });
    }
  }, []);

  useEffect(() => {
    if (!localStorage.getItem("jwtToken")) navigate("/");
    categoriesServices.fetchCategories(token).then((data) => {
      if (Array.isArray(data)) {
        const map = {};
        data.forEach((c) => (map[c.catId] = c.title));
        setClassesById(map);
      }
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Group quizzes by their class; optionally filter to one class (catId param).
  const buildClassGroups = () => {
    const visible = (quizzes || []).filter(
      (q) => catId == null || (q.category && q.category.catId == catId)
    );
    const groups = [];
    const byId = {};
    visible.forEach((quiz) => {
      const cid = quiz.category ? quiz.category.catId : "unassigned";
      if (!byId[cid]) {
        byId[cid] = {
          catId: cid,
          title: quiz.category ? classesById[cid] || `Class #${cid}` : "Unassigned",
          quizzes: [],
        };
        groups.push(byId[cid]);
      }
      byId[cid].quizzes.push(quiz);
    });
    return groups;
  };
  const classGroups = buildClassGroups();

  const renderQuizCard = (quiz) => (
    <Card className="mt-card mb-2" key={quiz.quizId}>
      <ListGroup variant="flush">
      <ListGroup.Item className="align-items-start">
        <div className="ms-2 me-auto">
          <div className="fw-bold d-flex align-items-center gap-2">
            <BsJournalBookmark style={{ color: "var(--mt-secondary)" }} />
            {quiz.title}
            <Badge bg={quiz.isActive ? "success" : "secondary"}>
              {quiz.isActive ? "Published" : "Draft"}
            </Badge>
          </div>
          {<p className="my-3">{quiz.description}</p>}
          <div className="adminQuizzesPage__content--ButtonsList">
            <div
              onClick={() => questionsHandler(quiz.title, quiz.quizId)}
              style={{
                border: "1px solid grey",
                width: "100px",
                height: "35px",
                padding: "1px",
                textAlign: "center",
                borderRadius: "5px",
                color: "white",
                backgroundColor: "#1E7A6F",
                margin: "0px 4px",
              }}
            >{`Questions`}</div>
            <div
              style={{
                border: "1px solid grey",
                width: "100px",
                padding: "1px",
                textAlign: "center",
                borderRadius: "5px",
                height: "35px",
                margin: "0px 4px",
              }}
            >{`Marks : ${quiz.numOfQuestions * 5}`}</div>
            <div
              style={{
                border: "1px solid grey",
                width: "100px",
                padding: "1px",
                textAlign: "center",
                borderRadius: "5px",
                height: "35px",
                margin: "0px 4px",
              }}
            >{`${quiz.numOfQuestions} Questions`}</div>
            <div
              onClick={() => updateQuizHandler(quiz.title, quiz.quizId)}
              style={{
                border: "1px solid grey",
                color: "white",
                backgroundColor: "#1E7A6F",
                width: "100px",
                padding: "1px",
                textAlign: "center",
                borderRadius: "5px",
                height: "35px",
                margin: "0px 4px",
              }}
            >{`Update`}</div>
            <div
              onClick={() => deleteQuizHandler(quiz)}
              style={{
                border: "1px solid grey",
                color: "white",
                backgroundColor: "#ff0b0bdb",
                width: "100px",
                padding: "2px",
                textAlign: "center",
                borderRadius: "5px",
                height: "35px",
                margin: "0px 4px",
              }}
            >{`Delete`}</div>
          </div>
        </div>
      </ListGroup.Item>
      </ListGroup>
    </Card>
  );

  return (
    <div className="adminQuizzesPage__container">
      <div className="adminQuizzesPage__sidebar">
        <RoleSidebar />
      </div>
      <div className="mt-page">
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <h2 style={{ color: "var(--mt-primary)" }}>Quizzes</h2>
          <Button variant="primary" onClick={addNewQuizHandler}>
            Add Quiz
          </Button>
        </div>
        {quizzes ? (
          classGroups.length === 0 ? (
            <Message>No quizzes are present. Try adding some quizzes.</Message>
          ) : (
            classGroups.map((group) => (
              <div
                key={group.catId}
                className="mt-card"
                style={{
                  padding: "12px 16px",
                  marginBottom: "20px",
                  marginTop: "12px",
                }}
              >
                <h4 style={{ marginBottom: "12px" }}>
                  {group.title}
                  <span
                    style={{
                      color: "grey",
                      fontSize: "0.9rem",
                      marginLeft: "8px",
                    }}
                  >
                    ({group.quizzes.length}{" "}
                    {group.quizzes.length === 1 ? "quiz" : "quizzes"})
                  </span>
                </h4>
                {group.quizzes.map((quiz) => renderQuizCard(quiz))}
              </div>
            ))
          )
        ) : (
          <Loader />
        )}
      </div>
    </div>
  );
};

export default AdminQuizzesPage;
