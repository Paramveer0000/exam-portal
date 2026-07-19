import * as c from "../constants/psychometricReportConstants";

const initialState = {
  loading: false,
  report: null,
  error: null,
};

export const psychometricReportReducer = (state = initialState, action) => {
  switch (action.type) {
    case c.FETCH_PSYCH_REPORT_REQUEST:
      return { ...state, loading: true, report: null, error: null };
    case c.FETCH_PSYCH_REPORT_SUCCESS:
      return { ...state, loading: false, report: action.payload };
    case c.FETCH_PSYCH_REPORT_FAILURE:
      return { ...state, loading: false, error: action.payload };
    default:
      return state;
  }
};
