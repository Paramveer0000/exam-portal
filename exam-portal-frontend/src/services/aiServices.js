import api from "./api";

const errText = (error) =>
  (error.response && error.response.data && error.response.data.message) ||
  (error.response && error.response.statusText) ||
  "Request failed";

const getSettings = async () => {
  try {
    const { data } = await api.get("/api/admin/ai-settings");
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const updateSettings = async (payload) => {
  try {
    const { data } = await api.put("/api/admin/ai-settings", payload);
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const testSettings = async () => {
  try {
    const { data } = await api.post("/api/admin/ai-settings/test");
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const generateSummary = async (quizResId, regenerate = false) => {
  try {
    const { data } = await api.post(
      `/api/psychometric-report/${quizResId}/ai-summary?regenerate=${regenerate}`
    );
    return { summary: data.summary, error: null };
  } catch (error) {
    return { summary: null, error: errText(error) };
  }
};

const aiServices = { getSettings, updateSettings, testSettings, generateSummary };
export default aiServices;
