import api from "./api";

const submitQuiz = async (quizId, answers) => {
  try {
    const { data } = await api.post(`/api/quizResult/submit/?quizId=${quizId}`, answers);
    return { data: data, isAdded: true, error: null };
  } catch (error) {
    return {
      data: null,
      isAdded: false,
      error: error.response ? error.response.statusText : "Request failed",
    };
  }
};

const fetchQuizResult = async (userId) => {
  try {
    let response;
    if (userId) {
      response = await api.get(`/api/quizResult/?userId=${userId}`);
    } else {
      response = await api.get("/api/quizResult/all");
    }
    return response.data;
  } catch (error) {
    return null;
  }
};

const quizResultServices = { submitQuiz, fetchQuizResult };
export default quizResultServices;
