import * as questionsConstants from "../constants/questionsConstants";
import questionsServices from "../services/questionsServices";

export const fetchQuestionsByQuiz = async (dispatch, quizId) => {
  dispatch({ type: questionsConstants.FETCH_QUESTIONS_REQUEST });
  const data = await questionsServices.fetchQuestionsByQuiz(quizId);
  if (data) {
    return dispatch({ type: questionsConstants.FETCH_QUESTIONS_SUCCESS, payload: data });
  } else {
    return dispatch({ type: questionsConstants.FETCH_QUESTIONS_FAILURE, payload: data });
  }
};

export const fetchExamQuestions = async (dispatch, quizId) => {
  dispatch({ type: questionsConstants.FETCH_QUESTIONS_REQUEST });
  const data = await questionsServices.fetchExamQuestions(quizId);
  if (data) {
    return dispatch({ type: questionsConstants.FETCH_QUESTIONS_SUCCESS, payload: data });
  } else {
    return dispatch({ type: questionsConstants.FETCH_QUESTIONS_FAILURE, payload: data });
  }
};

export const addQuestion = async (dispatch, question) => {
  dispatch({ type: questionsConstants.ADD_QUESTION_REQUEST });
  const { data, isAdded, error } = await questionsServices.addQuestion(question);
  if (isAdded) {
    return dispatch({ type: questionsConstants.ADD_QUESTION_SUCCESS, payload: data });
  } else {
    return dispatch({ type: questionsConstants.ADD_QUESTION_FAILURE, payload: error });
  }
};

export const deleteQuestion = async (dispatch, quesId) => {
  dispatch({ type: questionsConstants.DELETE_QUESTION_REQUEST });
  const { isDeleted, error } = await questionsServices.deleteQuestion(quesId);
  if (isDeleted) {
    return dispatch({ type: questionsConstants.DELETE_QUESTION_SUCCESS, payload: quesId });
  } else {
    return dispatch({ type: questionsConstants.DELETE_QUESTION_FAILURE, payload: error });
  }
};

export const updateQuestion = async (dispatch, question) => {
  dispatch({ type: questionsConstants.UPDATE_QUESTION_REQUEST });
  const { data, isUpdated, error } = await questionsServices.updateQuestion(question);
  if (isUpdated) {
    return dispatch({ type: questionsConstants.UPDATE_QUESTION_SUCCESS, payload: data });
  } else {
    return dispatch({ type: questionsConstants.UPDATE_QUESTION_FAILURE, payload: error });
  }
};
