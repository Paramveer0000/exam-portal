import axios from "axios";

const fetchReport = async (quizResId, token) => {
  try {
    const config = {
      headers: { Authorization: `Bearer ${token}` },
    };
    const { data } = await axios.get(
      `/api/psychometric-report/${quizResId}`,
      config
    );
    return { data, error: null };
  } catch (error) {
    const message =
      (error.response && error.response.data && error.response.data.message) ||
      (error.response && error.response.statusText) ||
      "Request failed";
    console.error("psychometricReportServices:fetchReport() Error: ", message);
    return { data: null, error: message };
  }
};

const psychometricReportServices = { fetchReport };
export default psychometricReportServices;
