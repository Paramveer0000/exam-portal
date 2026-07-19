import React, { useState } from "react";
import { Button, Card, Col, Form, Row, Table } from "react-bootstrap";
import { useSelector } from "react-redux";
import Image from "react-bootstrap/Image";
import swal from "sweetalert";
import profileServices from "../services/profileServices";

/**
 * Shared profile view for any role: details table, plus Edit Profile and
 * Change Password sections that are hidden behind buttons until clicked.
 */
const ProfilePanel = ({ showRole = false }) => {
  const user = useSelector((state) => state.loginReducer.user);
  const token = JSON.parse(localStorage.getItem("jwtToken"));

  const [showEdit, setShowEdit] = useState(false);
  const [showPw, setShowPw] = useState(false);

  const [firstName, setFirstName] = useState(user ? user.firstName || "" : "");
  const [lastName, setLastName] = useState(user ? user.lastName || "" : "");
  const [username, setUsername] = useState(user ? user.username || "" : "");
  const [phoneNumber, setPhoneNumber] = useState(
    user ? user.phoneNumber || "" : ""
  );
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");

  if (!user) return null;

  const saveProfile = (e) => {
    e.preventDefault();
    if (!username.trim() || !phoneNumber.trim()) {
      swal("Required", "Username and phone cannot be empty", "info");
      return;
    }
    profileServices
      .updateProfile({ firstName, lastName, username, phoneNumber }, token)
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
        width="20%"
        height="20%"
        roundedCircle
        src="images/user.png"
        style={{ display: "block", margin: "1rem auto" }}
      />

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
            <Button type="submit" variant="primary" className="mt-2">
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
