import React, { useState } from "react";
import { Button, Form } from "react-bootstrap";
import swal from "sweetalert";
import { addQuestion } from "../../../actions/questionsActions";
import FormContainer from "../../../components/FormContainer";
import Sidebar from "../../../components/Sidebar";
import * as questionsConstants from "../../../constants/questionsConstants";
import "./AdminAddQuestionsPage.css";
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import DimensionSelect from "../../../components/DimensionSelect";

const AdminAddQuestionsPage = () => {
  const [content, setContent] = useState("");
  const [image, setImage] = useState("");
  const [option1, setOption1] = useState("");
  const [option2, setOption2] = useState("");
  const [option3, setOption3] = useState("");
  const [option4, setOption4] = useState("");
  const [answer, setAnswer] = useState(null);
  const [dimension, setDimension] = useState("");
  // Optional per-option overrides — most questions leave these blank and just
  // use `dimension` above; set one when different answers measure different traits.
  const [option1Dimension, setOption1Dimension] = useState("");
  const [option2Dimension, setOption2Dimension] = useState("");
  const [option3Dimension, setOption3Dimension] = useState("");
  const [option4Dimension, setOption4Dimension] = useState("");

  const navigate = useNavigate();
  const dispatch = useDispatch();
  const urlParams = new URLSearchParams(window.location.search);
  const quizId = urlParams.get("quizId");
  const quizTitle = urlParams.get("quizTitle");
  const token = JSON.parse(localStorage.getItem("jwtToken"));
  const backToQuestionsUrl = `/adminQuestions/?quizId=${quizId}${
    quizTitle ? `&quizTitle=${quizTitle}` : ""
  }`;

  const onSelectAnswerHandler = (e) => {
    setAnswer(e.target.value);
  };

  const submitHandler = (e) => {
    e.preventDefault();
    if (dimension === "" || dimension === "n/a") {
      alert("Select the dimension this question measures!");
      return;
    }
    if (answer !== null && answer !== "n/a") {
      const question = {
        content: content,
        image: image,
        option1: option1,
        option2: option2,
        option3: option3,
        option4: option4,
        option1Dimension: option1Dimension,
        option2Dimension: option2Dimension,
        option3Dimension: option3Dimension,
        option4Dimension: option4Dimension,
        answer: answer,
        dimension: dimension,
        quiz: {
          quizId: quizId,
        },
      };

      addQuestion(dispatch, question, token).then((data) => {
        if (data.type === questionsConstants.ADD_QUESTION_SUCCESS) {
          swal("Question Added!", `${content} succesfully added`, "success").then(() =>
            navigate(backToQuestionsUrl)
          );
        } else {
          swal("Question Not Added!", data.payload || `${content} not added`, "error");
        }
      });
    } else {
      alert("Select valid answer!");
    }
  };

  return (
    <div className="adminAddQuestionPage__container">
      <div className="adminAddQuestionPage__sidebar">
        <Sidebar />
      </div>
      <div className="adminAddQuestionPage__content">
        <FormContainer>
          <Button
            variant="secondary"
            className="mb-3"
            onClick={() => navigate(backToQuestionsUrl)}
          >
            ← Back to Questions
          </Button>
          <h2>Add Question</h2>
          <Form onSubmit={submitHandler}>
            <Form.Group className="my-3" controlId="content">
              <Form.Label>Question</Form.Label>
              <Form.Control
                style={{ textAlign: "top" }}
                as="textarea"
                rows="3"
                type="text"
                placeholder="Enter Question Content"
                value={content}
                required
                onChange={(e) => {
                  setContent(e.target.value);
                }}
              ></Form.Control>
            </Form.Group>

            <Form.Group className="my-3" controlId="option1">
              <Form.Label>Option 1</Form.Label>
              <Form.Control
                style={{ textAlign: "top" }}
                as="textarea"
                rows="2"
                type="text"
                placeholder="Enter Option 1"
                value={option1}
                required
                onChange={(e) => {
                  setOption1(e.target.value);
                }}
              ></Form.Control>
              <div className="mt-1">
                <label htmlFor="option1-dimension" className="form-text">
                  Scores as (optional — leave blank to use the dimension below)
                </label>
                <DimensionSelect
                  id="option1-dimension"
                  value={option1Dimension}
                  onChange={(e) => setOption1Dimension(e.target.value)}
                  blankLabel="Use question's dimension"
                />
              </div>
            </Form.Group>

            <Form.Group className="my-3" controlId="option2">
              <Form.Label>Option 2</Form.Label>
              <Form.Control
                style={{ textAlign: "top" }}
                as="textarea"
                rows="2"
                type="text"
                placeholder="Enter Option 2"
                value={option2}
                required
                onChange={(e) => {
                  setOption2(e.target.value);
                }}
              ></Form.Control>
              <div className="mt-1">
                <label htmlFor="option2-dimension" className="form-text">
                  Scores as (optional — leave blank to use the dimension below)
                </label>
                <DimensionSelect
                  id="option2-dimension"
                  value={option2Dimension}
                  onChange={(e) => setOption2Dimension(e.target.value)}
                  blankLabel="Use question's dimension"
                />
              </div>
            </Form.Group>

            <Form.Group className="my-3" controlId="option3">
              <Form.Label>Option 3 (optional)</Form.Label>
              <Form.Control
                style={{ textAlign: "top" }}
                as="textarea"
                rows="2"
                type="text"
                placeholder="Enter Option 3"
                value={option3}
                onChange={(e) => {
                  setOption3(e.target.value);
                }}
              ></Form.Control>
              {option3 && (
                <div className="mt-1">
                  <label htmlFor="option3-dimension" className="form-text">
                    Scores as (optional — leave blank to use the dimension below)
                  </label>
                  <DimensionSelect
                    id="option3-dimension"
                    value={option3Dimension}
                    onChange={(e) => setOption3Dimension(e.target.value)}
                    blankLabel="Use question's dimension"
                  />
                </div>
              )}
            </Form.Group>

            <Form.Group className="my-3" controlId="option4">
              <Form.Label>Option 4 (optional)</Form.Label>
              <Form.Control
                style={{ textAlign: "top" }}
                as="textarea"
                rows="2"
                type="text"
                placeholder="Enter Option 4"
                value={option4}
                onChange={(e) => {
                  setOption4(e.target.value);
                }}
              ></Form.Control>
              {option4 && (
                <div className="mt-1">
                  <label htmlFor="option4-dimension" className="form-text">
                    Scores as (optional — leave blank to use the dimension below)
                  </label>
                  <DimensionSelect
                    id="option4-dimension"
                    value={option4Dimension}
                    onChange={(e) => setOption4Dimension(e.target.value)}
                    blankLabel="Use question's dimension"
                  />
                </div>
              )}
            </Form.Group>

            <div className="my-3">
              <label htmlFor="dimension-select">Dimension this question measures:</label>
              <DimensionSelect
                id="dimension-select"
                value={dimension}
                onChange={(e) => setDimension(e.target.value)}
                blankLabel="Choose Dimension"
              />
            </div>

            <div className="my-3">
              <label htmlFor="answer-select">Choose Correct Option:</label>
              <Form.Select
                aria-label="Choose Correct Option"
                id="answer-select"
                onChange={onSelectAnswerHandler}
              >
                <option value="n/a">Choose Option</option>
                <option value="option1">Option 1</option>
                <option value="option2">Option 2</option>
                {option3 && <option value="option3">Option 3</option>}
                {option4 && <option value="option4">Option 4</option>}
                {/* {categories ? (
                  categories.map((cat, index) => (
                    <option key={index} value={cat.catId}>
                      {cat.title}
                    </option>
                  ))
                ) : (
                  <option value="">Choose one from below</option>
                )} */}
                {/* <option value="1">One</option>
                <option value="2">Two</option>
                <option value="3">Three</option> */}
              </Form.Select>
            </div>
            <Button
              className="my-5 adminAddQuestionPage__content--button"
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

export default AdminAddQuestionsPage;
