import * as reportsConstants from "../constants/reportsConstants";

const initialState = {
  loading: false,
  error: null,
  rows: [],
};

export const reportsReducer = (state = initialState, action) => {
  switch (action.type) {
    case reportsConstants.FETCH_REPORT_REQUEST:
      return { ...state, loading: true, error: null };

    case reportsConstants.FETCH_REPORT_SUCCESS:
      return { ...state, loading: false, rows: action.payload };

    case reportsConstants.FETCH_REPORT_FAILURE:
      return { ...state, loading: false, error: action.payload, rows: [] };

    default:
      return state;
  }
};
