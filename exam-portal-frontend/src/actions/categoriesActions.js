import * as categoriesConstants from "../constants/categoriesConstants";
import categoriesServices from "../services/categoriesServices";

export const addCategory = async (dispatch, category) => {
  dispatch({ type: categoriesConstants.ADD_CATEGORY_REQUEST });
  const { data, isAdded, error } = await categoriesServices.addCategory(category);
  if (isAdded) {
    return dispatch({ type: categoriesConstants.ADD_CATEGORY_SUCCESS, payload: data });
  } else {
    return dispatch({ type: categoriesConstants.ADD_CATEGORY_FAILURE, payload: error });
  }
};

export const fetchCategories = async (dispatch) => {
  dispatch({ type: categoriesConstants.FETCH_CATEGORIES_REQUEST });
  const data = await categoriesServices.fetchCategories();
  if (data) {
    return dispatch({ type: categoriesConstants.FETCH_CATEGORIES_SUCCESS, payload: data });
  } else {
    return dispatch({ type: categoriesConstants.FETCH_CATEGORIES_FAILURE, payload: data });
  }
};

export const updateCategory = async (dispatch, category) => {
  dispatch({ type: categoriesConstants.UPDATE_CATEGORY_REQUEST });
  const { data, isUpdated, error } = await categoriesServices.updateCategory(category);
  if (isUpdated) {
    return dispatch({ type: categoriesConstants.UPDATE_CATEGORY_SUCCESS, payload: data });
  } else {
    return dispatch({ type: categoriesConstants.UPDATE_CATEGORY_FAILURE, payload: error });
  }
};

export const deleteCategory = async (dispatch, catId) => {
  dispatch({ type: categoriesConstants.DELETE_CATEGORY_REQUEST });
  const { isDeleted, error } = await categoriesServices.deleteCategory(catId);
  if (isDeleted) {
    return dispatch({ type: categoriesConstants.DELETE_CATEGORY_SUCCESS, payload: catId });
  } else {
    return dispatch({ type: categoriesConstants.DELETE_CATEGORY_FAILURE, payload: error });
  }
};
