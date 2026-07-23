import axios from "axios";

const authConfig = (token) => ({ headers: { Authorization: `Bearer ${token}` } });

const errText = (error) =>
  (error.response && error.response.data && error.response.data.message) ||
  (error.response && error.response.statusText) ||
  "Request failed";

// classId optional: when given, only subjects under that class.
const fetchSubjects = async (token, classId) => {
  try {
    const url = classId ? `/api/subject/?classId=${classId}` : "/api/subject/";
    const { data } = await axios.get(url, authConfig(token));
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const addSubject = async (subject, token) => {
  try {
    const { data } = await axios.post("/api/subject/", subject, authConfig(token));
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const updateSubject = async (subject, token) => {
  try {
    const { data } = await axios.put(
      `/api/subject/${subject.subjectId}`,
      subject,
      authConfig(token)
    );
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const deleteSubject = async (subjectId, token) => {
  try {
    await axios.delete(`/api/subject/${subjectId}`, authConfig(token));
    return { ok: true, error: null };
  } catch (error) {
    return { ok: false, error: errText(error) };
  }
};

const subjectsServices = { fetchSubjects, addSubject, updateSubject, deleteSubject };
export default subjectsServices;
