import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Button, Form } from "react-bootstrap";
import "./AdminUpdateQuiz.css";
import swal from "sweetalert";
import { useDispatch, useSelector } from "react-redux";
import Sidebar from "../../../components/Sidebar";
import FormContainer from "../../../components/FormContainer";
import * as quizzesConstants from "../../../constants/quizzesConstants";
import subjectsServices from "../../../services/subjectsServices";
import categoriesServices from "../../../services/categoriesServices";
import "./AdminUpdateQuiz.css";
import { fetchQuizzes, updateQuiz } from "../../../actions/quizzesActions";

const AdminUpdateQuiz = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const params = useParams();
  const quizId = params.quizId;

  const oldQuiz = useSelector((state) =>
    state.quizzesReducer.quizzes.filter((quiz) => quiz.quizId == quizId)
  )[0];

  const [title, setTitle] = useState(oldQuiz.title);
  const [description, setDescription] = useState(oldQuiz.description);
  const [maxMarks, setMaxMarks] = useState(oldQuiz.maxMarks);
  const [numberOfQuestions, setNumberOfQuestions] = useState(
    oldQuiz.numberOfQuestions
  );
  const [isActive, setIsActive] = useState(oldQuiz.isActive);
  const [selectedSubjectId, setSelectedSubjectId] = useState(
    oldQuiz.subject ? oldQuiz.subject.subjectId : null
  );
  const [questionsPerExam, setQuestionsPerExam] = useState(
    oldQuiz.questionsPerExam == null ? "" : oldQuiz.questionsPerExam
  );
  const [randomizeQuestions, setRandomizeQuestions] = useState(
    oldQuiz.randomizeQuestions || false
  );
  const [randomizeOptions, setRandomizeOptions] = useState(
    oldQuiz.randomizeOptions || false
  );
  const [timerEnabled, setTimerEnabled] = useState(
    oldQuiz.timerEnabled || false
  );
  const [timerMinutes, setTimerMinutes] = useState(
    oldQuiz.timerMinutes == null ? "" : oldQuiz.timerMinutes
  );
  const [allQuestionsMandatory, setAllQuestionsMandatory] = useState(
    oldQuiz.allQuestionsMandatory || false
  );

  const [subjects, setSubjects] = useState([]);
  const [classesById, setClassesById] = useState({});

  const onClickPublishedHandler = () => {
    setIsActive(!isActive);
  };

  const onSelectSubjectHandler = (e) => {
    setSelectedSubjectId(e.target.value);
  };

  const token = JSON.parse(localStorage.getItem("jwtToken"));

  const subjectLabel = (s) =>
    `${classesById[s.classId] || "Class #" + s.classId} → ${s.title}`;

  const submitHandler = (e) => {
    e.preventDefault();
    if (selectedSubjectId !== null && selectedSubjectId !== "n/a") {
      const quiz = {
        quizId: quizId,
        title: title,
        description: description,
        isActive: isActive,
        questionsPerExam:
          questionsPerExam === "" ? null : Number(questionsPerExam),
        randomizeQuestions: randomizeQuestions,
        randomizeOptions: randomizeOptions,
        timerEnabled: timerEnabled,
        timerMinutes:
          timerEnabled && timerMinutes !== "" ? Number(timerMinutes) : null,
        allQuestionsMandatory: allQuestionsMandatory,
        subject: { subjectId: Number(selectedSubjectId) },
      };
      updateQuiz(dispatch, quiz, token).then((data) => {
        if (data.type === quizzesConstants.UPDATE_QUIZ_SUCCESS) {
          swal("Quiz Updated!", `${quiz.title} succesfully updated`, "success");
          fetchQuizzes(dispatch, token);
        } else {
          swal("Quiz Not Updated!", `${quiz.title} not updated`, "error");
        }
      });
    } else {
      alert("Select a valid subject!");
    }
  };

  useEffect(() => {
    if (!localStorage.getItem("jwtToken")) navigate("/");
    subjectsServices.fetchSubjects(token).then(({ data }) => {
      if (Array.isArray(data)) setSubjects(data);
    });
    categoriesServices.fetchCategories(token).then((data) => {
      if (Array.isArray(data)) {
        const map = {};
        data.forEach((c) => (map[c.catId] = c.title));
        setClassesById(map);
      }
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="adminUpdateQuizPage__container">
      <div className="adminUpdateQuizPage__sidebar">
        <Sidebar />
      </div>
      <div className="adminUpdateQuizPage__content">
        <FormContainer>
          <Button
            variant="secondary"
            className="mb-3"
            onClick={() => navigate("/adminQuizzes")}
          >
            ← Back to Subjects
          </Button>
          <h2>Update Subject</h2>
          <Form onSubmit={submitHandler}>
            <Form.Group className="my-3" controlId="title">
              <Form.Label>Title</Form.Label>
              <Form.Control
                type="text"
                placeholder="Enter Subject Title"
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
            </Form.Group>

            <Form.Group className="my-3" controlId="numberOfQuestions">
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
              id="randomize-options-switch"
              label="Randomize answer options"
              onChange={() => setRandomizeOptions(!randomizeOptions)}
              checked={randomizeOptions}
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
            style={{borderColor:"rgb(68 177 49)"}}
              className="my-3"
              type="switch"
              id="publish-switch"
              label="Publish Quiz"
              onChange={onClickPublishedHandler}
              checked={isActive}
            />

            <div className="my-3">
              <label htmlFor="subject-select">Choose a Subject (Class → Subject):</label>
              <Form.Select
                aria-label="Choose Subject"
                id="subject-select"
                value={selectedSubjectId || "n/a"}
                onChange={onSelectSubjectHandler}
              >
                <option value="n/a">Choose Subject</option>
                {subjects.map((s) => (
                  <option key={s.subjectId} value={s.subjectId}>
                    {subjectLabel(s)}
                  </option>
                ))}
              </Form.Select>
            </div>
            <Button
              className="my-5 adminUpdateQuizPage__content--button"
              type="submit"
              variant="primary"
            >
              Update
            </Button>
          </Form>
        </FormContainer>
      </div>
    </div>
  );
};

export default AdminUpdateQuiz;
