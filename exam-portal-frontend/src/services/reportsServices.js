import axios from "axios";

const authConfig = (token) => ({
  headers: { Authorization: `Bearer ${token}` },
});

// Builds a query string from optional filters (passed, from, to).
const filterQuery = ({ passed, from, to } = {}) => {
  const params = [];
  if (passed !== undefined && passed !== null && passed !== "")
    params.push(`passed=${passed}`);
  if (from) params.push(`from=${from}`);
  if (to) params.push(`to=${to}`);
  return params.length ? `?${params.join("&")}` : "";
};

const fetchQuizReport = async (quizId, filters, token) => {
  try {
    const { data } = await axios.get(
      `/api/reports/quiz/${quizId}${filterQuery(filters)}`,
      authConfig(token)
    );
    return { data, error: null };
  } catch (error) {
    return {
      data: null,
      error: error.response ? error.response.statusText : "Request failed",
    };
  }
};

const fetchStudentReport = async (userId, filters, token) => {
  try {
    const { data } = await axios.get(
      `/api/reports/student/${userId}${filterQuery(filters)}`,
      authConfig(token)
    );
    return { data, error: null };
  } catch (error) {
    return {
      data: null,
      error: error.response ? error.response.statusText : "Request failed",
    };
  }
};

// Downloads the CSV export for a quiz report as a file in the browser.
const exportQuizReport = async (quizId, filters, token) => {
  try {
    const response = await axios.get(
      `/api/reports/quiz/${quizId}/export${filterQuery(filters)}`,
      { ...authConfig(token), responseType: "blob" }
    );
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", `quiz-${quizId}-report.csv`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
    return { ok: true, error: null };
  } catch (error) {
    return { ok: false, error: "Export failed" };
  }
};

const reportsServices = {
  fetchQuizReport,
  fetchStudentReport,
  exportQuizReport,
};

export default reportsServices;
