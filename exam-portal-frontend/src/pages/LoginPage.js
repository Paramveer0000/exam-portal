import React, { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { Form, Button, InputGroup } from "react-bootstrap";
import FormContainer from "../components/FormContainer";
import { FaEye, FaEyeSlash } from "react-icons/fa";
import { login } from "../actions/authActions";
import Loader from "../components/Loader";
import Message from "../components/Message";
import * as authConstants from "../constants/authConstants";
import { homePathForRoles } from "../components/ProtectedRoute";

const LoginPage = () => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [errorMsg, setErrorMsg] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [passwordType, setPasswordType] = useState("password");
  const [searchParams] = useSearchParams();

  const dispatch = useDispatch();
  const navigate = useNavigate();
  const loginReducer = useSelector((state) => state.loginReducer);

  const showPasswordHandler = () => {
    const temp = !showPassword;
    setShowPassword(temp);
    setPasswordType(temp ? "text" : "password");
  };

  const submitHandler = (e) => {
    e.preventDefault();
    setErrorMsg("");
    login(dispatch, username, password).then((data) => {
      if (data.type === authConstants.USER_LOGIN_SUCCESS) {
        const roleNames = data.payload.roles.map((r) => r.roleName);
        navigate(homePathForRoles(roleNames));
      } else {
        const payload = data.payload;
        setErrorMsg(
          (payload && payload.error) || "Invalid username or password"
        );
      }
    });
  };

  useEffect(() => {
    if (searchParams.get("expired") === "1") {
      setErrorMsg("Your session has expired. Please log in again.");
    }
  }, [searchParams]);

  useEffect(() => {
    const user = (() => {
      try { return JSON.parse(localStorage.getItem("user")); }
      catch (e) { return null; }
    })();
    if (user) {
      const roleNames = user.roles.map((r) => r.roleName);
      navigate(homePathForRoles(roleNames));
    }
  }, []);

  return (
    <FormContainer>
      <h1>Sign In</h1>
      {errorMsg && <Message variant="danger">{errorMsg}</Message>}
      <Form onSubmit={submitHandler}>
        <Form.Group className="my-3" controlId="username">
          <Form.Label>User Name</Form.Label>
          <Form.Control
            type="text"
            placeholder="Enter User Name"
            value={username}
            required
            onChange={(e) => setUsername(e.target.value)}
          ></Form.Control>
        </Form.Group>

        <Form.Group className="my-3" controlId="password">
          <Form.Label>Password</Form.Label>
          <InputGroup>
            <Form.Control
              type={passwordType}
              placeholder="Enter Password"
              value={password}
              required
              onChange={(e) => setPassword(e.target.value)}
            />
            <Button
              onClick={showPasswordHandler}
              variant=""
              style={{ border: "1px solid black" }}
            >
              {showPassword ? <FaEyeSlash /> : <FaEye />}
            </Button>
          </InputGroup>
        </Form.Group>

        <Button
          variant=""
          className="my-3"
          type="submit"
          style={{ backgroundColor: "#1E7A6F", color: "white" }}
        >
          Login
        </Button>
      </Form>

      {loginReducer.loading && <Loader />}
    </FormContainer>
  );
};

export default LoginPage;
