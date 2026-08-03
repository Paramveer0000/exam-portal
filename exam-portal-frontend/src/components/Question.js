import React, { useState } from "react";
import { InputGroup } from "react-bootstrap";
import "./Question.css";
import { useNavigate } from "react-router-dom";
import { useDispatch } from "react-redux";
import { deleteQuestion } from "../actions/questionsActions";
import swal from "sweetalert";
import * as questionsConstants from "../constants/questionsConstants";

const Question = ({ number, answers, question, isAdmin = false, canEdit = true, onAnswered }) => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const answer = question.answer;
  // Admin list can hold dozens of questions; collapsed by default so Update/Delete
  // are reachable without scrolling past every option block.
  const [expanded, setExpanded] = useState(!isAdmin);

  // Paging unmounts the off-screen questions, so a radio that was ticked earlier
  // comes back blank on return even though the answer is still in localStorage.
  // Seed each radio from the saved answer (defaultChecked, so clicking still works).
  const savedAnswer = isAdmin
    ? undefined
    : (JSON.parse(localStorage.getItem("answers")) || {})[question.quesId];

  const saveAnswer = (quesId, ans) => {
    const newAns = {};
    newAns[quesId] = ans;
    let answers = JSON.parse(localStorage.getItem("answers"));
    if (answers) {
      answers[quesId] = ans;
      localStorage.setItem("answers", JSON.stringify(answers));
    } else {
      localStorage.setItem("answers", JSON.stringify(newAns));
    }
    if (onAnswered) onAnswered(quesId);
  };
  const updateQuestionHandler = (ques) => {
    const quizTitle = new URLSearchParams(window.location.search).get("quizTitle");
    navigate(
      `/adminUpdateQuestion/${ques.quesId}/?quizId=${ques.quiz.quizId}${
        quizTitle ? `&quizTitle=${quizTitle}` : ""
      }`
    );
  };

  const deleteQuestionHandler = (ques) => {
    swal({
      title: "Are you sure?",
      text: "Once deleted, you will not be able to recover this question!",
      icon: "warning",
      buttons: true,
      dangerMode: true,
    }).then((willDelete) => {
      if (willDelete) {
        deleteQuestion(dispatch, ques.quesId).then((data) => {
          if (data.type === questionsConstants.DELETE_QUESTION_SUCCESS) {
            swal(
              "Question Deleted!",
              `Question with id ${ques.quesId}, succesfully deleted`,
              "success"
            );
          } else {
            swal(
              "Question Not Deleted!",
              `Question with id ${ques.quesId} not deleted`,
              "error"
            );
          }
        });
      } else {
        swal(`Question with id ${ques.quesId} is safe`);
      }
    });
  };

  return (
    <div className="question__container">
      <div
        className="question__content"
        onClick={isAdmin ? () => setExpanded((e) => !e) : undefined}
        style={
          isAdmin
            ? { cursor: "pointer", display: "flex", justifyContent: "space-between", alignItems: "center" }
            : undefined
        }
      >
        <span>{number + ". " + question.content}</span>
        {isAdmin && (
          <div className="question__content--editButtons" style={{ flexShrink: 0 }}>
            <span style={{ marginRight: "10px", color: "#888" }}>{expanded ? "▲" : "▼"}</span>
            {canEdit && (
              <>
                <span
                  onClick={(e) => {
                    e.stopPropagation();
                    updateQuestionHandler(question);
                  }}
                  style={{ margin: "2px 8px", color: "#1E7A6F", fontWeight: "500", cursor: "pointer" }}
                >
                  Update
                </span>
                <span
                  onClick={(e) => {
                    e.stopPropagation();
                    deleteQuestionHandler(question);
                  }}
                  style={{ margin: "2px 8px", color: "red", fontWeight: "500", cursor: "pointer" }}
                >
                  Delete
                </span>
              </>
            )}
          </div>
        )}
      </div>
      {!expanded ? null : (
      <div className="question__options">
        <InputGroup
          onChange={(e) => {
            saveAnswer(question.quesId, e.target.value);

          }}
        >
          <div className="question__options--2">
            <div className="question__options--optionDiv">
              <InputGroup.Radio
                value={question.option1}
                name={number}
                defaultChecked={savedAnswer === question.option1}
                aria-label="option 1"
              />
              <span className="question__options--optionText">
                {question.option1}
              </span>
            </div>
            <div className="question__options--optionDiv">
              <InputGroup.Radio
                value={question.option2}
                name={number}
                defaultChecked={savedAnswer === question.option2}
                aria-label="option 2"
              />
              <span className="question__options--optionText">
                {question.option2}
              </span>
            </div>
          </div>

          {(question.option3 || question.option4) && (
            <div className="question__options--2">
              {question.option3 && (
                <div className="question__options--optionDiv">
                  <InputGroup.Radio
                    value={question.option3}
                    name={number}
                    defaultChecked={savedAnswer === question.option3}
                    aria-label="option 3"
                  />
                  <span className="question__options--optionText">
                    {question.option3}
                  </span>
                </div>
              )}
              {question.option4 && (
                <div className="question__options--optionDiv">
                  <InputGroup.Radio
                    value={question.option4}
                    name={number}
                    defaultChecked={savedAnswer === question.option4}
                    aria-label="option 4"
                  />
                  <span className="question__options--optionText">
                    {question.option4}
                  </span>
                </div>
              )}
            </div>
          )}

          {question.option5 && (
            <div className="question__options--2">
              <div className="question__options--optionDiv">
                <InputGroup.Radio
                  value={question.option5}
                  name={number}
                  defaultChecked={savedAnswer === question.option5}
                  aria-label="option 5"
                />
                <span className="question__options--optionText">
                  {question.option5}
                </span>
              </div>
            </div>
          )}
        </InputGroup>
      </div>
      )}
      {isAdmin && expanded && (
        <p style={{ margin: "5px" }}>{`Correct Answer: ${question[answer]}`}</p>
      )}
      <hr />
    </div>
  );
};

export default Question;
