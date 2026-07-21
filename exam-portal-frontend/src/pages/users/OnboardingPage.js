import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { Button, Card, Form, ProgressBar } from "react-bootstrap";
import swal from "sweetalert";
import * as authConstants from "../../constants/authConstants";
import profileServices from "../../services/profileServices";
import authServices from "../../services/authServices";
import "./OnboardingPage.css";

export const GRADES = ["6", "7", "8", "9", "10", "11", "12"];
export const BOARDS = ["CBSE", "ICSE", "State Board", "IB", "Other"];

// A student's basic info is complete once name/phone/school are set; academics
// once grade/board/schoolName are set. Same rule the route gate uses.
export const isBasicComplete = (u) =>
  !!(u && u.firstName && u.lastName && u.phoneNumber && u.teacherId);
export const isAcademicsComplete = (u) =>
  !!(u && u.grade && u.board && u.schoolName);

const OnboardingPage = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const user = useSelector((state) => state.loginReducer.user);
  const token = JSON.parse(localStorage.getItem("jwtToken"));

  const [firstName, setFirstName] = useState(user?.firstName || "");
  const [lastName, setLastName] = useState(user?.lastName || "");
  const [phoneNumber, setPhoneNumber] = useState(user?.phoneNumber || "");
  const [teacherId, setTeacherId] = useState(
    user?.teacherId ? String(user.teacherId) : ""
  );
  const [grade, setGrade] = useState(user?.grade || "");
  const [board, setBoard] = useState(user?.board || "");
  const [schoolName, setSchoolName] = useState(user?.schoolName || "");
  const [teachers, setTeachers] = useState([]);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    authServices.getTeachers().then((list) => setTeachers(list || []));
  }, []);

  useEffect(() => {
    if (!token) navigate("/");
  }, []);

  // Already fully onboarded and lands here directly -> nothing to do here.
  useEffect(() => {
    if (isBasicComplete(user) && isAcademicsComplete(user)) {
      navigate("/profile", { replace: true });
    }
  }, [user]);

  const step = isBasicComplete(user) ? 2 : 1;

  const save = (payload) => {
    setSaving(true);
    profileServices.updateProfile(payload, token).then(({ data, error }) => {
      setSaving(false);
      if (data && data.jwtToken) {
        localStorage.setItem("jwtToken", JSON.stringify(data.jwtToken));
        localStorage.setItem("user", JSON.stringify(data.user));
        dispatch({ type: authConstants.USER_LOGIN_SUCCESS, payload: data.user });
        if (isAcademicsComplete(data.user)) {
          swal("All set!", "Your profile is complete.", "success").then(() =>
            navigate("/profile")
          );
        }
      } else {
        swal("Could not save", error || "Please try again", "error");
      }
    });
  };

  const submitBasic = (e) => {
    e.preventDefault();
    if (!firstName.trim() || !lastName.trim() || !phoneNumber.trim() || !teacherId) {
      swal("Missing info", "Please fill in every field and pick your school.", "info");
      return;
    }
    save({
      firstName,
      lastName,
      username: user.username,
      phoneNumber,
      teacherId: Number(teacherId),
      grade: user.grade,
      board: user.board,
      schoolName: user.schoolName,
    });
  };

  const submitAcademics = (e) => {
    e.preventDefault();
    if (!grade || !board || !schoolName.trim()) {
      swal("Missing info", "Please fill in every field.", "info");
      return;
    }
    save({
      firstName: user.firstName,
      lastName: user.lastName,
      username: user.username,
      phoneNumber: user.phoneNumber,
      teacherId: user.teacherId,
      grade,
      board,
      schoolName,
    });
  };

  if (!user) return null;

  return (
    <div className="onboardingPage__container">
      <Card className="onboardingPage__card">
        <h2>Welcome, {user.firstName || user.username}!</h2>
        <p className="text-muted">
          Just a couple of quick steps before you start your first test.
        </p>
        <ProgressBar
          now={step === 1 ? 50 : 100}
          label={`Step ${step} of 2`}
          className="mb-4"
        />

        {step === 1 ? (
          <Form onSubmit={submitBasic}>
            <h4>Step 1: Basic information</h4>
            <Form.Group className="my-3">
              <Form.Label>First name</Form.Label>
              <Form.Control
                value={firstName}
                required
                onChange={(e) => setFirstName(e.target.value)}
              />
            </Form.Group>
            <Form.Group className="my-3">
              <Form.Label>Last name</Form.Label>
              <Form.Control
                value={lastName}
                required
                onChange={(e) => setLastName(e.target.value)}
              />
            </Form.Group>
            <Form.Group className="my-3">
              <Form.Label>Phone number</Form.Label>
              <Form.Control
                value={phoneNumber}
                required
                onChange={(e) => setPhoneNumber(e.target.value)}
              />
            </Form.Group>
            <Form.Group className="my-3">
              <Form.Label>School</Form.Label>
              <Form.Select
                value={teacherId}
                required
                onChange={(e) => setTeacherId(e.target.value)}
              >
                <option value="">Choose a school</option>
                {teachers.map((t) => (
                  <option key={t.userId} value={t.userId}>
                    {t.name}
                  </option>
                ))}
              </Form.Select>
            </Form.Group>
            <Button type="submit" variant="primary" disabled={saving}>
              {saving ? "Saving..." : "Continue"}
            </Button>
          </Form>
        ) : (
          <Form onSubmit={submitAcademics}>
            <h4>Step 2: Academic details</h4>
            <Form.Group className="my-3">
              <Form.Label>Grade / Class</Form.Label>
              <Form.Select
                value={grade}
                required
                onChange={(e) => setGrade(e.target.value)}
              >
                <option value="">Choose your grade</option>
                {GRADES.map((g) => (
                  <option key={g} value={g}>
                    Class {g}
                  </option>
                ))}
              </Form.Select>
            </Form.Group>
            <Form.Group className="my-3">
              <Form.Label>Board</Form.Label>
              <Form.Select
                value={board}
                required
                onChange={(e) => setBoard(e.target.value)}
              >
                <option value="">Choose your board</option>
                {BOARDS.map((b) => (
                  <option key={b} value={b}>
                    {b}
                  </option>
                ))}
              </Form.Select>
            </Form.Group>
            <Form.Group className="my-3">
              <Form.Label>School name</Form.Label>
              <Form.Control
                value={schoolName}
                required
                placeholder="Your actual school's name"
                onChange={(e) => setSchoolName(e.target.value)}
              />
            </Form.Group>
            <Button type="submit" variant="success" disabled={saving}>
              {saving ? "Saving..." : "Finish"}
            </Button>
          </Form>
        )}
      </Card>
    </div>
  );
};

export default OnboardingPage;
