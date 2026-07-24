import axios from "axios";

const generateReport = async (quizResId, token, { counsellorName, counsellorRemarks, regenerate } = {}) => {
  try {
    const config = { headers: { Authorization: `Bearer ${token}` } };
    const { data } = await axios.post(
      `/api/mentalist-report/${quizResId}/generate?regenerate=${!!regenerate}`,
      { counsellorName, counsellorRemarks },
      config
    );
    return { data, error: null };
  } catch (error) {
    const message =
      (error.response && error.response.data && error.response.data.message) ||
      (error.response && error.response.statusText) ||
      "Request failed";
    console.error("mentalistReportServices:generateReport() Error: ", message);
    return { data: null, error: message };
  }
};

// Downloads the stored PDF and triggers a browser save-as, without the
// service having to know about DOM APIs beyond the blob it returns.
const downloadReport = async (quizResId, token) => {
  try {
    const config = {
      headers: { Authorization: `Bearer ${token}` },
      responseType: "blob",
    };
    const response = await axios.get(`/api/mentalist-report/${quizResId}/download`, config);
    return { blob: response.data, error: null };
  } catch (error) {
    return { blob: null, error: "Could not download the report" };
  }
};

const mentalistReportServices = { generateReport, downloadReport };
export default mentalistReportServices;
