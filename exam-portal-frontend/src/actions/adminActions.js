import * as adminConstants from "../constants/adminConstants";
import adminServices from "../services/adminServices";

export const fetchAdmins = async (dispatch, token) => {
  dispatch({ type: adminConstants.FETCH_ADMINS_REQUEST });
  const { data, error } = await adminServices.fetchAdmins(token);
  if (data) {
    return dispatch({ type: adminConstants.FETCH_ADMINS_SUCCESS, payload: data });
  }
  return dispatch({ type: adminConstants.FETCH_ADMINS_FAILURE, payload: error });
};

export const createAdmin = async (dispatch, admin, token) => {
  dispatch({ type: adminConstants.CREATE_ADMIN_REQUEST });
  const { data, isSaved, error } = await adminServices.createAdmin(admin, token);
  if (isSaved) {
    return dispatch({ type: adminConstants.CREATE_ADMIN_SUCCESS, payload: data });
  }
  return dispatch({ type: adminConstants.CREATE_ADMIN_FAILURE, payload: error });
};

export const updateAdmin = async (dispatch, adminId, admin, token) => {
  dispatch({ type: adminConstants.UPDATE_ADMIN_REQUEST });
  const { data, isSaved, error } = await adminServices.updateAdmin(
    adminId,
    admin,
    token
  );
  if (isSaved) {
    return dispatch({ type: adminConstants.UPDATE_ADMIN_SUCCESS, payload: data });
  }
  return dispatch({ type: adminConstants.UPDATE_ADMIN_FAILURE, payload: error });
};

export const setAdminStatus = async (dispatch, adminId, active, token) => {
  const { data, error } = await adminServices.setAdminStatus(
    adminId,
    active,
    token
  );
  if (data) {
    return dispatch({
      type: adminConstants.SET_ADMIN_STATUS_SUCCESS,
      payload: data,
    });
  }
  return dispatch({
    type: adminConstants.SET_ADMIN_STATUS_FAILURE,
    payload: error,
  });
};

export const resetAdminPassword = async (
  dispatch,
  adminId,
  newPassword,
  token
) => {
  const { isReset, error } = await adminServices.resetPassword(
    adminId,
    newPassword,
    token
  );
  return dispatch({
    type: isReset
      ? adminConstants.RESET_ADMIN_PASSWORD_SUCCESS
      : adminConstants.RESET_ADMIN_PASSWORD_FAILURE,
    payload: isReset ? adminId : error,
  });
};

export const deleteAdmin = async (dispatch, adminId, token) => {
  const { isDeleted, error } = await adminServices.deleteAdmin(adminId, token);
  if (isDeleted) {
    return dispatch({
      type: adminConstants.DELETE_ADMIN_SUCCESS,
      payload: adminId,
    });
  }
  return dispatch({ type: adminConstants.DELETE_ADMIN_FAILURE, payload: error });
};

export const fetchMetrics = async (dispatch, token) => {
  const { data, error } = await adminServices.fetchMetrics(token);
  if (data) {
    return dispatch({
      type: adminConstants.FETCH_METRICS_SUCCESS,
      payload: data,
    });
  }
  return dispatch({ type: adminConstants.FETCH_METRICS_FAILURE, payload: error });
};

export const fetchAnalytics = async (dispatch, token) => {
  const { data, error } = await adminServices.fetchAnalytics(token);
  if (data) {
    return dispatch({
      type: adminConstants.FETCH_ANALYTICS_SUCCESS,
      payload: data,
    });
  }
  return dispatch({
    type: adminConstants.FETCH_ANALYTICS_FAILURE,
    payload: error,
  });
};

export const fetchActivity = async (dispatch, adminId, token) => {
  const { data, error } = await adminServices.fetchActivity(adminId, token);
  if (data) {
    return dispatch({
      type: adminConstants.FETCH_ADMIN_ACTIVITY_SUCCESS,
      payload: data,
    });
  }
  return dispatch({
    type: adminConstants.FETCH_ADMIN_ACTIVITY_FAILURE,
    payload: error,
  });
};

export const fetchUnowned = async (dispatch, token) => {
  const { data, error } = await adminServices.fetchUnowned(token);
  if (data) {
    return dispatch({
      type: adminConstants.FETCH_UNOWNED_SUCCESS,
      payload: data,
    });
  }
  return dispatch({ type: adminConstants.FETCH_UNOWNED_FAILURE, payload: error });
};

export const reassignUnowned = async (dispatch, adminId, token) => {
  const { data, error } = await adminServices.reassignUnowned(adminId, token);
  if (data) {
    return dispatch({
      type: adminConstants.REASSIGN_UNOWNED_SUCCESS,
      payload: data,
    });
  }
  return dispatch({
    type: adminConstants.REASSIGN_UNOWNED_FAILURE,
    payload: error,
  });
};
