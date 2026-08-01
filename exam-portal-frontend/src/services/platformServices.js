import api from "./api";

const getBranding = async () => {
  try {
    const { data } = await api.get("/api/platform/branding");
    return { companyLogo: data.companyLogo || null, error: null };
  } catch (error) {
    return { companyLogo: null, error: "Could not load branding" };
  }
};

const updateBranding = async (companyLogo) => {
  try {
    const { data } = await api.put("/api/platform/branding", { companyLogo });
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
