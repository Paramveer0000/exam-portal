import * as adminConstants from "../constants/adminConstants";

const initialState = {
  loading: false,
  error: null,
  admins: [],
  metrics: null,
  analytics: [],
  activity: null,
  unowned: null,
};

export const adminReducer = (state = initialState, action) => {
  switch (action.type) {
    case adminConstants.FETCH_ADMINS_REQUEST:
    case adminConstants.CREATE_ADMIN_REQUEST:
    case adminConstants.UPDATE_ADMIN_REQUEST:
      return { ...state, loading: true, error: null };

    case adminConstants.FETCH_ADMINS_SUCCESS:
      return { ...state, loading: false, admins: action.payload };

    case adminConstants.CREATE_ADMIN_SUCCESS:
      return {
        ...state,
        loading: false,
        admins: [...state.admins, action.payload],
      };

    case adminConstants.UPDATE_ADMIN_SUCCESS:
    case adminConstants.SET_ADMIN_STATUS_SUCCESS:
      return {
        ...state,
        loading: false,
        admins: state.admins.map((a) =>
          a.userId === action.payload.userId ? action.payload : a
        ),
      };

    case adminConstants.DELETE_ADMIN_SUCCESS:
      return {
        ...state,
        admins: state.admins.filter((a) => a.userId !== action.payload),
      };

    case adminConstants.FETCH_METRICS_SUCCESS:
      return { ...state, metrics: action.payload };

    case adminConstants.FETCH_ANALYTICS_SUCCESS:
      return { ...state, analytics: action.payload };

    case adminConstants.FETCH_ADMIN_ACTIVITY_SUCCESS:
      return { ...state, activity: action.payload };

    case adminConstants.FETCH_UNOWNED_SUCCESS:
      return { ...state, unowned: action.payload };

    case adminConstants.FETCH_ADMINS_FAILURE:
    case adminConstants.CREATE_ADMIN_FAILURE:
    case adminConstants.UPDATE_ADMIN_FAILURE:
    case adminConstants.SET_ADMIN_STATUS_FAILURE:
    case adminConstants.DELETE_ADMIN_FAILURE:
    case adminConstants.RESET_ADMIN_PASSWORD_FAILURE:
    case adminConstants.FETCH_METRICS_FAILURE:
    case adminConstants.FETCH_ADMIN_ACTIVITY_FAILURE:
    case adminConstants.FETCH_UNOWNED_FAILURE:
    case adminConstants.REASSIGN_UNOWNED_FAILURE:
      return { ...state, loading: false, error: action.payload };

    default:
      return state;
  }
};
