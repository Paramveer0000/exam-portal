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
  // Only schools self-sign-up. Students are created by their school.
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
      <h1>School Sign Up</h1>
      <p className="text-muted">
        Students don’t sign up here — your school creates student accounts after logging in.
      </p>

      {(
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
            style={{ backgroundColor: "#1E7A6F", color: "white" }}
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
            <Link to="/" style={{ color: "#1E7A6F" }}>
              Login
            </Link>
          </Col>
        </Row>
      )}
    </FormContainer>
  );
};

export default RegisterPage;
