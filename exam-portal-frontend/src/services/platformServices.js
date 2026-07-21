import axios from "axios";

// Public: the platform company logo shown in the header for everyone.
const getBranding = async () => {
  try {
    const { data } = await axios.get("/api/platform/branding");
    return { companyLogo: data.companyLogo || null, error: null };
  } catch (error) {
    return { companyLogo: null, error: "Could not load branding" };
  }
};

// SUPER_ADMIN only. Pass null companyLogo to clear it.
const updateBranding = async (companyLogo, token) => {
  try {
    const { data } = await axios.put(
      "/api/platform/branding",
      { companyLogo },
      { headers: { Authorization: `Bearer ${token}` } }
    );
    return { companyLogo: data.companyLogo || null, error: null };
  } catch (error) {
    const message =
      (error.response && error.response.data && error.response.data.message) ||
      (error.response && error.response.statusText) ||
      "Request failed";
    return { companyLogo: null, error: message };
  }
};

const platformServices = { getBranding, updateBranding };
export default platformServices;
