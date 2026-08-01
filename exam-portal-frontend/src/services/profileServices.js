import api from "./api";

const errText = (error) =>
  (error.response && error.response.data && error.response.data.message) ||
  (error.response && error.response.statusText) ||
  "Request failed";

const updateProfile = async (profile) => {
  try {
    const { data } = await api.put("/api/profile/", profile);
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const changePassword = async (payload) => {
  try {
    await api.post("/api/profile/change-password", payload);
    return { ok: true, error: null };
  } catch (error) {
    return { ok: false, error: errText(error) };
  }
};

const profileServices = { updateProfile, changePassword };
export default profileServices;
