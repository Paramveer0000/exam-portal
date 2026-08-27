import axios from "axios";

const api = axios.create({
  withCredentials: true,
});

// Track if a token refresh is in flight to avoid duplicate refresh calls.
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve();
    }
  });
  failedQueue = [];
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // If 401 and not already retrying, try a token refresh.
    if (
      error.response &&
      error.response.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url.includes("/api/login") &&
      !originalRequest.url.includes("/api/refresh")
    ) {
      if (isRefreshing) {
        // Queue this request until the refresh completes.
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(() => api(originalRequest));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        await api.post("/api/refresh");
        processQueue(null);
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError);
        // Refresh failed: session expired. Clear state and redirect.
        localStorage.removeItem("user");
        localStorage.removeItem("impersonatorBackup");
        // Dispatch a custom event so Redux can react.
        window.dispatchEvent(new Event("auth:session-expired"));
        if (
          window.location.pathname !== "/login" &&
          window.location.pathname !== "/" &&
          window.location.pathname !== "/register"
        ) {
          window.location.href = "/login?expired=1";
        }
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default api;
