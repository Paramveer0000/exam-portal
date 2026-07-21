import axios from "axios";

// Admin management view: full questions incl. answers. Admin-only on the backend.
const fetchQuestionsByQuiz = async (quizId, token) => {
  try {
    const config = {
      headers: { Authorization: `Bearer ${token}` },
    };
    const { data } = await axios.get(
      `/api/question/?quizId=${quizId}`,
      config
    );
    console.log("questionsServices:fetchQuestionsByQuiz() Success: ", data);
    return data;
  } catch (error) {
    console.error(
      "questionsServices:fetchQuestionsByQuiz() Error: ",
      error.response.statusText
    );
    return error.response.statusText;
  }
};

// Student exam view: a randomized/limited subset with correct answers stripped.
const fetchExamQuestions = async (quizId, token) => {
  try {
    const config = {
      headers: { Authorization: `Bearer ${token}` },
    };
    const { data } = await axios.get(`/api/quiz/${quizId}/exam`, config);
    console.log("questionsServices:fetchExamQuestions() Success: ", data);
    return data;
  } catch (error) {
    console.error(
      "questionsServices:fetchExamQuestions() Error: ",
      error.response.statusText
    );
    return error.response.statusText;
  }
};

const addQuestion = async (question, token) => {
  try {
    const config = {
      headers: { Authorization: `Bearer ${token}` },
    };

    const { data } = await axios.post("/api/question/", question, config);
    console.log("questionsServices:addQuestion()  Success: ", data);
    return { data: data, isAdded: true, error: null };
  } catch (error) {
    const message = error.response.data?.message || error.response.statusText;
    console.error("questionsServices:addQuestion()  Error: ", message);
    return { data: null, isAdded: false, error: message };
  }
};

const deleteQuestion = async (quesId, token) => {
  try {
    const config = {
      headers: { Authorization: `Bearer ${token}` },
    };
    const { data } = await axios.delete(`/api/question/${quesId}`, config);
    console.log("questionsServices:deleteQuestion() Success: ", data);
    return {
      isDeleted: true,
      error: null,
    };
  } catch (error) {
    console.error(
      "questionsServices:deleteQuestion() Error: ",
      error.response.statusText
    );
    return {
      isDeleted: false,
      error: error.response.statusText,
    };
  }
};

const updateQuestion = async (question, token) => {
  try {
    const config = {
      headers: { Authorization: `Bearer ${token}` },
    };
    const { data } = await axios.put(
      `/api/question/${question.quesId}`,
      question,
      config
    );
    console.log("questionsServices:updateQuestion() Success: ", data);
    return {
      data: data,
      isUpdated: true,
      error: null,
    };
  } catch (error) {
    const message = error.response.data?.message || error.response.statusText;
    console.error("questionsServices:updateQuestion() Error: ", message);
    return {
      data: null,
      isUpdated: false,
      error: message,
    };
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
