import api from "./api";

const register = async (user) => {
  try {
    const { data } = await api.post("/api/register", user);
    if (data && data.userId) {
      return { isRegistered: true, error: null };
    } else {
      return { isRegistered: false, error: data };
    }
  } catch (error) {
    const message =
      (error.response && error.response.data && error.response.data.message) ||
      (error.response && error.response.statusText) ||
      "Registration failed";
    return { isRegistered: false, error: message };
  }
};

const login = async (username, password) => {
  try {
    const { data } = await api.post("/api/login", {
      username: username,
      password: password,
    });

    if (data && data.user) {
      // Store user info for UI state (no token — it's in HttpOnly cookie).
      localStorage.setItem("user", JSON.stringify(data.user));
      return data;
    } else {
      return { error: "Login failed" };
    }
  } catch (error) {
    const message =
      (error.response && error.response.data && error.response.data.message) ||
      (error.response && error.response.statusText) ||
      "Login failed";
    return { error: message };
  }
};

const registerSchool = async (school) => {
  try {
    const { data } = await api.post("/api/register/school", school);
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

const getTeachers = async () => {
  try {
    const { data } = await api.get("/api/teachers");
    return data;
  } catch (error) {
    return [];
  }
};

const logout = async () => {
  try {
    await api.post("/api/logout");
  } catch (error) {
    // Logout best-effort; clear client state regardless.
  }
  localStorage.removeItem("user");
  localStorage.removeItem("impersonatorBackup");
};

const refreshSession = async () => {
  try {
    const { data } = await api.post("/api/refresh");
    if (data && data.user) {
      localStorage.setItem("user", JSON.stringify(data.user));
    }
    return data;
  } catch (error) {
    return null;
  }
};

const getCurrentUser = async () => {
  try {
    const { data } = await api.get("/api/me");
    return data;
  } catch (error) {
    return null;
  }
};

const authServices = { register, registerSchool, login, logout, getTeachers, refreshSession, getCurrentUser };
export default authServices;
