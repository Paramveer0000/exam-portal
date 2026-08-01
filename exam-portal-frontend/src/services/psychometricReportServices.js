import api from "./api";

const fetchReport = async (quizResId) => {
  try {
    const { data } = await api.get(`/api/psychometric-report/${quizResId}`);
    return { data, error: null };
  } catch (error) {
    const message =
      (error.response && error.response.data && error.response.data.message) ||
      (error.response && error.response.statusText) ||
      "Request failed";
    return { data: null, error: message };
  }
};

const psychometricReportServices = { fetchReport };
export default psychometricReportServices;
