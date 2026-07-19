import * as c from "../constants/psychometricReportConstants";
import psychometricReportServices from "../services/psychometricReportServices";

export const fetchPsychReport = async (dispatch, quizResId, token) => {
  dispatch({ type: c.FETCH_PSYCH_REPORT_REQUEST });
  const { data, error } = await psychometricReportServices.fetchReport(
    quizResId,
    token
  );
  if (data) {
    return dispatch({ type: c.FETCH_PSYCH_REPORT_SUCCESS, payload: data });
  }
  return dispatch({ type: c.FETCH_PSYCH_REPORT_FAILURE, payload: error });
};
