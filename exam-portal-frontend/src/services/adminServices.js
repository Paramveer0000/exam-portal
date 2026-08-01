import api from "./api";

const errText = (error) =>
  error.response && error.response.data
    ? error.response.data.message || error.response.statusText
    : "Request failed";

const fetchAdmins = async () => {
  try {
    const { data } = await api.get("/api/admin/");
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const createAdmin = async (admin) => {
  try {
    const { data } = await api.post("/api/admin/", admin);
    return { data, isSaved: true, error: null };
  } catch (error) {
    return { data: null, isSaved: false, error: errText(error) };
  }
};

const updateAdmin = async (adminId, admin) => {
  try {
    const { data } = await api.put(`/api/admin/${adminId}`, admin);
    return { data, isSaved: true, error: null };
  } catch (error) {
    return { data: null, isSaved: false, error: errText(error) };
  }
};

const setAdminStatus = async (adminId, active) => {
  try {
    const { data } = await api.patch(`/api/admin/${adminId}/status?active=${active}`);
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const resetPassword = async (adminId, newPassword) => {
  try {
    await api.post(`/api/admin/${adminId}/reset-password`, { newPassword });
    return { isReset: true, error: null };
  } catch (error) {
    return { isReset: false, error: errText(error) };
  }
};

const deleteAdmin = async (adminId) => {
  try {
    await api.delete(`/api/admin/${adminId}`);
    return { isDeleted: true, error: null };
  } catch (error) {
    return { isDeleted: false, error: errText(error) };
  }
};

const fetchMetrics = async () => {
  try {
    const { data } = await api.get("/api/admin/metrics");
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const fetchAnalytics = async () => {
  try {
    const { data } = await api.get("/api/admin/analytics");
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const fetchActivity = async (adminId) => {
  try {
    const { data } = await api.get(`/api/admin/${adminId}/activity`);
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const fetchUnowned = async () => {
  try {
    const { data } = await api.get("/api/admin/unowned");
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const reassignUnowned = async (adminId) => {
  try {
    const { data } = await api.post(`/api/admin/${adminId}/reassign-unowned`);
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const fetchResultsBySchool = async () => {
  try {
    const { data } = await api.get("/api/admin/results-by-school");
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

// A school's own students' results grouped by class.
const fetchResultsByClass = async () => {
  try {
    const { data } = await api.get("/api/quizResult/by-class");
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const impersonate = async (adminId) => {
  try {
    const { data } = await api.post(`/api/admin/${adminId}/impersonate`);
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const stopImpersonation = async (originalUserId) => {
  try {
    const { data } = await api.post(`/api/admin/stop-impersonation?originalUserId=${originalUserId}`);
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const adminServices = {
  fetchAdmins,
  createAdmin,
  updateAdmin,
  setAdminStatus,
  resetPassword,
  deleteAdmin,
  fetchMetrics,
  fetchAnalytics,
  fetchActivity,
  fetchUnowned,
  reassignUnowned,
  fetchResultsBySchool,
  fetchResultsByClass,
  impersonate,
  stopImpersonation,
};

export default adminServices;
