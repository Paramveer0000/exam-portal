import axios from "axios";

const register = async (user) => {
  try {
    const { data } = await axios.post("/api/register", user);
    if (data && data.userId) {
      console.log(
        "authService:register() Success: ",
        user.username,
        " successfully registerd."
      );
      return { isRegistered: true, error: null };
    } else {
      console.error("authService:register() Error: ", data);
      return { isRegistered: false, error: data };
    }
  } catch (error) {
    const message =
      (error.response && error.response.data && error.response.data.message) ||
      (error.response && error.response.statusText) ||
      "Registration failed";
    console.error("authService:register() Error: ", message);
    return { isRegistered: false, error: message };
  }
};

const login = async (username, password) => {
  try {
    const { data } = await axios.post("/api/login", {
      username: username,
      password: password,
    });

    if (data && data.jwtToken.length) {
      localStorage.setItem("user", JSON.stringify(data.user));
      localStorage.setItem("jwtToken", JSON.stringify(data.jwtToken));
      console.log("authService:login() Success: ", data.user);
      return data;
    } else {
      console.error("authService:login() Error: ", data);
      return { error: "Login failed" };
    }
  } catch (error) {
    const message =
      (error.response && error.response.data && error.response.data.message) ||
      (error.response && error.response.statusText) ||
      "Login failed";
    console.error("authService:login() Error: ", message);
    return { error: message };
  }
};

// Public School (admin) self-signup.
const registerSchool = async (school) => {
  try {
    const { data } = await axios.post("/api/register/school", school);
    if (data && data.userId) return { isRegistered: true, error: null };
    return { isRegistered: false, error: "Signup failed" };
  } catch (error) {
    const message =
      (error.response && error.response.data && error.response.data.message) ||
      (error.response && error.response.statusText) ||
      "Signup failed";
    return { isRegistered: false, error: message };
  }
};

// Public list of teachers (schools) for the registration page.
const getTeachers = async () => {
  try {
    const { data } = await axios.get("/api/teachers");
    return data;
  } catch (error) {
    console.error("authService:getTeachers() Error");
    return [];
  }
};

const authServices = { register, registerSchool, login, getTeachers };
export default authServices;
