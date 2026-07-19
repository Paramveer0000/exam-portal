import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button, Form } from "react-bootstrap";
import "./AdminAddQuiz.css";
import { useDispatch, useSelector } from "react-redux";
import swal from "sweetalert";
import Sidebar from "../../../components/Sidebar";
import FormContainer from "../../../components/FormContainer";
import * as quizzesConstants from "../../../constants/quizzesConstants";
import { addQuiz } from "../../../actions/quizzesActions";
import { fetchCategories } from "../../../actions/categoriesActions";

const AdminAddQuiz = () => {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [maxMarks, setMaxMarks] = useState(0);
  const [numberOfQuestions, setNumberOfQuestions] = useState(0);
  const [isActive, setIsActive] = useState(false);
  const [selectedCategoryId, setSelectedCategoryId] = useState(null);
  const [questionsPerExam, setQuestionsPerExam] = useState("");
  const [randomizeQuestions, setRandomizeQuestions] = useState(false);
  const [randomizeOptions, setRandomizeOptions] = useState(false);
  const [timerEnabled, setTimerEnabled] = useState(false);
  const [timerMinutes, setTimerMinutes] = useState("");
  const [allQuestionsMandatory, setAllQuestionsMandatory] = useState(false);

  const categoriesReducer = useSelector((state) => state.categoriesReducer);
  const [categories, setCategories] = useState(categoriesReducer.categories);

  const navigate = useNavigate();
  const dispatch = useDispatch();

  const onClickPublishedHandler = () => {
    setIsActive(!isActive);
  };

  const onSelectCategoryHandler = (e) => {
    setSelectedCategoryId(e.target.value);
  };

  const token = JSON.parse(localStorage.getItem("jwtToken"));

  const submitHandler = (e) => {
    e.preventDefault();
    if (selectedCategoryId !== null && selectedCategoryId !== "n/a") {
      const quiz = {
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
        category: {
          catId: selectedCategoryId,
          title: categories.filter((cat) => cat.catId == selectedCategoryId)[0][
            "title"
          ],
          description: categories.filter(
            (cat) => cat.catId == selectedCategoryId
          )[0]["description"],
        },
      };
      addQuiz(dispatch, quiz, token).then((data) => {
        if (data.type === quizzesConstants.ADD_QUIZ_SUCCESS) {
          swal("Subject Added!", `${quiz.title} succesfully added`, "success");
          navigate("/adminQuizzes");
        } else {
          swal("Subject Not Added!", data.payload || `${quiz.title} not added`, "error");
        }
      });
    } else {
      alert("Select valid class!");
    }
  };

  useEffect(() => {
    if (!localStorage.getItem("jwtToken")) navigate("/");
  }, []);

  useEffect(() => {
    if (categories.length === 0) {
      fetchCategories(dispatch, token).then((data) => {
        setCategories(data.payload);
      });
    }
  }, []);

  return (
    <div className="adminAddQuizPage__container">
      <div className="adminAddQuizPage__sidebar">
        <Sidebar />
      </div>
      <div className="adminAddQuizPage__content">
        <FormContainer>
          <Button
            variant="secondary"
            className="mb-3"
            onClick={() => navigate("/adminQuizzes")}
          >
            ← Back to Subjects
          </Button>
          <h2>Add Subject</h2>
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
              className="my-3"
              type="switch"
              id="publish-switch"
              label="Publish Quiz"
              onChange={onClickPublishedHandler}
              checked={isActive}
            />

            <div className="my-3">
              <label htmlFor="category-select">Choose a Class:</label>
              <Form.Select
                aria-label="Choose Category"
                id="category-select"
                onChange={onSelectCategoryHandler}
              >
                <option value="n/a">Choose Category</option>
                {categories ? (
                  categories.map((cat, index) => (
                    <option key={index} value={cat.catId}>
                      {cat.title}
                    </option>
                  ))
                ) : (
                  <option value="">Choose one from below</option>
                )}
                {/* <option value="1">One</option>
                  <option value="2">Two</option>
                  <option value="3">Three</option> */}
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
        </FormContainer>
      </div>
    </div>
  );
};

export default AdminAddQuiz;
