import React, { useEffect, useState } from "react";
import "./UserQuizManualPage.css";
import SidebarUser from "../../components/SidebarUser";
import { useParams } from "react-router-dom";
import { Link, useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { Button } from "react-bootstrap";
import Loader from "../../components/Loader";
import { fetchQuizzes } from "../../actions/quizzesActions";

const UserQuizManualPage = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const urlParams = new URLSearchParams(window.location.search);
  const quizId = urlParams.get("quizId");

  const quizzesReducer = useSelector((state) => state.quizzesReducer);
  const [quizzes, setQuizzes] = useState(quizzesReducer.quizzes);
  const [quiz, setQuiz] = useState(
    quizzes.filter((q) => q.quizId == quizId)[0]
  );
  const token = JSON.parse(localStorage.getItem("jwtToken"));

  // Questions a student actually receives: the per-exam limit if set, else the whole pool.
  const questionsCount =
    quiz && quiz.questionsPerExam != null
      ? quiz.questionsPerExam
      : quiz
      ? quiz.numOfQuestions
      : 0;
  // Time limit comes from the school-configured timer (see Add/Update Quiz).
  const timeLimit =
    quiz && quiz.timerEnabled && quiz.timerMinutes
      ? `${quiz.timerMinutes} minute${quiz.timerMinutes === 1 ? "" : "s"}`
      : "No time limit — take as long as you need";

  const startQuizHandler = (quizTitle, quizId) => {
    navigate(`/questions/?quizId=${quizId}&quizTitle=${quizTitle}`);
  };

  useEffect(() => {
    if (quizzes.length == 0) {
      fetchQuizzes(dispatch, token).then((data) => {
        const temp = data.payload;
        setQuizzes(temp);
        setQuiz(temp.filter((q) => q.quizId == quizId)[0]);
      });
    }
  }, []);

  useEffect(() => {
    if (!localStorage.getItem("jwtToken")) navigate("/");
  }, []);

  return (
    <div className="quizManualPage__container">
      <div className="quizManualPage__sidebar">
        <SidebarUser />
      </div>
      {quiz ? (
        <div className="quizManualPage__content">
          <div className="quizManualPage__content--section">
            <h5>Read the instruction of this page carefully</h5>
            <p style={{ color: "grey" }}>One more step to go</p>
          </div>

          <div className="quizManualPage__content--section">
            <h3 style={{ color: "var(--mt-primary)" }}>{quiz.title}</h3>
            <p>{quiz.description}</p>
          </div>

          <hr />

          <div className="mt-card" style={{ padding: "16px 20px", marginBottom: "16px" }}>
            <h3>Important Instructions</h3>
            <ul>
              <li>
                This quiz has <strong>{questionsCount}</strong>{" "}
                {questionsCount === 1 ? "question" : "questions"}.
              </li>
              <li>
                Every question is multiple-choice (MCQ) and carries{" "}
                <strong>1 mark</strong>, so the total is{" "}
                <strong>{questionsCount} marks</strong>.
              </li>
              <li>
                Time limit: <strong>{timeLimit}</strong>.
              </li>
              <li>
                You need <strong>{quiz.passingPercentage}%</strong> to pass.
              </li>
              <li>You can attempt this quiz any number of times.</li>
              <li>
                {quiz.allQuestionsMandatory
                  ? "You must answer every question before you can submit."
                  : "You may leave questions unanswered and submit at any time."}
              </li>
              <li>Your progress bar shows how many you have answered.</li>
            </ul>
          </div>

          <hr />

          <div className="mt-card" style={{ padding: "16px 20px", marginBottom: "16px" }}>
            <h3>Attempting Quiz</h3>
            <ul>
              <li>
                Click <strong>Start Quiz</strong> to begin.
              </li>
              {quiz.timerEnabled && quiz.timerMinutes && (
                <li>
                  The <strong>{quiz.timerMinutes}-minute</strong> timer starts the
                  moment you click Start Quiz, and the quiz auto-submits when it
                  runs out.
                </li>
              )}
              <li>
                You cannot resume this quiz if you leave or are interrupted.
              </li>
              <li>
                Click on <strong>Submit Quiz</strong> button on completion of
                the quiz.
              </li>
              <li>
                Result of the test is generated automatically in PDF format.
              </li>
            </ul>
          </div>

          <Button
            className="quizManualPage__content--button"
            onClick={() => startQuizHandler(quiz.title, quiz.quizId)}
            style={{
              border: "1px solid grey",
              margin: "2px 8px",
            }}
            variant="primary"
          >{`Start Quiz`}</Button>
        </div>
      ) : (
        <Loader />
      )}
    </div>
  );
};

export default UserQuizManualPage;
