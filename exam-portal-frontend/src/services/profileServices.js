import axios from "axios";

const authConfig = (token) => ({
  headers: { Authorization: `Bearer ${token}` },
});

const errText = (error) =>
  (error.response && error.response.data && error.response.data.message) ||
  (error.response && error.response.statusText) ||
  "Request failed";

// Returns { user, jwtToken } on success so the caller can refresh the session.
const updateProfile = async (profile, token) => {
  try {
    const { data } = await axios.put("/api/profile/", profile, authConfig(token));
    return { data, error: null };
  } catch (error) {
    return { data: null, error: errText(error) };
  }
};

const changePassword = async (payload, token) => {
  try {
    await axios.post("/api/profile/change-password", payload, authConfig(token));
    return { ok: true, error: null };
  } catch (error) {
    return { ok: false, error: errText(error) };
  }
};

const profileServices = { updateProfile, changePassword };
export default profileServices;
