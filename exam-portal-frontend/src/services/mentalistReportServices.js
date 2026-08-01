import api from "./api";

const generateReport = async (quizResId, { counsellorName, counsellorRemarks, regenerate } = {}) => {
  try {
    const { data } = await api.post(
      `/api/mentalist-report/${quizResId}/generate?regenerate=${!!regenerate}`,
      { counsellorName, counsellorRemarks }
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
const downloadReport = async (quizResId) => {
  try {
    const response = await api.get(`/api/mentalist-report/${quizResId}/download`, {
      responseType: "blob",
    });
    return { blob: response.data, error: null };
  } catch (error) {
    return { blob: null, error: "Could not download the report" };
  }
};

const mentalistReportServices = { generateReport, downloadReport };
export default mentalistReportServices;
