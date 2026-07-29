import React, { useEffect, useRef, useState } from "react";
import "./UserQuestionsPage.css";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { Button, ProgressBar } from "react-bootstrap";
import { fetchExamQuestions } from "../../actions/questionsActions";
import Question from "../../components/Question";
import Loader from "../../components/Loader";
import swal from "sweetalert";
import * as quizResultConstants from "../../constants/quizResultConstants";
import { submitQuiz } from "../../actions/quizResultActions";
import { fetchQuizzes } from "../../actions/quizzesActions";
import ReactSpinnerTimer from "react-spinner-timer";

const UserQuestionsPage = () => {
  Number.prototype.zeroPad = function () {
    return ("0" + this).slice(-2);
  };

  const navigate = useNavigate();
  const dispatch = useDispatch();
  const urlParams = new URLSearchParams(window.location.search);
  const quizId = urlParams.get("quizId");
  const quizTitle = urlParams.get("quizTitle");
  const quizzesReducer = useSelector((state) => state.quizzesReducer);
  const [quizzes, setQuizzes] = useState(quizzesReducer.quizzes);
  const [quiz, setQuiz] = useState(
    quizzes.filter((q) => q.quizId == quizId)[0]
  );
  const questionsReducer = useSelector((state) => state.questionsReducer);
  const [questions, setQuestions] = useState(questionsReducer.questions);
  const token = JSON.parse(localStorage.getItem("jwtToken"));
  const user = JSON.parse(localStorage.getItem("user"));
  const userId = user ? user.userId : null;
  const [timeRemaining, setTimeRemaining] = useState(questions.length * 2 * 60);
  const [answeredCount, setAnsweredCount] = useState(0);
  const QUESTIONS_PER_PAGE = 6;
  const [page, setPage] = useState(0);
  const totalPages = Math.max(1, Math.ceil(questions.length / QUESTIONS_PER_PAGE));

  // Count how many of THIS quiz's questions have a saved answer.
  const recomputeAnswered = () => {
    const ans = JSON.parse(localStorage.getItem("answers")) || {};
    setAnsweredCount(questions.filter((q) => ans[q.quesId] != null).length);
  };
  const intervalRef = useRef(null);
  const submittedRef = useRef(false);
  const startedRef = useRef(false);

  const stopTimer = () => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  };

  // Start the countdown ONCE, only after both the questions AND the quiz
  // config have loaded (quiz can arrive slightly later than questions).
  // If the school has NOT enabled a timer, there is no countdown at all —
  // no auto-submit, no fallback timer.
  useEffect(() => {
    if (startedRef.current) return;
    if (!questions || questions.length === 0) return;
    if (quiz === undefined) return; // wait for quiz config to resolve
    startedRef.current = true;

    if (!quiz || !quiz.timerEnabled || !quiz.timerMinutes) {
      setTimeRemaining(0); // no time limit
      return;
    }

    setTimeRemaining(quiz.timerMinutes * 60);
    intervalRef.current = setInterval(() => {
      setTimeRemaining((prev) => {
        if (prev <= 1) {
          stopTimer();
          if (!submittedRef.current) {
            submittedRef.current = true;
            submitQuizHandler(true);
          }
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return stopTimer;
  }, [questions, quiz]);

  const submitQuizHandler = (isTimesUp = false) => {
    const answers = JSON.parse(localStorage.getItem("answers"));
    if (isTimesUp) {
      submitQuiz(dispatch, quizId, answers, token).then((data) => {
        if (data.type === quizResultConstants.ADD_QUIZ_RESULT_SUCCESS) {
          swal(
            "Quiz Submitted!",
            `You scored ${data.payload.totalObtainedMarks} marks in ${quizTitle} quiz.`,
            "success"
          );
          return navigate("/quizResults");
        } else {
          swal(
            "Quiz not Submitted!",
            `${quizTitle} is still active. You can modify your answers`,
            "info"
          );
          return navigate("/quizResults");
        }
      });
    } else {
      // When the school marks every question mandatory, block submit until all answered.
      if (
        quiz &&
        quiz.allQuestionsMandatory &&
        answeredCount < questions.length
      ) {
        swal(
          "Answer all questions",
          `This quiz requires every question answered. You have answered ${answeredCount} of ${questions.length}.`,
          "warning"
        );
        return;
      }
      swal({
        title: "Are you sure?",
        text: "Once submitted, you will not be able to modify your answers!",
        icon: "warning",
        buttons: true,
        dangerMode: true,
      }).then((willSubmit) => {
        if (willSubmit) {
          submittedRef.current = true;
          stopTimer();
          submitQuiz(dispatch, quizId, answers, token).then((data) => {
            if (data.type === quizResultConstants.ADD_QUIZ_RESULT_SUCCESS) {
              swal(
                "Quiz Submitted!",
                `You scored ${data.payload.totalObtainedMarks} marks in ${quizTitle} quiz.`,
                "success"
              );
              return navigate("/quizResults");
            } else {
              swal(
                "Quiz not Submitted!",
                `${quizTitle} is still active. You can modify your answers`,
                "info"
              );
              return navigate("/quizResults");
            }
          });
        }
      });
    }
    stopTimer();
  };

  useEffect(() => {
    if (!localStorage.getItem("jwtToken")) navigate("/");
  }, []);

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
    // Fresh attempt: clear any answers left over from a previous quiz.
    localStorage.removeItem("answers");
    setAnsweredCount(0);
    // Remember that this student has opened this quiz (used to highlight it in the panel).
    if (userId != null && quizId != null) {
      const key = `openedQuizzes_${userId}`;
      const opened = JSON.parse(localStorage.getItem(key)) || [];
      if (!opened.includes(String(quizId))) {
        opened.push(String(quizId));
        localStorage.setItem(key, JSON.stringify(opened));
      }
    }
    fetchExamQuestions(dispatch, quizId, token).then((data) => {
      setQuestions(data.payload);
      setPage(0);
      // Countdown is started by the timer effect once questions are set.
    });
  }, []);

  const exitQuizHandler = () => {
    swal({
      title: "Exit without submitting?",
      text: "Your answers will not be saved. You can retake this quiz later.",
      icon: "warning",
      buttons: ["Stay", "Exit"],
      dangerMode: true,
    }).then((willExit) => {
      if (willExit) {
        submittedRef.current = true;
        stopTimer();
        localStorage.removeItem("answers");
        navigate("/quizzes");
      }
    });
  };

  return (
    <div className="userQuestionsPage__container">
      <div className="userQuestionsPage__content">
        <h2 style={{ color: "var(--mt-primary)" }}>{`Questions : ${quizTitle}`}</h2>
        {questions.length > 0 && (
          <div style={{ margin: "10px 0" }}>
            <ProgressBar
              now={(answeredCount / questions.length) * 100}
              label={`${answeredCount} / ${questions.length} answered`}
              variant="success"
              style={{ height: "22px" }}
            />
          </div>
        )}
        <div className="userQuestionsPage__content--options">
          <Button
            variant="outline-danger"
            className="userQuestionsPage__content--button"
            onClick={exitQuizHandler}
          >
            Exit Quiz
          </Button>
          <div className="userQuestionsPage__content--spinner">
            {quiz && quiz.timerEnabled && quiz.timerMinutes ? (
              <>
                <ReactSpinnerTimer
                  timeInSeconds={timeRemaining || 1}
                  totalLaps={1}
                  onLapInteraction={() => {}}
                  isRefresh={false}
                  isPause={false}
                />
                <h4 style={{ marginTop: "18px" }}>{`${parseInt(
                  timeRemaining / 60
                ).zeroPad()} : ${(timeRemaining % 60).zeroPad()}`}</h4>
                Timer
              </>
            ) : (
              <h4 style={{ margin: 0 }}>No time limit</h4>
            )}
          </div>
        </div>
        {questions && questions.length > 0 ? (
          <>
            {questions
              .slice(
                page * QUESTIONS_PER_PAGE,
                page * QUESTIONS_PER_PAGE + QUESTIONS_PER_PAGE
              )
              .map((q, i) => {
                const index = page * QUESTIONS_PER_PAGE + i;
                return (
                  <Question
                    key={index}
                    number={index + 1}
                    question={q}
                    onAnswered={recomputeAnswered}
                  />
                );
              })}

            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                margin: "20px 0",
              }}
            >
              <Button
                variant="secondary"
                disabled={page === 0}
                onClick={() => {
                  setPage((p) => Math.max(0, p - 1));
                  window.scrollTo({ top: 0, behavior: "smooth" });
                }}
              >
                ← Previous
              </Button>

              <span>
                Page {page + 1} of {totalPages}
              </span>

              {page < totalPages - 1 ? (
                <Button
                  variant="primary"
                  onClick={() => {
                    setPage((p) => Math.min(totalPages - 1, p + 1));
                    window.scrollTo({ top: 0, behavior: "smooth" });
                  }}
                >
                  Next →
                </Button>
              ) : (
                <Button variant="success" onClick={() => submitQuizHandler()}>
                  Submit Quiz
                </Button>
              )}
            </div>
          </>
        ) : (
          <Loader />
        )}
      </div>
    </div>
  );
};

export default UserQuestionsPage;
