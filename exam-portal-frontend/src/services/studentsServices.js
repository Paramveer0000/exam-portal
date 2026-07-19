import axios from "axios";

const authConfig = (token) => ({
  headers: { Authorization: `Bearer ${token}` },
});

const errText = (error) =>
  (error.response && error.response.data && error.response.data.message) ||
  (error.response && error.response.statusText) ||
  "Request failed";

const fetchStudents = async (token) => {
  try {
    const { data } = await axios.get("/api/students/", authConfig(token));
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const setStatus = async (studentId, active, token) => {
  try {
    const { data } = await axios.patch(
      `/api/students/${studentId}/status?active=${active}`,
      {},
      authConfig(token)
    );
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const updateStudent = async (studentId, profile, token) => {
  try {
    const { data } = await axios.put(
      `/api/students/${studentId}`,
      profile,
      authConfig(token)
    );
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const resetPassword = async (studentId, newPassword, token) => {
  try {
    await axios.post(
      `/api/students/${studentId}/reset-password`,
      { newPassword },
      authConfig(token)
    );
    return { ok: true, error: null };
  } catch (error) {
    return { ok: false, error: errText(error) };
  }
};

const deleteStudent = async (studentId, token) => {
  try {
    await axios.delete(`/api/students/${studentId}`, authConfig(token));
    return { ok: true, error: null };
  } catch (error) {
    return { ok: false, error: errText(error) };
  }
};

const studentsServices = { fetchStudents, updateStudent, setStatus, resetPassword, deleteStudent };
export default studentsServices;
