import api from "./api";

const filterQuery = ({ passed, from, to } = {}) => {
  const params = [];
  if (passed !== undefined && passed !== null && passed !== "")
    params.push(`passed=${passed}`);
  if (from) params.push(`from=${from}`);
  if (to) params.push(`to=${to}`);
  return params.length ? `?${params.join("&")}` : "";
};

const fetchQuizReport = async (quizId, filters) => {
  try {
    const { data } = await api.get(`/api/reports/quiz/${quizId}${filterQuery(filters)}`);
    return { data, error: null };
  } catch (error) {
    return {
      data: null,
      error: error.response ? error.response.statusText : "Request failed",
    };
  }
};

const fetchStudentReport = async (userId, filters) => {
  try {
    const { data } = await api.get(`/api/reports/student/${userId}${filterQuery(filters)}`);
    return { data, error: null };
  } catch (error) {
    return {
      data: null,
      error: error.response ? error.response.statusText : "Request failed",
    };
  }
};

const exportQuizReport = async (quizId, filters) => {
  try {
    const response = await api.get(
      `/api/reports/quiz/${quizId}/export${filterQuery(filters)}`,
      { responseType: "blob" }
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
