import React, { useEffect, useState } from "react";
import { Navbar, Nav, Container } from "react-bootstrap";
import { useSelector } from "react-redux";
import { LinkContainer } from "react-router-bootstrap";
import { useNavigate } from "react-router-dom";

const Header = () => {
  const navigate = useNavigate();
  const loginReducer = useSelector((state) => state.loginReducer);
  const [isLoggedIn, setIsLoggedIn] = useState(loginReducer.loggedIn);
  let profilePageUrl = "";

  const isImpersonating = !!localStorage.getItem("impersonatorBackup");

  const logoutHandler = () => {
    setIsLoggedIn(false);
    localStorage.clear();
    navigate("/login");
  };

  // Restore the stashed Super Admin session and go back to the dashboard.
  const returnToSuperAdmin = () => {
    const backup = JSON.parse(localStorage.getItem("impersonatorBackup"));
    if (backup) {
      localStorage.setItem("jwtToken", backup.token);
      localStorage.setItem("user", backup.user);
      localStorage.removeItem("impersonatorBackup");
    }
    window.location.href = "/superadmin";
  };

  useEffect(() => {
    if (localStorage.getItem("jwtToken")) {
      setIsLoggedIn(true);
      loginReducer.user.roles.map((r) => {
        if (r["roleName"] === "ADMIN") {
          profilePageUrl = "/adminProfile";
        } else {
          profilePageUrl = "/";
        }
      });
    }
  }, [navigate]);

  return (
    <header>
      <Navbar bg="dark" variant="dark" expand="lg" collapseOnSelect>
        <Container>
            <Navbar.Brand>Exam-Portal</Navbar.Brand>

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
                  <Nav.Link>{loginReducer.user.firstName}</Nav.Link>
              ) : (
                <LinkContainer to="/">
                  <Nav.Link>Login</Nav.Link>
                </LinkContainer>
              )}

              {isLoggedIn ? (
                <LinkContainer to="/">
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
