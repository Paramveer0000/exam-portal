import React, { useEffect, useState } from "react";
import { Button, Card, Col, Form, Row, Table } from "react-bootstrap";
import { useSelector } from "react-redux";
import Image from "react-bootstrap/Image";
import swal from "sweetalert";
import profileServices from "../services/profileServices";
import authServices from "../services/authServices";
import { BOARDS, GRADES } from "../pages/users/OnboardingPage";

// Age is derived from the date of birth, never stored separately, so the two
// can't drift apart. Returns null when the DOB is unset or unparseable.
const ageFromDob = (dob) => {
  if (!dob) return null;
  const d = new Date(dob);
  if (isNaN(d)) return null;
  const now = new Date();
  let age = now.getFullYear() - d.getFullYear();
  const m = now.getMonth() - d.getMonth();
  if (m < 0 || (m === 0 && now.getDate() < d.getDate())) age--;
  return age >= 0 ? age : null;
};

/**
 * Shared profile view for any role: details table, plus Edit Profile and
 * Change Password sections that are hidden behind buttons until clicked.
 * showSchool: students only — shows the school (teacher) they're under and
 * lets them switch to a different one.
 */
const ProfilePanel = ({ showRole = false, showSchool = false, showLogo = false }) => {
  const user = useSelector((state) => state.loginReducer.user);
  const token = JSON.parse(localStorage.getItem("jwtToken"));

  const [showEdit, setShowEdit] = useState(false);
  const [showPw, setShowPw] = useState(false);
  const [uploadingLogo, setUploadingLogo] = useState(false);

  const [firstName, setFirstName] = useState(user ? user.firstName || "" : "");
  const [lastName, setLastName] = useState(user ? user.lastName || "" : "");
  const [username, setUsername] = useState(user ? user.username || "" : "");
  const [phoneNumber, setPhoneNumber] = useState(
    user ? user.phoneNumber || "" : ""
  );
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [teachers, setTeachers] = useState([]);
  const [teacherId, setTeacherId] = useState(
    user && user.teacherId ? String(user.teacherId) : ""
  );
  const [grade, setGrade] = useState(user ? user.grade || "" : "");
  const [board, setBoard] = useState(user ? user.board || "" : "");
  const [schoolName, setSchoolName] = useState(
    user ? user.schoolName || "" : ""
  );
  const [guardianName, setGuardianName] = useState(
    user ? user.guardianName || "" : ""
  );
  const [gender, setGender] = useState(user ? user.gender || "" : "");
  const [dob, setDob] = useState(user ? user.dob || "" : "");
  const [city, setCity] = useState(user ? user.city || "" : "");

  useEffect(() => {
    if (showSchool) {
      authServices.getTeachers().then((list) => setTeachers(list || []));
    }
  }, [showSchool]);

  if (!user) return null;

  const currentSchoolName =
    teachers.find((t) => t.userId === user.teacherId)?.name || "—";

  const saveProfile = (e) => {
    e.preventDefault();
    if (!username.trim() || !phoneNumber.trim()) {
      swal("Required", "Username and phone cannot be empty", "info");
      return;
    }
    const payload = { firstName, lastName, username, phoneNumber };
    if (showSchool) {
      if (teacherId) payload.teacherId = Number(teacherId);
      payload.grade = grade;
      payload.board = board;
      payload.schoolName = schoolName;
      payload.guardianName = guardianName;
      payload.gender = gender;
      payload.dob = dob;
      payload.city = city;
    }
    profileServices
      .updateProfile(payload, token)
      .then(({ data, error }) => {
        if (data && data.jwtToken) {
          localStorage.setItem("jwtToken", JSON.stringify(data.jwtToken));
          localStorage.setItem("user", JSON.stringify(data.user));
          swal("Profile updated", "Your details were saved", "success").then(
            () => window.location.reload()
          );
        } else {
          swal("Update failed", error || "Could not update profile", "error");
        }
      });
  };

  const onLogoChange = (e) => {
    const file = e.target.files && e.target.files[0];
    e.target.value = ""; // allow re-selecting the same file
    if (!file) return;
    if (file.type !== "image/png") {
      swal("PNG only", "Please choose a PNG image.", "info");
      return;
    }
    if (file.size > 1024 * 1024) {
      swal("Too large", "Please choose a PNG under 1 MB.", "info");
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      setUploadingLogo(true);
      // The profile update requires the identity fields, so send the current
      // values alongside the new logo (the service only overwrites the logo).
      profileServices
        .updateProfile(
          {
            firstName: user.firstName,
            lastName: user.lastName,
            username: user.username,
            phoneNumber: user.phoneNumber,
            logo: reader.result,
          },
          token
        )
        .then(({ data, error }) => {
          setUploadingLogo(false);
          if (data && data.jwtToken) {
            localStorage.setItem("jwtToken", JSON.stringify(data.jwtToken));
            localStorage.setItem("user", JSON.stringify(data.user));
            swal("Logo updated", "Your logo was saved", "success").then(() =>
              window.location.reload()
            );
          } else {
            swal("Upload failed", error || "Could not save logo", "error");
          }
        });
    };
    reader.readAsDataURL(file);
  };

  const changePasswordHandler = (e) => {
    e.preventDefault();
    if (!currentPassword || !newPassword) {
      swal("Missing fields", "Enter current and new password", "info");
      return;
    }
    profileServices
      .changePassword({ currentPassword, newPassword }, token)
      .then(({ ok, error }) => {
        if (ok) {
          swal("Password changed", "Your password was updated", "success");
          setCurrentPassword("");
          setNewPassword("");
          setShowPw(false);
        } else {
          swal("Failed", error || "Could not change password", "error");
        }
      });
  };

  return (
    <div style={{ maxWidth: "700px", margin: "0 auto", width: "100%" }}>
      <Image
        roundedCircle
        src={showLogo && user.logo ? user.logo : "images/user.png"}
        style={{
          display: "block",
          margin: "1rem auto",
          width: "120px",
          height: "120px",
          objectFit: "cover",
          border: "2px solid #eee",
          background: "#fff",
        }}
      />

      {showLogo && (
        <div style={{ textAlign: "center", marginBottom: "1rem" }}>
          <Form.Label
            htmlFor="logo-upload"
            className="btn btn-outline-primary btn-sm mb-0"
            style={{ cursor: uploadingLogo ? "default" : "pointer" }}
          >
            {uploadingLogo ? "Uploading…" : user.logo ? "Change logo (PNG)" : "Upload logo (PNG)"}
          </Form.Label>
          <input
            id="logo-upload"
            type="file"
            accept="image/png"
            hidden
            disabled={uploadingLogo}
            onChange={onLogoChange}
          />
          <div style={{ fontSize: "0.8rem", color: "#888", marginTop: "6px" }}>
            PNG only · square recommended (e.g. 256×256 px) · max 1 MB
          </div>
        </div>
      )}

      <h5 className="text-start mt-2">Profile</h5>
      <Table bordered>
        <tbody>
          <tr>
            <td>Name</td>
            <td>{`${user.firstName || ""} ${user.lastName || ""}`}</td>
          </tr>
          <tr>
            <td>Username</td>
            <td>{user.username}</td>
          </tr>
          <tr>
            <td>Phone</td>
            <td>{user.phoneNumber}</td>
          </tr>
          {showSchool && (
            <tr>
              <td>School</td>
              <td>{currentSchoolName}</td>
            </tr>
          )}
          {showRole && user.roles && user.roles[0] && (
            <tr>
              <td>Role</td>
              <td>{user.roles[0].roleName}</td>
            </tr>
          )}
          <tr>
            <td>Account Status</td>
            <td>{`${user.enabled}`}</td>
          </tr>
        </tbody>
      </Table>

      {showSchool && (
        <>
          <h5 className="text-start mt-4">Academics</h5>
          <Table bordered>
            <tbody>
              <tr>
                <td>Grade</td>
                <td>{user.grade ? `Class ${user.grade}` : "—"}</td>
              </tr>
              <tr>
                <td>Board</td>
                <td>{user.board || "—"}</td>
              </tr>
              <tr>
                <td>School name</td>
                <td>{user.schoolName || "—"}</td>
              </tr>
            </tbody>
          </Table>

          <h5 className="text-start mt-4">Personal details</h5>
          <Table bordered>
            <tbody>
              <tr>
                <td>Guardian's name</td>
                <td>{user.guardianName || "—"}</td>
              </tr>
              <tr>
                <td>Gender</td>
                <td>{user.gender || "—"}</td>
              </tr>
              <tr>
                <td>Date of birth</td>
                <td>{user.dob || "—"}</td>
              </tr>
              <tr>
                <td>Age</td>
                <td>{ageFromDob(user.dob) ?? "—"}</td>
              </tr>
              <tr>
                <td>City</td>
                <td>{user.city || "—"}</td>
              </tr>
            </tbody>
          </Table>
        </>
      )}

      <div className="d-flex gap-2 my-3">
        <Button variant="primary" onClick={() => setShowEdit((v) => !v)}>
          {showEdit ? "Close" : "Edit Profile"}
        </Button>
        <Button variant="warning" onClick={() => setShowPw((v) => !v)}>
          {showPw ? "Close" : "Change Password"}
        </Button>
      </div>

      {showEdit && (
        <Card body className="my-3 text-start">
          <h4>Edit Profile</h4>
          <Form onSubmit={saveProfile}>
            <h6 className="text-muted">Profile</h6>
            <Row>
              <Col md={6} className="mb-2">
                <Form.Label>First name</Form.Label>
                <Form.Control
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                />
              </Col>
              <Col md={6} className="mb-2">
                <Form.Label>Last name</Form.Label>
                <Form.Control
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                />
              </Col>
              <Col md={6} className="mb-2">
                <Form.Label>Username</Form.Label>
                <Form.Control
                  value={username}
                  required
                  onChange={(e) => setUsername(e.target.value)}
                />
              </Col>
              <Col md={6} className="mb-2">
                <Form.Label>Phone number</Form.Label>
                <Form.Control
                  value={phoneNumber}
                  required
                  onChange={(e) => setPhoneNumber(e.target.value)}
                />
              </Col>
            </Row>

            {showSchool && (
              <>
                <hr />
                <h6 className="text-muted">Academics</h6>
                <Row>
                  <Col md={6} className="mb-2">
                    <Form.Label>School</Form.Label>
                    <Form.Select
                      value={teacherId}
                      onChange={(e) => setTeacherId(e.target.value)}
                    >
                      <option value="">Choose a school</option>
                      {teachers.map((t) => (
                        <option key={t.userId} value={t.userId}>
                          {t.name}
                        </option>
                      ))}
                    </Form.Select>
                  </Col>
                  <Col md={6} className="mb-2">
                    <Form.Label>Grade / Class</Form.Label>
                    <Form.Select
                      value={grade}
                      onChange={(e) => setGrade(e.target.value)}
                    >
                      <option value="">Choose your grade</option>
                      {GRADES.map((g) => (
                        <option key={g} value={g}>
                          Class {g}
                        </option>
                      ))}
                    </Form.Select>
                  </Col>
                  <Col md={6} className="mb-2">
                    <Form.Label>Board</Form.Label>
                    <Form.Select
                      value={board}
                      onChange={(e) => setBoard(e.target.value)}
                    >
                      <option value="">Choose your board</option>
                      {BOARDS.map((b) => (
                        <option key={b} value={b}>
                          {b}
                        </option>
                      ))}
                    </Form.Select>
                  </Col>
                  <Col md={6} className="mb-2">
                    <Form.Label>School name</Form.Label>
                    <Form.Control
                      value={schoolName}
                      placeholder="Your actual school's name"
                      onChange={(e) => setSchoolName(e.target.value)}
                    />
                  </Col>
                </Row>

                <hr />
                <h6 className="text-muted">Personal details (optional)</h6>
                <Row>
                  <Col md={6} className="mb-2">
                    <Form.Label>Guardian's name</Form.Label>
                    <Form.Control
                      value={guardianName}
                      onChange={(e) => setGuardianName(e.target.value)}
                    />
                  </Col>
                  <Col md={4} className="mb-2">
                    <Form.Label>Gender</Form.Label>
                    <Form.Select
                      value={gender}
                      onChange={(e) => setGender(e.target.value)}
                    >
                      <option value="">Choose</option>
                      <option value="Male">Male</option>
                      <option value="Female">Female</option>
                      <option value="Other">Other</option>
                    </Form.Select>
                  </Col>
                  <Col md={4} className="mb-2">
                    <Form.Label>Date of birth</Form.Label>
                    <Form.Control
                      type="date"
                      value={dob}
                      onChange={(e) => setDob(e.target.value)}
                    />
                  </Col>
                  <Col md={4} className="mb-2">
                    <Form.Label>City</Form.Label>
                    <Form.Control
                      value={city}
                      onChange={(e) => setCity(e.target.value)}
                    />
                  </Col>
                </Row>
              </>
            )}

            <Button type="submit" variant="primary" className="mt-3">
              Save changes
            </Button>
          </Form>
        </Card>
      )}

      {showPw && (
        <Card body className="my-3 text-start">
          <h4>Change Password</h4>
          <Form onSubmit={changePasswordHandler}>
            <Row>
              <Col md={6} className="mb-2">
                <Form.Label>Current password</Form.Label>
                <Form.Control
                  type="password"
                  value={currentPassword}
                  required
                  onChange={(e) => setCurrentPassword(e.target.value)}
                />
              </Col>
              <Col md={6} className="mb-2">
                <Form.Label>New password</Form.Label>
                <Form.Control
                  type="password"
                  value={newPassword}
                  required
                  onChange={(e) => setNewPassword(e.target.value)}
                />
              </Col>
            </Row>
            <Button type="submit" variant="warning" className="mt-2">
              Update password
            </Button>
          </Form>
        </Card>
      )}
    </div>
  );
};

export default ProfilePanel;
