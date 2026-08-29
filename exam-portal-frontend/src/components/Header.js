import React, { useEffect, useState } from "react";
import { Navbar, Nav, Container } from "react-bootstrap";
import { useSelector, useDispatch } from "react-redux";
import { LinkContainer } from "react-router-bootstrap";
import { useLocation, useNavigate } from "react-router-dom";
import platformServices from "../services/platformServices";
import adminServices from "../services/adminServices";
import { homePathForRoles } from "./ProtectedRoute";
import { logout } from "../actions/authActions";
import useTheme, { logoForTheme } from "../hooks/useTheme";
import swal from "sweetalert";
import * as authConstants from "../constants/authConstants";

const Header = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch();
  const loginReducer = useSelector((state) => state.loginReducer);
  const [isLoggedIn, setIsLoggedIn] = useState(loginReducer.loggedIn);
  const [companyLogo, setCompanyLogo] = useState(null);
  const [returningToSuperAdmin, setReturningToSuperAdmin] = useState(false);
  const theme = useTheme();

  const isImpersonating = !!localStorage.getItem("impersonatorBackup");

  useEffect(() => {
    platformServices.getBranding().then(({ companyLogo }) =>
      setCompanyLogo(companyLogo)
    );
  }, []);

  // The brand acts as a dashboard button when logged in; otherwise it goes to
  // the public landing page (not back to the login form).
  const goHome = () => {
    try {
      const user = JSON.parse(localStorage.getItem("user"));
      const roles = user && user.roles ? user.roles.map((r) => r.roleName) : [];
      navigate(user ? homePathForRoles(roles) : "/");
    } catch (e) {
      navigate("/");
    }
  };

  const logoutHandler = async () => {
    setIsLoggedIn(false);
    await logout(dispatch);
    navigate("/login");
  };

  const returnToSuperAdmin = async () => {
    if (returningToSuperAdmin) return;
    let backup;
    try {
      backup = JSON.parse(localStorage.getItem("impersonatorBackup"));
    } catch (_) {
      backup = null;
    }
    if (!backup) {
      localStorage.removeItem("impersonatorBackup");
      await swal("Could not return", "Session recovery data is missing. Please sign in again.", "error");
      return;
    }

    setReturningToSuperAdmin(true);
    const { data, error } = await adminServices.stopImpersonation();
    if (!data || !data.user) {
      setReturningToSuperAdmin(false);
      await swal(
        "Could not return",
        error || "Could not restore the super-admin session",
        "error"
      );
      return;
    }

    localStorage.setItem("user", JSON.stringify(data.user));
    localStorage.removeItem("impersonatorBackup");
    dispatch({ type: authConstants.USER_LOGIN_SUCCESS, payload: data.user });
    navigate("/superadmin", { replace: true });
  };

  useEffect(() => {
    if (loginReducer.loggedIn) {
      setIsLoggedIn(true);
    }
  }, [loginReducer.loggedIn]);

  // Listen for session-expired events from the axios interceptor.
  useEffect(() => {
    const handler = () => {
      setIsLoggedIn(false);
      dispatch({ type: "USER_LOGOUT" });
    };
    window.addEventListener("auth:session-expired", handler);
    return () => window.removeEventListener("auth:session-expired", handler);
  }, [dispatch]);

  // The public landing page ships its own fixed navbar; rendering this one too
  // stacks two brands at top:0 and buries its links under the landing nav.
  if (location.pathname === "/") return null;

  return (
    <header>
      {/* The bar follows the theme too: a white-background logo on a
          permanently dark bar would read as a white block. */}
      <Navbar
        bg={theme === "light" ? "light" : "dark"}
        variant={theme === "light" ? "light" : "dark"}
        expand="lg"
        collapseOnSelect
      >
        <Container>
            <Navbar.Brand
              onClick={goHome}
              style={{ cursor: "pointer", display: "flex", alignItems: "center" }}
              title="Go to dashboard"
            >
              {/* An uploaded logo wins so white-labelling still works; with
                  none, fall back to the variant that suits the theme.
                  Clipped to a circle: every variant (and any uploaded PNG)
                  carries square corners in its own background colour, which
                  read as a pale block against the bar. */}
              <img
                src={companyLogo || logoForTheme(theme)}
                alt="The Mentalist - go to dashboard"
                style={{
                  height: "44px",
                  width: "44px",
                  objectFit: "cover",
                  borderRadius: "50%",
                }}
              />
            </Navbar.Brand>

          <Navbar.Toggle aria-controls="responsive-navbar-nav" />
          <Navbar.Collapse id="responsive-navbar-nav">
            <Nav className="justify-content-end flex-grow-1 pe-3">
              {isImpersonating && (
                <Nav.Link
                  onClick={returnToSuperAdmin}
                  disabled={returningToSuperAdmin}
                  style={{ color: "#ffc107", fontWeight: 600 }}
                >
                  {returningToSuperAdmin ? "Returning…" : "← Return to Super Admin"}
                </Nav.Link>
              )}
              {isLoggedIn ? (
                  <Nav.Link>{loginReducer.user && loginReducer.user.firstName}</Nav.Link>
              ) : (
                <LinkContainer to="/login">
                  <Nav.Link>Login</Nav.Link>
                </LinkContainer>
              )}

              {isLoggedIn ? (
                <LinkContainer to="/login">
                  <Nav.Link onClick={logoutHandler}>Logout</Nav.Link>
                </LinkContainer>
              ) : (
                <LinkContainer to="/register">
                  <Nav.Link>Register</Nav.Link>
                </LinkContainer>
              )}
            </Nav>
          </Navbar.Collapse>
        </Container>
      </Navbar>
    </header>
  );
};

export default Header;
