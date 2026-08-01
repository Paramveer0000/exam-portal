import { useEffect, useRef } from "react";
import { useSelector, useDispatch } from "react-redux";
import authServices from "../services/authServices";
import * as authConstants from "../constants/authConstants";

const IDLE_TIMEOUT_MS = 30 * 60 * 1000; // 30 minutes

const IdleTimeout = () => {
  const { loggedIn } = useSelector((state) => state.loginReducer);
  const dispatch = useDispatch();
  const timerRef = useRef(null);

  useEffect(() => {
    if (!loggedIn) return;

    const resetTimer = () => {
      if (timerRef.current) clearTimeout(timerRef.current);
      timerRef.current = setTimeout(async () => {
        await authServices.logout();
        dispatch({ type: authConstants.USER_LOGOUT });
        window.location.href = "/login?expired=1";
      }, IDLE_TIMEOUT_MS);
    };

    const events = ["mousedown", "keydown", "scroll", "touchstart"];
    events.forEach((e) => window.addEventListener(e, resetTimer));
    resetTimer();

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
      events.forEach((e) => window.removeEventListener(e, resetTimer));
    };
  }, [loggedIn, dispatch]);

  return null;
};

export default IdleTimeout;
