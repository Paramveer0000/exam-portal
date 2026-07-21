import axios from "axios";

const authConfig = (token) => ({
  headers: { Authorization: `Bearer ${token}` },
});

const errText = (error) =>
  (error.response && error.response.data && error.response.data.message) ||
  (error.response && error.response.statusText) ||
  "Request failed";

// SUPER_ADMIN: read platform AI settings (never includes the raw key).
const getSettings = async (token) => {
  try {
    const { data } = await axios.get("/api/admin/ai-settings", authConfig(token));
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

// SUPER_ADMIN: update settings. Leave apiKey blank to keep the stored key.
const updateSettings = async (payload, token) => {
  try {
    const { data } = await axios.put(
      "/api/admin/ai-settings",
      payload,
      authConfig(token)
    );
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

// SUPER_ADMIN: fire one real completion against the saved key to confirm it works.
const testSettings = async (token) => {
  try {
    const { data } = await axios.post("/api/admin/ai-settings/test", {}, authConfig(token));
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

// Student/teacher: generate (or fetch cached) AI narrative for a report.
const generateSummary = async (quizResId, token, regenerate = false) => {
  try {
    const { data } = await axios.post(
      `/api/psychometric-report/${quizResId}/ai-summary?regenerate=${regenerate}`,
      {},
      authConfig(token)
    );
    return { summary: data.summary, error: null };
  } catch (error) {
    return { summary: null, error: errText(error) };
  }
};

const aiServices = { getSettings, updateSettings, testSettings, generateSummary };
export default aiServices;
