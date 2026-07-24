import * as c from "../constants/mentalistReportConstants";
import mentalistReportServices from "../services/mentalistReportServices";

export const generateMentalistReport = async (dispatch, quizResId, token, options) => {
  dispatch({ type: c.GENERATE_MENTALIST_REPORT_REQUEST });
  const { data, error } = await mentalistReportServices.generateReport(quizResId, token, options);
  if (data) {
    return dispatch({ type: c.GENERATE_MENTALIST_REPORT_SUCCESS, payload: data });
  }
  return dispatch({ type: c.GENERATE_MENTALIST_REPORT_FAILURE, payload: error });
};
