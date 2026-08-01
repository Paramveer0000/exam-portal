import api from "./api";

const errText = (error) =>
  (error.response && error.response.data && error.response.data.message) ||
  (error.response && error.response.statusText) ||
  "Request failed";

const fetchStudents = async () => {
  try {
    const { data } = await api.get("/api/students/");
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const setStatus = async (studentId, active) => {
  try {
    const { data } = await api.patch(`/api/students/${studentId}/status?active=${active}`);
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const updateStudent = async (studentId, profile) => {
  try {
    const { data } = await api.put(`/api/students/${studentId}`, profile);
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const resetPassword = async (studentId, newPassword) => {
  try {
    await api.post(`/api/students/${studentId}/reset-password`, { newPassword });
    return { ok: true, error: null };
  } catch (error) {
    return { ok: false, error: errText(error) };
  }
};

const deleteStudent = async (studentId) => {
  try {
    await api.delete(`/api/students/${studentId}`);
    return { ok: true, error: null };
  } catch (error) {
    return { ok: false, error: errText(error) };
  }
};

const createStudent = async (student) => {
  try {
    const { data } = await api.post("/api/students/", student);
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

// Set (or change) the student's single class.
const setClass = async (studentId, classId) => {
  try {
    const { data } = await api.put(`/api/students/${studentId}/class/${classId}`);
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const studentsServices = {
  fetchStudents,
  createStudent,
  updateStudent,
  setStatus,
  resetPassword,
  deleteStudent,
  setClass,
};
export default studentsServices;
