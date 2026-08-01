import React, { useEffect, useState } from "react";
import { Navbar, Nav, Container } from "react-bootstrap";
import { useSelector, useDispatch } from "react-redux";
import { LinkContainer } from "react-router-bootstrap";
import { useLocation, useNavigate } from "react-router-dom";
import platformServices from "../services/platformServices";
import adminServices from "../services/adminServices";
import { homePathForRoles } from "./ProtectedRoute";
import { logout } from "../actions/authActions";

const Header = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch();
  const loginReducer = useSelector((state) => state.loginReducer);
  const [isLoggedIn, setIsLoggedIn] = useState(loginReducer.loggedIn);
  const [companyLogo, setCompanyLogo] = useState(null);

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
    const backup = JSON.parse(localStorage.getItem("impersonatorBackup"));
    if (backup) {
      const { data } = await adminServices.stopImpersonation(backup.userId);
      if (data) {
        localStorage.setItem("user", JSON.stringify(data.user));
      }
      localStorage.removeItem("impersonatorBackup");
    }
    window.location.href = "/superadmin";
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
      <Navbar bg="dark" variant="dark" expand="lg" collapseOnSelect>
        <Container>
            <Navbar.Brand
              onClick={goHome}
              style={{ cursor: "pointer", display: "flex", alignItems: "center" }}
              title="Go to dashboard"
            >
              {companyLogo ? (
                <img
                  src={companyLogo}
                  alt="Home"
                  style={{ height: "40px", objectFit: "contain" }}
                />
              ) : (
                "The Mentalist"
              )}
            </Navbar.Brand>

          <Navbar.Toggle aria-controls="responsive-navbar-nav" />
          <Navbar.Collapse id="responsive-navbar-nav">
            <Nav className="justify-content-end flex-grow-1 pe-3">
              {isImpersonating && (
                <Nav.Link
                  onClick={returnToSuperAdmin}
                  style={{ color: "#ffc107", fontWeight: 600 }}
                >
                  ← Return to Super Admin
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
