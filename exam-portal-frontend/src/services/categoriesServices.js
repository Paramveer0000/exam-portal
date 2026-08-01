import api from "./api";

const fetchCategories = async () => {
  try {
    const { data } = await api.get("/api/category/");
    return data;
  } catch (error) {
    return error.response ? error.response.statusText : "Request failed";
  }
};

const addCategory = async (category) => {
  try {
    const { data } = await api.post("/api/category/", category);
    return { data: data, isAdded: true, error: null };
  } catch (error) {
    const message =
      (error.response && error.response.data && error.response.data.message) ||
      (error.response && error.response.statusText) ||
      "Request failed";
    return { data: null, isAdded: false, error: message };
  }
};

const deleteCategory = async (catId) => {
  try {
    await api.delete(`/api/category/${catId}/`);
    return { isDeleted: true, error: null };
  } catch (error) {
    return {
      isDeleted: false,
      error: error.response ? error.response.statusText : "Request failed",
    };
  }
};

const updateCategory = async (category) => {
  try {
    const { data } = await api.put(`/api/category/${category.catId}/`, category);
    return { data: data, isUpdated: true, error: null };
  } catch (error) {
    return {
      data: null,
      isUpdated: false,
      error: error.response ? error.response.statusText : "Request failed",
    };
  }
};

const categoriesService = {
  addCategory,
  fetchCategories,
  updateCategory,
  deleteCategory,
};
export default categoriesService;
