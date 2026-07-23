import axios from "axios";

const authConfig = (token) => ({
  headers: { Authorization: `Bearer ${token}` },
});

const errText = (error) =>
  error.response && error.response.data
    ? error.response.data.message || error.response.statusText
    : "Request failed";

const fetchAdmins = async (token) => {
  try {
    const { data } = await axios.get("/api/admin/", authConfig(token));
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const createAdmin = async (admin, token) => {
  try {
    const { data } = await axios.post("/api/admin/", admin, authConfig(token));
    return { data, isSaved: true, error: null };
  } catch (error) {
    return { data: null, isSaved: false, error: errText(error) };
  }
};

const updateAdmin = async (adminId, admin, token) => {
  try {
    const { data } = await axios.put(
      `/api/admin/${adminId}`,
      admin,
      authConfig(token)
    );
    return { data, isSaved: true, error: null };
  } catch (error) {
    return { data: null, isSaved: false, error: errText(error) };
  }
};

const setAdminStatus = async (adminId, active, token) => {
  try {
    const { data } = await axios.patch(
      `/api/admin/${adminId}/status?active=${active}`,
      {},
      authConfig(token)
    );
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const resetPassword = async (adminId, newPassword, token) => {
  try {
    await axios.post(
      `/api/admin/${adminId}/reset-password`,
      { newPassword },
      authConfig(token)
    );
    return { isReset: true, error: null };
  } catch (error) {
    return { isReset: false, error: errText(error) };
  }
};

const deleteAdmin = async (adminId, token) => {
  try {
    await axios.delete(`/api/admin/${adminId}`, authConfig(token));
    return { isDeleted: true, error: null };
  } catch (error) {
    return { isDeleted: false, error: errText(error) };
  }
};

const fetchMetrics = async (token) => {
  try {
    const { data } = await axios.get("/api/admin/metrics", authConfig(token));
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const fetchAnalytics = async (token) => {
  try {
    const { data } = await axios.get("/api/admin/analytics", authConfig(token));
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const fetchActivity = async (adminId, token) => {
  try {
    const { data } = await axios.get(
      `/api/admin/${adminId}/activity`,
      authConfig(token)
    );
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const fetchUnowned = async (token) => {
  try {
    const { data } = await axios.get("/api/admin/unowned", authConfig(token));
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const reassignUnowned = async (adminId, token) => {
  try {
    const { data } = await axios.post(
      `/api/admin/${adminId}/reassign-unowned`,
      {},
      authConfig(token)
    );
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const fetchResultsBySchool = async (token) => {
  try {
    const { data } = await axios.get(
      "/api/admin/results-by-school",
      authConfig(token)
    );
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

// A school's own students' results grouped by class.
const fetchResultsByClass = async (token) => {
  try {
    const { data } = await axios.get("/api/quizResult/by-class", authConfig(token));
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

// Mints a token to act as the given admin (returns { user, jwtToken }).
const impersonate = async (adminId, token) => {
  try {
    const { data } = await axios.post(
      `/api/admin/${adminId}/impersonate`,
      {},
      authConfig(token)
    );
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
};

export default adminServices;
