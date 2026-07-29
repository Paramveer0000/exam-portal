import axios from "axios";

const submitQuiz = async (quizId, answers, token) => {
  try {
    const config = {
      headers: { Authorization: `Bearer ${token}` },
    };
    // The backend derives the user from the JWT; no userId is sent.
    const { data } = await axios.post(
      `/api/quizResult/submit/?quizId=${quizId}`,
      answers,
      config
    );
    return { data: data, isAdded: true, error: null };
  } catch (error) {
    console.error(
      "quizResultServices:submitQuiz() Error: ",
      error.response.statusText
    );
    return { data: null, isAdded: false, error: error.response.statusText };
  }
};

const fetchQuizResult = async (userId, token) => {
  try {
    const config = {
      headers: { Authorization: `Bearer ${token}` },
    };
    let data = null;
    if (userId) {
      data = await axios.get(
        `/api/quizResult/?userId=${userId}`,
        config
      );
    }
    else {
      data = await axios.get(
        `/api/quizResult/all`,
        config
      );
    }
    return data.data;
  } catch (error) {
    console.error(
      "quizResultServices:fetchQuizResult() Error: ",
      error.response.statusText
    );
    return null;
  }
};


const quizResultServices = { submitQuiz, fetchQuizResult };

export default quizResultServices;
