import * as reportsConstants from "../constants/reportsConstants";
import reportsServices from "../services/reportsServices";

export const fetchQuizReport = async (dispatch, quizId, filters) => {
  dispatch({ type: reportsConstants.FETCH_REPORT_REQUEST });
  const { data, error } = await reportsServices.fetchQuizReport(quizId, filters);
  if (data) {
    return dispatch({ type: reportsConstants.FETCH_REPORT_SUCCESS, payload: data });
  }
  return dispatch({ type: reportsConstants.FETCH_REPORT_FAILURE, payload: error });
};

export const fetchStudentReport = async (dispatch, userId, filters) => {
  dispatch({ type: reportsConstants.FETCH_REPORT_REQUEST });
  const { data, error } = await reportsServices.fetchStudentReport(userId, filters);
  if (data) {
    return dispatch({ type: reportsConstants.FETCH_REPORT_SUCCESS, payload: data });
  }
  return dispatch({ type: reportsConstants.FETCH_REPORT_FAILURE, payload: error });
};

export const exportQuizReport = (quizId, filters) =>
  reportsServices.exportQuizReport(quizId, filters);
