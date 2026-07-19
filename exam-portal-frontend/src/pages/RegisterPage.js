import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { register } from "../actions/authActions";
import authServices from "../services/authServices";
import { useDispatch, useSelector } from "react-redux";
import Loader from "../components/Loader";
import { Form, Button, InputGroup, Row, Col } from "react-bootstrap";
import FormContainer from "../components/FormContainer";
import { FaEye, FaEyeSlash } from "react-icons/fa";
import * as authConstants from "../constants/authConstants";
import { Link } from "react-router-dom";
import swal from "sweetalert";

const RegisterPage = () => {
  const [mode, setMode] = useState("STUDENT"); // STUDENT | SCHOOL
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [address, setAddress] = useState("");
  const [schoolType, setSchoolType] = useState("");
  const [teacherId, setTeacherId] = useState("");
  const [schools, setSchools] = useState([]);
  const [showPassword, setShowPassword] = useState(false);
  const [passwordType, setPasswordType] = useState("password");

  const dispatch = useDispatch();
  const navigate = useNavigate();
  const registerReducer = useSelector((state) => state.registerReducer);

  useEffect(() => {
    authServices.getTeachers().then((data) => setSchools(data || []));
  }, []);

  const showPasswordHandler = () => {
    const temp = !showPassword;
    setShowPassword(temp);
    setPasswordType(temp ? "text" : "password");
  };

  const commonChecks = () => {
    if (password !== confirmPassword) {
      alert("Passwords do not match");
      return false;
    }
    if (!phoneNumber || phoneNumber.trim() === "") {
      alert("Phone number is required");
      return false;
    }
    return true;
  };

  const studentSubmit = (e) => {
    e.preventDefault();
    if (!commonChecks()) return;
    if (!teacherId) {
      alert("Please select your school");
      return;
    }
    const user = {
      firstName,
      lastName,
      username,
      password,
      phoneNumber,
      teacherId: Number(teacherId),
    };
    register(dispatch, user).then((data) => {
      if (data.type === authConstants.USER_REGISTER_SUCCESS) navigate("/login");
      else alert(data.payload || "Registration failed");
    });
  };

  const schoolSubmit = (e) => {
    e.preventDefault();
    if (!commonChecks()) return;
    // For a school, firstName holds the school name.
    if (!address.trim() || !schoolType) {
      alert("Address and school type are required");
      return;
    }
    const school = {
      firstName,
      lastName,
      username,
      password,
      phoneNumber,
      address,
      schoolType,
    };
    authServices.registerSchool(school).then(({ isRegistered, error }) => {
      if (isRegistered) {
        swal("School registered", "You can now log in", "success").then(() =>
          navigate("/login")
        );
      } else {
        alert(error || "Signup failed");
      }
    });
  };

  const passwordFields = (
    <>
      <Form.Group className="my-3" controlId="password">
        <Form.Label>Password</Form.Label>
        <InputGroup>
          <Form.Control
            type={`${passwordType}`}
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
      <Form.Group className="my-3" controlId="confirmPassword">
        <Form.Label>Confirm Password</Form.Label>
        <Form.Control
          type="password"
          placeholder="Confirm Password"
          value={confirmPassword}
          required
          onChange={(e) => setConfirmPassword(e.target.value)}
        />
      </Form.Group>
      <Form.Group className="my-3" controlId="phoneNumber">
        <Form.Label>Phone Number *</Form.Label>
        <Form.Control
          type="tel"
          placeholder="Enter Phone Number"
          value={phoneNumber}
          required
          onChange={(e) => setPhoneNumber(e.target.value)}
        />
      </Form.Group>
    </>
  );

  return (
    <FormContainer>
      <h1>Sign Up</h1>

      <div className="d-flex gap-2 my-3">
        <Button
          variant={mode === "STUDENT" ? "primary" : "outline-primary"}
          onClick={() => setMode("STUDENT")}
        >
          Student Sign Up
        </Button>
        <Button
          variant={mode === "SCHOOL" ? "primary" : "outline-primary"}
          onClick={() => setMode("SCHOOL")}
        >
          School Sign Up
        </Button>
      </div>

      {mode === "STUDENT" ? (
        <Form onSubmit={studentSubmit}>
          <Form.Group className="my-3" controlId="fname">
            <Form.Label>First Name</Form.Label>
            <Form.Control
              placeholder="Enter First Name"
              value={firstName}
              required
              onChange={(e) => setFirstName(e.target.value)}
            />
          </Form.Group>
          <Form.Group className="my-3" controlId="lname">
            <Form.Label>Last Name</Form.Label>
            <Form.Control
              placeholder="Enter Last Name"
              value={lastName}
              required
              onChange={(e) => setLastName(e.target.value)}
            />
          </Form.Group>
          <Form.Group className="my-3" controlId="username">
            <Form.Label>User Name</Form.Label>
            <Form.Control
              placeholder="Enter User Name"
              value={username}
              required
              onChange={(e) => setUsername(e.target.value)}
            />
          </Form.Group>
          {passwordFields}
          <Form.Group className="my-3" controlId="school">
            <Form.Label>School *</Form.Label>
            <Form.Select
              value={teacherId}
              required
              onChange={(e) => setTeacherId(e.target.value)}
            >
              <option value="">Select your school</option>
              {schools.map((s) => (
                <option key={s.userId} value={s.userId}>
                  {s.name}
                </option>
              ))}
            </Form.Select>
          </Form.Group>
          <Button
            variant=""
            className="my-3"
            type="submit"
            style={{ backgroundColor: "rgb(68 177 49)", color: "white" }}
          >
            Register as Student
          </Button>
        </Form>
      ) : (
        <Form onSubmit={schoolSubmit}>
          <Form.Group className="my-3" controlId="schoolName">
            <Form.Label>School Name</Form.Label>
            <Form.Control
              placeholder="Enter School Name"
              value={firstName}
              required
              onChange={(e) => setFirstName(e.target.value)}
            />
          </Form.Group>
          <Form.Group className="my-3" controlId="address">
            <Form.Label>Address</Form.Label>
            <Form.Control
              as="textarea"
              rows="2"
              placeholder="Enter School Address"
              value={address}
              required
              onChange={(e) => setAddress(e.target.value)}
            />
          </Form.Group>
          <Form.Group className="my-3" controlId="schoolType">
            <Form.Label>School Type / Board</Form.Label>
            <Form.Select
              value={schoolType}
              required
              onChange={(e) => setSchoolType(e.target.value)}
            >
              <option value="">Select board</option>
              <option value="CBSE">CBSE</option>
              <option value="ICSE">ICSE</option>
              <option value="State Board">State Board</option>
              <option value="IB">IB</option>
              <option value="IGCSE">IGCSE</option>
              <option value="Other">Other</option>
            </Form.Select>
          </Form.Group>
          <Form.Group className="my-3" controlId="username">
            <Form.Label>User Name</Form.Label>
            <Form.Control
              placeholder="Enter User Name"
              value={username}
              required
              onChange={(e) => setUsername(e.target.value)}
            />
          </Form.Group>
          {passwordFields}
          <Button
            variant=""
            className="my-3"
            type="submit"
            style={{ backgroundColor: "rgb(68 177 49)", color: "white" }}
          >
            Register as School
          </Button>
        </Form>
      )}

      {registerReducer.loading ? (
        <Loader />
      ) : (
        <Row className="py-3">
          <Col>
            Have an Account?{" "}
            <Link to="/" style={{ color: "rgb(68 177 49)" }}>
              Login
            </Link>
          </Col>
        </Row>
      )}
    </FormContainer>
  );
};

export default RegisterPage;
