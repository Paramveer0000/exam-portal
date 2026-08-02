import React, { useEffect, useState } from "react";
import { Button, Form } from "react-bootstrap";
import swal from "sweetalert";
import FormContainer from "../../../components/FormContainer";
import Sidebar from "../../../components/Sidebar";
import * as questionsConstants from "../../../constants/questionsConstants";
import { useNavigate, useParams } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import "./AdminUpdateQuestionPage.css";
import {
  fetchQuestionsByQuiz,
  updateQuestion,
} from "../../../actions/questionsActions";
import DimensionSelect from "../../../components/DimensionSelect";

const AdminUpdateQuestionPage = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const params = useParams();

  const quesId = params.quesId;
  const urlParams = new URLSearchParams(window.location.search);
  const quizId = urlParams.get("quizId");
  const quizTitle = urlParams.get("quizTitle");
  const backToQuestionsUrl = `/adminQuestions/?quizId=${quizId}${
    quizTitle ? `&quizTitle=${quizTitle}` : ""
  }`;

  const questionsReducer = useSelector((state) => state.questionsReducer);
  const [oldQuestion, setOldQuestion] = useState(
    questionsReducer.questions.filter((ques) => ques.quesId == quesId)[0]
  );

  const [content, setContent] = useState(
    oldQuestion ? oldQuestion.content : ""
  );
  const [image, setImage] = useState(oldQuestion ? oldQuestion.image : "");
  const [option1, setOption1] = useState(
    oldQuestion ? oldQuestion.option1 : ""
  );
  const [option2, setOption2] = useState(
    oldQuestion ? oldQuestion.option2 : ""
  );
  const [option3, setOption3] = useState(
    oldQuestion ? oldQuestion.option3 : ""
  );
  const [option4, setOption4] = useState(
    oldQuestion ? oldQuestion.option4 : ""
  );
  const [option5, setOption5] = useState(
    oldQuestion ? oldQuestion.option5 : ""
  );
  const [answer, setAnswer] = useState(oldQuestion ? oldQuestion.answer : null);
  const [dimensionCodes, setDimensionCodes] = useState(
    oldQuestion && oldQuestion.dimensions
      ? new Set(oldQuestion.dimensions.map((d) => d.dimensionCode))
      : new Set()
  );
  const [option1Dimension, setOption1Dimension] = useState(
    oldQuestion ? oldQuestion.option1Dimension || "" : ""
  );
  const [option2Dimension, setOption2Dimension] = useState(
    oldQuestion ? oldQuestion.option2Dimension || "" : ""
  );
  const [option3Dimension, setOption3Dimension] = useState(
    oldQuestion ? oldQuestion.option3Dimension || "" : ""
  );
  const [option4Dimension, setOption4Dimension] = useState(
    oldQuestion ? oldQuestion.option4Dimension || "" : ""
  );
  const [option5Dimension, setOption5Dimension] = useState(
    oldQuestion ? oldQuestion.option5Dimension || "" : ""
  );

  const onSelectAnswerHandler = (e) => {
    setAnswer(e.target.value);
  };

  const submitHandler = (e) => {
    e.preventDefault();
    if (dimensionCodes.size === 0) {
      alert("Select at least one dimension!");
      return;
    }
    if (answer !== null && answer !== "n/a") {
      const question = {
        quesId: quesId,
        content: content,
        image: image,
        option1: option1,
        option2: option2,
        option3: option3,
        option4: option4,
        option5: option5,
        option1Dimension: option1Dimension,
        option2Dimension: option2Dimension,
        option3Dimension: option3Dimension,
        option4Dimension: option4Dimension,
        option5Dimension: option5Dimension,
        answer: answer,
        dimensionCodes: Array.from(dimensionCodes),
        quizId: quizId,
      };

      updateQuestion(dispatch, question).then((data) => {
        if (data.type === questionsConstants.UPDATE_QUESTION_SUCCESS) {
          swal(
            "Question Updated!",
            `${content} succesfully updated`,
            "success"
          ).then(() => navigate(backToQuestionsUrl));
        } else {
          swal("Question Not Updated!", data.payload || `${content} not updated`, "error");
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
      <div className="mt-page">
        <FormContainer>
          <Button
            variant="secondary"
            className="mb-3"
            onClick={() => navigate(backToQuestionsUrl)}
          >
            ← Back to Questions
          </Button>
          <h2 style={{ color: "var(--mt-primary)" }}>Update Question</h2>
          <div className="mt-card p-4">
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

            <Form.Group className="my-3" controlId="option5">
              <Form.Label>Option 5 (optional)</Form.Label>
              <Form.Control
                style={{ textAlign: "top" }}
                as="textarea"
                rows="2"
                type="text"
                placeholder="Enter Option 5"
                value={option5}
                onChange={(e) => {
                  setOption5(e.target.value);
                }}
              ></Form.Control>
              {option5 && (
                <div className="mt-1">
                  <label htmlFor="option5-dimension" className="form-text">
                    Scores as (optional — leave blank to use the dimension below)
                  </label>
                  <DimensionSelect
                    id="option5-dimension"
                    value={option5Dimension}
                    onChange={(e) => setOption5Dimension(e.target.value)}
                    blankLabel="Use question's dimension"
                  />
                </div>
              )}
            </Form.Group>

            <div className="my-3">
              <label htmlFor="dimension-select">Dimensions this question measures (select one or more):</label>
              <DimensionSelect
                id="dimension-select"
                value={dimensionCodes}
                onChange={setDimensionCodes}
                blankLabel="Choose Dimensions"
                isMulti={true}
              />
            </div>

            <div className="my-3">
              <label htmlFor="answer-select">Choose Correct Option:</label>
              <Form.Select
                aria-label="Choose Correct Option"
                id="answer-select"
                value={answer || "n/a"}
                onChange={onSelectAnswerHandler}
              >
                <option value="n/a">Choose Option</option>
                <option value="option1">Option 1</option>
                <option value="option2">Option 2</option>
                {option3 && <option value="option3">Option 3</option>}
                {option4 && <option value="option4">Option 4</option>}
                {option5 && <option value="option5">Option 5</option>}
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
              Update
            </Button>
          </Form>
          </div>
        </FormContainer>
      </div>
    </div>
  );
};

export default AdminUpdateQuestionPage;
