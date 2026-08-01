import * as adminConstants from "../constants/adminConstants";
import adminServices from "../services/adminServices";

export const fetchAdmins = async (dispatch) => {
  dispatch({ type: adminConstants.FETCH_ADMINS_REQUEST });
  const { data, error } = await adminServices.fetchAdmins();
  if (data) {
    return dispatch({ type: adminConstants.FETCH_ADMINS_SUCCESS, payload: data });
  }
  return dispatch({ type: adminConstants.FETCH_ADMINS_FAILURE, payload: error });
};

export const createAdmin = async (dispatch, admin) => {
  dispatch({ type: adminConstants.CREATE_ADMIN_REQUEST });
  const { data, isSaved, error } = await adminServices.createAdmin(admin);
  if (isSaved) {
    return dispatch({ type: adminConstants.CREATE_ADMIN_SUCCESS, payload: data });
  }
  return dispatch({ type: adminConstants.CREATE_ADMIN_FAILURE, payload: error });
};

export const updateAdmin = async (dispatch, adminId, admin) => {
  dispatch({ type: adminConstants.UPDATE_ADMIN_REQUEST });
  const { data, isSaved, error } = await adminServices.updateAdmin(adminId, admin);
  if (isSaved) {
    return dispatch({ type: adminConstants.UPDATE_ADMIN_SUCCESS, payload: data });
  }
  return dispatch({ type: adminConstants.UPDATE_ADMIN_FAILURE, payload: error });
};

export const setAdminStatus = async (dispatch, adminId, active) => {
  const { data, error } = await adminServices.setAdminStatus(adminId, active);
  if (data) {
    return dispatch({ type: adminConstants.SET_ADMIN_STATUS_SUCCESS, payload: data });
  }
  return dispatch({ type: adminConstants.SET_ADMIN_STATUS_FAILURE, payload: error });
};

export const resetAdminPassword = async (dispatch, adminId, newPassword) => {
  const { isReset, error } = await adminServices.resetPassword(adminId, newPassword);
  return dispatch({
    type: isReset ? adminConstants.RESET_ADMIN_PASSWORD_SUCCESS : adminConstants.RESET_ADMIN_PASSWORD_FAILURE,
    payload: isReset ? adminId : error,
  });
};

export const deleteAdmin = async (dispatch, adminId) => {
  const { isDeleted, error } = await adminServices.deleteAdmin(adminId);
  if (isDeleted) {
    return dispatch({ type: adminConstants.DELETE_ADMIN_SUCCESS, payload: adminId });
  }
  return dispatch({ type: adminConstants.DELETE_ADMIN_FAILURE, payload: error });
};

export const fetchMetrics = async (dispatch) => {
  const { data, error } = await adminServices.fetchMetrics();
  if (data) {
    return dispatch({ type: adminConstants.FETCH_METRICS_SUCCESS, payload: data });
  }
  return dispatch({ type: adminConstants.FETCH_METRICS_FAILURE, payload: error });
};

export const fetchAnalytics = async (dispatch) => {
  const { data, error } = await adminServices.fetchAnalytics();
  if (data) {
    return dispatch({ type: adminConstants.FETCH_ANALYTICS_SUCCESS, payload: data });
  }
  return dispatch({ type: adminConstants.FETCH_ANALYTICS_FAILURE, payload: error });
};

export const fetchActivity = async (dispatch, adminId) => {
  const { data, error } = await adminServices.fetchActivity(adminId);
  if (data) {
    return dispatch({ type: adminConstants.FETCH_ADMIN_ACTIVITY_SUCCESS, payload: data });
  }
  return dispatch({ type: adminConstants.FETCH_ADMIN_ACTIVITY_FAILURE, payload: error });
};

export const fetchUnowned = async (dispatch) => {
  const { data, error } = await adminServices.fetchUnowned();
  if (data) {
    return dispatch({ type: adminConstants.FETCH_UNOWNED_SUCCESS, payload: data });
  }
  return dispatch({ type: adminConstants.FETCH_UNOWNED_FAILURE, payload: error });
};

export const reassignUnowned = async (dispatch, adminId) => {
  const { data, error } = await adminServices.reassignUnowned(adminId);
  if (data) {
    return dispatch({ type: adminConstants.REASSIGN_UNOWNED_SUCCESS, payload: data });
  }
  return dispatch({ type: adminConstants.REASSIGN_UNOWNED_FAILURE, payload: error });
};
