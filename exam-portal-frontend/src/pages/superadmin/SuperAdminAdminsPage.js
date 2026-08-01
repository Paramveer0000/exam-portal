import React, { useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Badge, Button, Card, Col, Form, Row, Table } from "react-bootstrap";
import {
  BsBoxArrowInRight,
  BsKey,
  BsGraphUp,
  BsPauseCircle,
  BsPlayCircle,
  BsSearch,
  BsSortDown,
  BsSortUp,
  BsTrash,
} from "react-icons/bs";
import swal from "sweetalert";
import SuperAdminSidebar from "../../components/SuperAdminSidebar";
import adminServices from "../../services/adminServices";
import {
  createAdmin,
  deleteAdmin,
  fetchActivity,
  fetchAdmins,
  fetchMetrics,
  resetAdminPassword,
  setAdminStatus,
  updateAdmin,
} from "../../actions/adminActions";

const emptyForm = {
  username: "",
  password: "",
  firstName: "",
  lastName: "",
  phoneNumber: "",
  role: "ADMIN",
  studentLimit: "",
};

const SuperAdminAdminsPage = () => {
  const dispatch = useDispatch();
  const { admins, activity, error } = useSelector((state) => state.adminReducer);
  const [form, setForm] = useState(emptyForm);
  const [showCreate, setShowCreate] = useState(false);
  const [limitDraft, setLimitDraft] = useState({});
  const [search, setSearch] = useState("");
  const [sort, setSort] = useState({ key: "name", dir: 1 });

  // Open the create form pre-set to a role (from the top-right buttons).
  const openCreate = (role) => {
    setForm({ ...emptyForm, role });
    setShowCreate(true);
  };

  useEffect(() => {
    fetchAdmins(dispatch);
  }, []);

  const onFormChange = (e) =>
    setForm({ ...form, [e.target.name]: e.target.value });

  const createHandler = (e) => {
    e.preventDefault();
    if (!form.phoneNumber || form.phoneNumber.trim() === "") {
      swal("Phone required", "Phone number is required", "info");
      return;
    }
    const payload = {
      ...form,
      studentLimit: form.studentLimit === "" ? null : Number(form.studentLimit),
    };
    createAdmin(dispatch, payload).then((data) => {
      if (data.type === "CREATE_ADMIN_SUCCESS") {
        swal("Created", `${form.username} was added`, "success");
        setForm(emptyForm);
        setShowCreate(false);
        fetchMetrics(dispatch);
      } else {
        swal("Not created", data.payload || "Failed to create admin", "error");
      }
    });
  };

  const limitValue = (admin) =>
    limitDraft[admin.userId] !== undefined
      ? limitDraft[admin.userId]
      : admin.studentLimit == null
      ? ""
      : String(admin.studentLimit);

  const saveLimit = (admin) => {
    const raw = limitValue(admin);
    const studentLimit = raw === "" ? null : Number(raw);
    if (studentLimit != null && (!Number.isInteger(studentLimit) || studentLimit < 0)) {
      swal("Invalid limit", "Enter a whole number 0 or greater, or leave blank for unlimited", "warning");
      return;
    }
    updateAdmin(
      dispatch,
      admin.userId,
      {
        firstName: admin.firstName,
        lastName: admin.lastName,
        phoneNumber: admin.phoneNumber,
        studentLimit,
      }
    ).then((data) => {
      if (data.type === "UPDATE_ADMIN_SUCCESS") {
        swal("Saved", "Student limit updated", "success");
        setLimitDraft((prev) => {
          const next = { ...prev };
          delete next[admin.userId];
          return next;
        });
      } else {
        swal("Not saved", data.payload || "Could not update limit", "error");
      }
    });
  };

  const toggleStatus = (admin) => {
    setAdminStatus(dispatch, admin.userId, !admin.active);
  };

  const resetHandler = (admin) => {
    const newPassword = window.prompt(
      `Enter a new password for ${admin.username}`
    );
    if (newPassword) {
      resetAdminPassword(dispatch, admin.userId, newPassword).then(
        (data) => {
          if (data.type === "RESET_ADMIN_PASSWORD_SUCCESS") {
            swal("Password reset", `Updated for ${admin.username}`, "success");
          } else {
            swal("Failed", data.payload || "Could not reset password", "error");
          }
        }
      );
    }
  };

  const deleteHandler = (admin) => {
    swal({
      title: "Delete this admin?",
      text: `${admin.username} will be removed. Admins that still own quizzes or categories cannot be deleted.`,
      icon: "warning",
      buttons: true,
      dangerMode: true,
    }).then((confirmed) => {
      if (confirmed) {
        deleteAdmin(dispatch, admin.userId).then((data) => {
          if (data.type === "DELETE_ADMIN_SUCCESS") {
            swal("Deleted", `${admin.username} removed`, "success");
            fetchMetrics(dispatch);
          } else {
            swal("Not deleted", data.payload || "Could not delete", "error");
          }
        });
      }
    });
  };

  const viewActivity = (admin) => {
    fetchActivity(dispatch, admin.userId);
  };

  const toggleSort = (key) =>
    setSort((prev) =>
      prev.key === key ? { key, dir: -prev.dir } : { key, dir: 1 }
    );

  const sortIcon = (key) =>
    sort.key !== key ? null : sort.dir === 1 ? (
      <BsSortDown className="ms-1" />
    ) : (
      <BsSortUp className="ms-1" />
    );

  const filteredAdmins = useMemo(() => {
    const q = search.trim().toLowerCase();
    const list = admins.filter((a) => {
      if (!q) return true;
      const name = `${a.firstName || ""} ${a.lastName || ""} ${a.username || ""}`.toLowerCase();
      return name.includes(q);
    });
    const dir = sort.dir;
    return [...list].sort((a, b) => {
      if (sort.key === "students")
        return dir * ((a.activeStudentCount || 0) - (b.activeStudentCount || 0));
      const an = `${a.firstName || ""} ${a.lastName || ""}`.trim() || a.username;
      const bn = `${b.firstName || ""} ${b.lastName || ""}`.trim() || b.username;
      return dir * an.localeCompare(bn);
    });
  }, [admins, search, sort]);

  const schools = admins.filter((a) => a.role !== "SUPER_ADMIN");
  const stats = {
    total: schools.length,
    active: schools.filter((a) => a.active).length,
    students: schools.reduce((sum, a) => sum + (a.activeStudentCount || 0), 0),
    capacity: schools.some((a) => a.studentLimit == null)
      ? "∞"
      : schools.reduce((sum, a) => sum + (a.studentLimit || 0), 0),
  };

  const currentUserId = (() => {
    try {
      return JSON.parse(localStorage.getItem("user"))?.userId;
    } catch (e) {
      return null;
    }
  })();

  // Sign in as the target admin: stash the super-admin session, swap in the
  // admin's token/user, then reload into the admin portal.
  const loginAsHandler = (admin) => {
    adminServices.impersonate(admin.userId).then(({ data, error }) => {
      if (data && data.user) {
        const currentUser = JSON.parse(localStorage.getItem("user"));
        localStorage.setItem("impersonatorBackup", JSON.stringify({
          userId: currentUser.userId,
        }));
        localStorage.setItem("user", JSON.stringify(data.user));
        window.location.href = "/adminProfile";
      } else {
        swal("Failed", error || "Could not sign in as this admin", "error");
      }
    });
  };

  return (
    <div style={{ display: "flex" }}>
      <SuperAdminSidebar />
      <div style={{ padding: "1.5rem", flexGrow: 1, minWidth: 0 }}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            flexWrap: "wrap",
            gap: "8px",
          }}
        >
          <h2 className="mb-0" style={{ color: "var(--mt-primary)" }}>
            Schools
          </h2>
          <div style={{ display: "flex", gap: "8px" }}>
            <Button variant="primary" onClick={() => openCreate("ADMIN")}>
              + Register Partner / School
            </Button>
            <Button
              variant="outline-primary"
              onClick={() => openCreate("SUPER_ADMIN")}
            >
              + Register Super Admin
            </Button>
          </div>
        </div>

        <Row className="my-3">
          {[
            { label: "Schools", value: stats.total },
            { label: "Active", value: stats.active },
            { label: "Active students", value: stats.students },
            { label: "Total capacity", value: stats.capacity },
          ].map((s) => (
            <Col key={s.label} xs={6} md={3} className="mb-3">
              <Card
                body
                style={{
                  borderRadius: "var(--mt-radius-md)",
                  border: "1px solid var(--mt-border)",
                  boxShadow: "var(--mt-shadow-sm)",
                  textAlign: "center",
                }}
              >
                <div
                  style={{
                    fontSize: "var(--mt-fs-2xl)",
                    fontWeight: 700,
                    color: "var(--mt-primary)",
                  }}
                >
                  {s.value}
                </div>
                <div style={{ color: "var(--mt-text-muted)", fontSize: "var(--mt-fs-sm)" }}>
                  {s.label}
                </div>
              </Card>
            </Col>
          ))}
        </Row>

        {showCreate && (
        <Card body className="my-3">
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
            }}
          >
            <h4 className="mb-0">
              {form.role === "SUPER_ADMIN"
                ? "Register Super Admin"
                : "Register Partner / School"}
            </h4>
            <Button
              variant="link"
              className="text-secondary"
              onClick={() => setShowCreate(false)}
            >
              Close
            </Button>
          </div>
          <Form onSubmit={createHandler}>
            <Row>
              <Col md={4} className="mb-2">
                <Form.Control
                  name="username"
                  placeholder="Username"
                  value={form.username}
                  onChange={onFormChange}
                  required
                />
              </Col>
              <Col md={4} className="mb-2">
                <Form.Control
                  name="password"
                  type="password"
                  placeholder="Password"
                  value={form.password}
                  onChange={onFormChange}
                  required
                />
              </Col>
              <Col md={4} className="mb-2">
                <Form.Control
                  name="phoneNumber"
                  placeholder="Phone number"
                  value={form.phoneNumber}
                  onChange={onFormChange}
                  required
                />
              </Col>
              <Col md={4} className="mb-2">
                <Form.Control
                  name="firstName"
                  placeholder="First name"
                  value={form.firstName}
                  onChange={onFormChange}
                />
              </Col>
              <Col md={4} className="mb-2">
                <Form.Control
                  name="lastName"
                  placeholder="Last name"
                  value={form.lastName}
                  onChange={onFormChange}
                />
              </Col>
              <Col md={4} className="mb-2">
                <Form.Select
                  name="role"
                  value={form.role}
                  onChange={onFormChange}
                >
                  <option value="ADMIN">Role: School</option>
                  <option value="SUPER_ADMIN">Role: Super Admin</option>
                </Form.Select>
              </Col>
              {form.role === "ADMIN" && (
                <Col md={4} className="mb-2">
                  <Form.Control
                    name="studentLimit"
                    type="number"
                    min="0"
                    placeholder="Student limit (blank = unlimited)"
                    value={form.studentLimit}
                    onChange={onFormChange}
                  />
                </Col>
              )}
              <Col md={4} className="mb-2">
                <Button type="submit" variant="primary">
                  {form.role === "SUPER_ADMIN"
                    ? "Create Super Admin"
                    : "Create School"}
                </Button>
              </Col>
            </Row>
          </Form>
          {error && <p style={{ color: "red" }}>{error}</p>}
        </Card>
        )}

        <Card
          body
          className="my-3"
          style={{
            borderRadius: "var(--mt-radius-md)",
            border: "1px solid var(--mt-border)",
            boxShadow: "var(--mt-shadow-sm)",
          }}
        >
          <h4>All Schools</h4>
          <div className="mb-3" style={{ maxWidth: 320, position: "relative" }}>
            <BsSearch
              style={{
                position: "absolute",
                left: 12,
                top: "50%",
                transform: "translateY(-50%)",
                color: "var(--mt-text-muted)",
              }}
            />
            <Form.Control
              placeholder="Search by school name"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{ paddingLeft: 34 }}
            />
          </div>
          <Table striped bordered hover responsive>
            <thead>
              <tr>
                <th>ID</th>
                <th>Username</th>
                <th role="button" onClick={() => toggleSort("name")}>
                  Name {sortIcon("name")}
                </th>
                <th>Phone</th>
                <th>Role</th>
                <th role="button" onClick={() => toggleSort("students")}>
                  Students {sortIcon("students")}
                </th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredAdmins.map((a) => {
                const initials = (
                  (a.firstName?.[0] || a.username?.[0] || "?") +
                  (a.lastName?.[0] || "")
                ).toUpperCase();
                const capacity = a.studentLimit;
                const atCapacity =
                  capacity != null && (a.activeStudentCount || 0) >= capacity;
                return (
                  <tr key={a.userId}>
                    <td>{a.userId}</td>
                    <td>{a.username}</td>
                    <td>
                      <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                        <div
                          style={{
                            width: 28,
                            height: 28,
                            borderRadius: "50%",
                            background: "var(--mt-primary-light)",
                            color: "var(--mt-text-on-primary)",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            fontSize: "var(--mt-fs-xs)",
                            fontWeight: 700,
                            flexShrink: 0,
                          }}
                        >
                          {initials}
                        </div>
                        {`${a.firstName || ""} ${a.lastName || ""}`.trim() || "—"}
                      </div>
                    </td>
                    <td>{a.phoneNumber}</td>
                    <td>
                      <Badge bg={a.role === "SUPER_ADMIN" ? "dark" : "info"}>
                        {a.role === "SUPER_ADMIN" ? "Super Admin" : "School"}
                      </Badge>
                    </td>
                    <td>
                      {a.role === "SUPER_ADMIN" ? (
                        "—"
                      ) : (
                        <div style={{ display: "flex", gap: "4px", alignItems: "center", minWidth: 200 }}>
                          <span
                            className={atCapacity ? "text-danger" : "text-muted"}
                            style={{ whiteSpace: "nowrap" }}
                          >
                            {a.activeStudentCount} active / allowed:
                          </span>
                          <Form.Control
                            size="sm"
                            type="number"
                            min="0"
                            placeholder="∞"
                            style={{ width: 80 }}
                            value={limitValue(a)}
                            onChange={(e) =>
                              setLimitDraft((prev) => ({ ...prev, [a.userId]: e.target.value }))
                            }
                          />
                          <Button size="sm" variant="outline-primary" onClick={() => saveLimit(a)}>
                            Save
                          </Button>
                        </div>
                      )}
                    </td>
                    <td>
                      <Badge bg={a.active ? "success" : "secondary"}>
                        {a.active ? "Active" : "Disabled"}
                      </Badge>
                    </td>
                    <td style={{ display: "flex", gap: "4px", flexWrap: "wrap" }}>
                      {a.userId !== currentUserId && a.role !== "SUPER_ADMIN" && (
                        <Button
                          size="sm"
                          variant="success"
                          title="Login as this school"
                          onClick={() => loginAsHandler(a)}
                        >
                          <BsBoxArrowInRight />
                        </Button>
                      )}
                      <Button
                        size="sm"
                        variant="warning"
                        title="Reset password"
                        onClick={() => resetHandler(a)}
                      >
                        <BsKey />
                      </Button>
                      <Button
                        size="sm"
                        variant="info"
                        title="View activity"
                        onClick={() => viewActivity(a)}
                      >
                        <BsGraphUp />
                      </Button>
                      {/* You can't disable or delete your own account. */}
                      {a.userId !== currentUserId && (
                        <>
                          <Button
                            size="sm"
                            variant="secondary"
                            title={a.active ? "Disable" : "Enable"}
                            onClick={() => toggleStatus(a)}
                          >
                            {a.active ? <BsPauseCircle /> : <BsPlayCircle />}
                          </Button>
                          <Button
                            size="sm"
                            variant="danger"
                            title="Delete"
                            onClick={() => deleteHandler(a)}
                          >
                            <BsTrash />
                          </Button>
                        </>
                      )}
                    </td>
                  </tr>
                );
              })}
              {filteredAdmins.length === 0 && (
                <tr>
                  <td colSpan="8" className="text-center">
                    {admins.length === 0 ? "No schools yet." : "No schools match your search."}
                  </td>
                </tr>
              )}
            </tbody>
          </Table>
        </Card>

        {activity && (
          <Card body className="my-3">
            <h4>Activity — {activity.username}</h4>
            <Row>
              <Col md={3}>Categories created: {activity.categoriesCreated}</Col>
              <Col md={3}>Quizzes created: {activity.quizzesCreated}</Col>
              <Col md={3}>Exams conducted: {activity.examsConducted}</Col>
              <Col md={3}>Total attempts: {activity.totalAttempts}</Col>
            </Row>
            <h5 className="mt-3">Recent activity</h5>
            <ul>
              {(activity.recentActivity || []).map((line, i) => (
                <li key={i}>{line}</li>
              ))}
              {(!activity.recentActivity ||
                activity.recentActivity.length === 0) && <li>No activity yet.</li>}
            </ul>
          </Card>
        )}
      </div>
    </div>
  );
};

export default SuperAdminAdminsPage;
