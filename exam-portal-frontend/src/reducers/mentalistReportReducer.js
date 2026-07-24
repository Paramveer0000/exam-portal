import * as c from "../constants/mentalistReportConstants";

const initialState = {
  loading: false,
  report: null,
  error: null,
};

export const mentalistReportReducer = (state = initialState, action) => {
  switch (action.type) {
    case c.GENERATE_MENTALIST_REPORT_REQUEST:
      return { ...state, loading: true, error: null };
    case c.GENERATE_MENTALIST_REPORT_SUCCESS:
      return { ...state, loading: false, report: action.payload };
    case c.GENERATE_MENTALIST_REPORT_FAILURE:
      return { ...state, loading: false, error: action.payload };
    default:
      return state;
  }
};
