import api from "./api";

const fetchQuizzes = async (catId) => {
  try {
    const url = catId === null ? "/api/quiz/" : `/api/quiz/?catId=${catId}`;
    const { data } = await api.get(url);
    return data;
  } catch (error) {
    return error.response ? error.response.statusText : "Request failed";
  }
};

const addQuiz = async (quiz) => {
  try {
    const { data } = await api.post("/api/quiz/", quiz);
    return { data: data, isAdded: true, error: null };
  } catch (error) {
    const message =
      (error.response && error.response.data && error.response.data.message) ||
      (error.response && error.response.statusText) ||
      "Request failed";
    return { data: null, isAdded: false, error: message };
  }
};

const deleteQuiz = async (quizId) => {
  try {
    await api.delete(`/api/quiz/${quizId}/`);
    return { isDeleted: true, error: null };
  } catch (error) {
    return {
      isDeleted: false,
      error: error.response ? error.response.statusText : "Request failed",
    };
  }
};

const updateQuiz = async (quiz) => {
  try {
    const { data } = await api.put(`/api/quiz/${quiz.quizId}/`, quiz);
    return { data: data, isUpdated: true, error: null };
  } catch (error) {
    return {
      data: null,
      isUpdated: false,
      error: error.response ? error.response.statusText : "Request failed",
    };
  }
};

const quizzesService = { fetchQuizzes, addQuiz, deleteQuiz, updateQuiz };
export default quizzesService;
