import React, { useEffect, useState } from "react";
import { Button, Form, Table } from "react-bootstrap";
import swal from "sweetalert";
import Sidebar from "../../components/Sidebar";
import studentsServices from "../../services/studentsServices";
import categoriesServices from "../../services/categoriesServices";

const AdminStudentsPage = () => {
  const token = JSON.parse(localStorage.getItem("jwtToken"));
  const [students, setStudents] = useState([]);
  const [error, setError] = useState("");
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState({});
  const [allClasses, setAllClasses] = useState([]);
  const [assigningId, setAssigningId] = useState(null);
  const [assignedIds, setAssignedIds] = useState(new Set());

  const load = () => {
    studentsServices.fetchStudents(token).then(({ data, error }) => {
      if (data) setStudents(data);
      else setError(error || "Could not load students");
    });
  };

  useEffect(() => {
    load();
    categoriesServices.fetchCategories(token).then((data) => {
      if (Array.isArray(data)) setAllClasses(data);
    });
  }, []);

  const openAssign = (s) => {
    if (assigningId === s.userId) {
      setAssigningId(null);
      return;
    }
    setAssigningId(s.userId);
    studentsServices.getAssignedClasses(s.userId, token).then(({ data }) => {
      setAssignedIds(new Set((data || []).map((c) => c.id)));
    });
  };

  const toggleClass = (studentId, catId, checked) => {
    const call = checked
      ? studentsServices.assignClass(studentId, catId, token)
      : studentsServices.unassignClass(studentId, catId, token);
    call.then(({ ok, error }) => {
      if (ok) {
        setAssignedIds((prev) => {
          const next = new Set(prev);
          if (checked) next.add(catId);
          else next.delete(catId);
          return next;
        });
      } else {
        swal("Failed", error || "Could not update class", "error");
      }
    });
  };

  const startEdit = (s) => {
    setEditingId(s.userId);
    setForm({
      username: s.username || "",
      firstName: s.firstName || "",
      lastName: s.lastName || "",
      phoneNumber: s.phoneNumber || "",
    });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setForm({});
  };

  const setField = (e) =>
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));

  const saveEdit = (s) => {
    studentsServices.updateStudent(s.userId, form, token).then(({ data, error }) => {
      if (data) {
        setStudents((prev) => prev.map((x) => (x.userId === data.userId ? data : x)));
        cancelEdit();
      } else {
        swal("Not saved", error || "Could not update student", "error");
      }
    });
  };

  const toggleStatus = (s) => {
    studentsServices.setStatus(s.userId, !s.active, token).then(({ data }) => {
      if (data)
        setStudents((prev) =>
          prev.map((x) => (x.userId === data.userId ? data : x))
        );
    });
  };

  const resetHandler = (s) => {
    const newPassword = window.prompt(`New password for ${s.username}`);
    if (newPassword) {
      studentsServices
        .resetPassword(s.userId, newPassword, token)
        .then(({ ok, error }) => {
          swal(
            ok ? "Password reset" : "Failed",
            ok ? `Updated for ${s.username}` : error || "Could not reset",
            ok ? "success" : "error"
          );
        });
    }
  };

  const deleteHandler = (s) => {
    swal({
      title: "Delete this student?",
      text: `${s.username} and all their exam results will be removed.`,
      icon: "warning",
      buttons: true,
      dangerMode: true,
    }).then((confirmed) => {
      if (confirmed) {
        studentsServices.deleteStudent(s.userId, token).then(({ ok, error }) => {
          if (ok) {
            swal("Deleted", `${s.username} removed`, "success");
            setStudents((prev) => prev.filter((x) => x.userId !== s.userId));
          } else {
            swal("Not deleted", error || "Could not delete", "error");
          }
        });
      }
    });
  };

  return (
    <div style={{ display: "flex" }}>
      <Sidebar />
      <div style={{ padding: "1.5rem", flexGrow: 1 }}>
        <h2>My Students</h2>
        {error && <p style={{ color: "red" }}>{error}</p>}
        <Table striped bordered hover responsive className="mt-3">
          <thead>
            <tr>
              <th>ID</th>
              <th>Username</th>
              <th>Name</th>
              <th>Phone</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {students.map((s) =>
              editingId === s.userId ? (
                <tr key={s.userId}>
                  <td>{s.userId}</td>
                  <td>
                    <Form.Control
                      name="username"
                      value={form.username}
                      onChange={setField}
                    />
                  </td>
                  <td style={{ display: "flex", gap: "4px" }}>
                    <Form.Control
                      name="firstName"
                      placeholder="First"
                      value={form.firstName}
                      onChange={setField}
                    />
                    <Form.Control
                      name="lastName"
                      placeholder="Last"
                      value={form.lastName}
                      onChange={setField}
                    />
                  </td>
                  <td>
                    <Form.Control
                      name="phoneNumber"
                      value={form.phoneNumber}
                      onChange={setField}
                    />
                  </td>
                  <td>{s.active ? "Active" : "Disabled"}</td>
                  <td style={{ display: "flex", gap: "4px", flexWrap: "wrap" }}>
                    <Button size="sm" variant="success" onClick={() => saveEdit(s)}>
                      Save
                    </Button>
                    <Button size="sm" variant="light" onClick={cancelEdit}>
                      Cancel
                    </Button>
                  </td>
                </tr>
              ) : (
                <tr key={s.userId}>
                  <td>{s.userId}</td>
                  <td>{s.username}</td>
                  <td>{`${s.firstName || ""} ${s.lastName || ""}`.trim()}</td>
                  <td>{s.phoneNumber}</td>
                  <td>{s.active ? "Active" : "Disabled"}</td>
                  <td style={{ display: "flex", gap: "4px", flexWrap: "wrap" }}>
                    <Button size="sm" variant="info" onClick={() => openAssign(s)}>
                      Classes
                    </Button>
                    <Button size="sm" variant="primary" onClick={() => startEdit(s)}>
                      Edit
                    </Button>
                    <Button size="sm" variant="secondary" onClick={() => toggleStatus(s)}>
                      {s.active ? "Disable" : "Enable"}
                    </Button>
                    <Button size="sm" variant="warning" onClick={() => resetHandler(s)}>
                      Reset PW
                    </Button>
                    <Button size="sm" variant="danger" onClick={() => deleteHandler(s)}>
                      Delete
                    </Button>
                  </td>
                </tr>
              )
            )}
            {assigningId != null &&
              students.some((s) => s.userId === assigningId) && (
                <tr>
                  <td colSpan="6" style={{ background: "#f6f9ff" }}>
                    <strong>
                      Assign classes to{" "}
                      {students.find((s) => s.userId === assigningId)?.username}
                    </strong>
                    {allClasses.length === 0 ? (
                      <p className="mb-0 mt-2 text-muted">
                        No classes available yet. Ask a platform admin to create
                        classes.
                      </p>
                    ) : (
                      <div
                        style={{
                          display: "flex",
                          flexWrap: "wrap",
                          gap: "12px",
                          marginTop: "8px",
                        }}
                      >
                        {allClasses.map((c) => (
                          <Form.Check
                            key={c.catId}
                            type="checkbox"
                            id={`cls-${assigningId}-${c.catId}`}
                            label={c.title}
                            checked={assignedIds.has(c.catId)}
                            onChange={(e) =>
                              toggleClass(assigningId, c.catId, e.target.checked)
                            }
                          />
                        ))}
                      </div>
                    )}
                  </td>
                </tr>
              )}
            {students.length === 0 && (
              <tr>
                <td colSpan="6" className="text-center">
                  No students yet. Students appear here once they register under you.
                </td>
              </tr>
            )}
          </tbody>
        </Table>
      </div>
    </div>
  );
};

export default AdminStudentsPage;
