import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button, Form } from "react-bootstrap";
import "./AdminAddQuiz.css";
import { useDispatch } from "react-redux";
import swal from "sweetalert";
import RoleSidebar from "../../../components/RoleSidebar";
import FormContainer from "../../../components/FormContainer";
import * as quizzesConstants from "../../../constants/quizzesConstants";
import { addQuiz } from "../../../actions/quizzesActions";
import categoriesServices from "../../../services/categoriesServices";

const AdminAddQuiz = () => {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [maxMarks, setMaxMarks] = useState(0);
  const [numberOfQuestions, setNumberOfQuestions] = useState(0);
  const [isActive, setIsActive] = useState(false);
  const [selectedClassId, setSelectedClassId] = useState(null);
  const [questionsPerExam, setQuestionsPerExam] = useState("");
  const [randomizeQuestions, setRandomizeQuestions] = useState(false);
  const [timerEnabled, setTimerEnabled] = useState(false);
  const [timerMinutes, setTimerMinutes] = useState("");
  const [allQuestionsMandatory, setAllQuestionsMandatory] = useState(false);

  const [classes, setClasses] = useState([]);

  const navigate = useNavigate();
  const dispatch = useDispatch();

  const onClickPublishedHandler = () => {
    setIsActive(!isActive);
  };

  const onSelectClassHandler = (e) => {
    setSelectedClassId(e.target.value);
  };

  const submitHandler = (e) => {
    e.preventDefault();
    if (selectedClassId === null || selectedClassId === "n/a") {
      alert("Select a valid class!");
      return;
    }
    const quiz = {
      title: title,
      description: description,
      isActive: isActive,
      questionsPerExam:
        questionsPerExam === "" ? null : Number(questionsPerExam),
      randomizeQuestions: randomizeQuestions,
      // Answer-option randomization is not exposed in the UI; always off.
      randomizeOptions: false,
      timerEnabled: timerEnabled,
      timerMinutes:
        timerEnabled && timerMinutes !== "" ? Number(timerMinutes) : null,
      allQuestionsMandatory: allQuestionsMandatory,
      category: { catId: Number(selectedClassId) },
    };
    addQuiz(dispatch, quiz).then((data) => {
      if (data.type === quizzesConstants.ADD_QUIZ_SUCCESS) {
        swal("Quiz Added!", `${quiz.title} succesfully added`, "success");
        navigate("/adminQuizzes");
      } else {
        swal("Quiz Not Added!", data.payload || `${quiz.title} not added`, "error");
      }
    });
  };

  useEffect(() => {
    if (!localStorage.getItem("user")) navigate("/");
    categoriesServices.fetchCategories().then((data) => {
      if (Array.isArray(data)) setClasses(data);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="adminAddQuizPage__container">
      <div className="adminAddQuizPage__sidebar">
        <RoleSidebar />
      </div>
      <div className="mt-page">
        <FormContainer>
          <Button
            variant="secondary"
            className="mb-3"
            onClick={() => navigate("/adminQuizzes")}
          >
            ← Back to Quizzes
          </Button>
          <h2 style={{ color: "var(--mt-primary)" }}>Add Quiz</h2>
          <div className="mt-card p-4">
          <Form onSubmit={submitHandler}>
            <Form.Group className="my-3" controlId="title">
              <Form.Label>Title</Form.Label>
              <Form.Control
                type="text"
                placeholder="Enter Quiz Title"
                value={title}
                required
                onChange={(e) => {
                  setTitle(e.target.value);
                }}
              ></Form.Control>
            </Form.Group>

            <Form.Group className="my-3" controlId="description">
              <Form.Label>Description</Form.Label>
              <Form.Control
                style={{ textAlign: "top" }}
                as="textarea"
                rows="3"
                type="text"
                placeholder="Enter Quiz Description"
                value={description}
                onChange={(e) => {
                  setDescription(e.target.value);
                }}
              ></Form.Control>
            </Form.Group>

            {/* <Form.Group className="my-3" controlId="maxMarks">
              <Form.Label>Maximum Marks</Form.Label>
              <Form.Control
                type="number"
                placeholder="Enter Maximum Marks"
                value={maxMarks}
                onChange={(e) => {
                  setMaxMarks(e.target.value);
                }}
              ></Form.Control>
            </Form.Group> */}

            {/* <Form.Group className="my-3" controlId="numberOfQuestions">
              <Form.Label>Number of Questions</Form.Label>
              <Form.Control
                type="number"
                placeholder="Enter Number of Questions"
                value={numberOfQuestions}
                onChange={(e) => {
                  setNumberOfQuestions(e.target.value);
                }}
              ></Form.Control>
            </Form.Group> */}

            <Form.Group className="my-3" controlId="questionsPerExam">
              <Form.Label>Questions per exam</Form.Label>
              <Form.Control
                type="number"
                min="1"
                placeholder="Leave blank to serve the whole pool"
                value={questionsPerExam}
                onChange={(e) => setQuestionsPerExam(e.target.value)}
              ></Form.Control>
              <Form.Text muted>
                How many questions each student receives from this quiz's pool.
              </Form.Text>
            </Form.Group>

            <Form.Check
              className="my-3"
              type="switch"
              id="randomize-questions-switch"
              label="Randomize question order"
              onChange={() => setRandomizeQuestions(!randomizeQuestions)}
              checked={randomizeQuestions}
            />

            <Form.Check
              className="my-3"
              type="switch"
              id="timer-switch"
              label="Enable exam timer"
              onChange={() => setTimerEnabled(!timerEnabled)}
              checked={timerEnabled}
            />

            {timerEnabled && (
              <Form.Group className="my-3" controlId="timerMinutes">
                <Form.Label>Timer duration (minutes)</Form.Label>
                <Form.Control
                  type="number"
                  min="1"
                  required
                  placeholder="e.g. 30"
                  value={timerMinutes}
                  onChange={(e) => setTimerMinutes(e.target.value)}
                ></Form.Control>
                <Form.Text muted>
                  Students must finish the exam within this time.
                </Form.Text>
              </Form.Group>
            )}

            <Form.Check
              className="my-3"
              type="switch"
              id="mandatory-switch"
              label="All questions mandatory"
              onChange={() => setAllQuestionsMandatory(!allQuestionsMandatory)}
              checked={allQuestionsMandatory}
            />

            <Form.Check
              className="my-3"
              type="switch"
              id="publish-switch"
              label="Publish Quiz"
              onChange={onClickPublishedHandler}
              checked={isActive}
            />

            <div className="my-3">
              <label htmlFor="class-select">Choose a Class:</label>
              <Form.Select
                aria-label="Choose Class"
                id="class-select"
                onChange={onSelectClassHandler}
              >
                <option value="n/a">Choose Class</option>
                {classes.map((c) => (
                  <option key={c.catId} value={c.catId}>
                    {c.title}
                  </option>
                ))}
              </Form.Select>
            </div>
            <Button
              className="my-5 adminAddQuizPage__content--button"
              type="submit"
              variant="primary"
            >
              Add
            </Button>
          </Form>
          </div>
        </FormContainer>
      </div>
    </div>
  );
};

export default AdminAddQuiz;
