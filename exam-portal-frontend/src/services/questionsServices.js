import api from "./api";

const fetchQuestionsByQuiz = async (quizId) => {
  try {
    const { data } = await api.get(`/api/question/?quizId=${quizId}`);
    return data;
  } catch (error) {
    return error.response ? error.response.statusText : "Request failed";
  }
};

const fetchExamQuestions = async (quizId) => {
  try {
    const { data } = await api.get(`/api/quiz/${quizId}/exam`);
    return data;
  } catch (error) {
    return error.response ? error.response.statusText : "Request failed";
  }
};

const addQuestion = async (question) => {
  try {
    const { data } = await api.post("/api/question/", question);
    return { data: data, isAdded: true, error: null };
  } catch (error) {
    const message = error.response?.data?.message || error.response?.statusText || "Request failed";
    return { data: null, isAdded: false, error: message };
  }
};

const deleteQuestion = async (quesId) => {
  try {
    await api.delete(`/api/question/${quesId}`);
    return { isDeleted: true, error: null };
  } catch (error) {
    return {
      isDeleted: false,
      error: error.response ? error.response.statusText : "Request failed",
    };
  }
};

const updateQuestion = async (question) => {
  try {
    const { data } = await api.put(`/api/question/${question.quesId}`, question);
    return { data: data, isUpdated: true, error: null };
  } catch (error) {
    const message = error.response?.data?.message || error.response?.statusText || "Request failed";
    return { data: null, isUpdated: false, error: message };
  }
};

const questionsServices = {
  fetchQuestionsByQuiz,
  fetchExamQuestions,
  addQuestion,
  deleteQuestion,
  updateQuestion,
};
export default questionsServices;
