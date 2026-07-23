import React, { useEffect, useState } from "react";
import { Button, Form, Table } from "react-bootstrap";
import swal from "sweetalert";
import Sidebar from "../../components/Sidebar";
import studentsServices from "../../services/studentsServices";
import categoriesServices from "../../services/categoriesServices";

const emptyNew = {
  username: "",
  password: "",
  firstName: "",
  lastName: "",
  phoneNumber: "",
  classId: "",
};

const AdminStudentsPage = () => {
  const token = JSON.parse(localStorage.getItem("jwtToken"));
  const [students, setStudents] = useState([]);
  const [error, setError] = useState("");
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState({});
  const [allClasses, setAllClasses] = useState([]);
  const [showCreate, setShowCreate] = useState(false);
  const [newStudent, setNewStudent] = useState(emptyNew);

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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const classTitle = (classId) => {
    const c = allClasses.find((x) => x.catId === classId);
    return c ? c.title : classId ? `#${classId}` : "—";
  };

  const setNewField = (e) =>
    setNewStudent((prev) => ({ ...prev, [e.target.name]: e.target.value }));

  const createHandler = (e) => {
    e.preventDefault();
    if (!newStudent.classId) {
      swal("Class required", "Pick a class for the student", "warning");
      return;
    }
    studentsServices
      .createStudent({ ...newStudent, classId: Number(newStudent.classId) }, token)
      .then(({ data, error }) => {
        if (data) {
          setStudents((prev) => [...prev, data]);
          setNewStudent(emptyNew);
          setShowCreate(false);
          swal("Created", `${data.username} added`, "success");
        } else {
          swal("Not created", error || "Could not create student", "error");
        }
      });
  };

  const changeClass = (s, classId) => {
    if (!classId) return;
    studentsServices.setClass(s.userId, Number(classId), token).then(({ data, error }) => {
      if (data) {
        setStudents((prev) => prev.map((x) => (x.userId === data.userId ? data : x)));
      } else {
        swal("Failed", error || "Could not change class", "error");
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
        setStudents((prev) => prev.map((x) => (x.userId === data.userId ? data : x)));
    });
  };

  const resetHandler = (s) => {
    const newPassword = window.prompt(`New password for ${s.username}`);
    if (newPassword) {
      studentsServices.resetPassword(s.userId, newPassword, token).then(({ ok, error }) => {
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
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <h2>My Students</h2>
          <Button variant="success" onClick={() => setShowCreate((v) => !v)}>
            {showCreate ? "Close" : "+ Add Student"}
          </Button>
        </div>
        {error && <p style={{ color: "red" }}>{error}</p>}

        {showCreate && (
          <Form onSubmit={createHandler} className="p-3 mt-2" style={{ background: "#f6f9ff", borderRadius: 8 }}>
            <div style={{ display: "flex", flexWrap: "wrap", gap: "10px" }}>
              <Form.Control name="username" placeholder="Username *" value={newStudent.username} onChange={setNewField} required style={{ maxWidth: 180 }} />
              <Form.Control name="password" type="password" placeholder="Password *" value={newStudent.password} onChange={setNewField} required style={{ maxWidth: 180 }} />
              <Form.Control name="firstName" placeholder="First name" value={newStudent.firstName} onChange={setNewField} style={{ maxWidth: 150 }} />
              <Form.Control name="lastName" placeholder="Last name" value={newStudent.lastName} onChange={setNewField} style={{ maxWidth: 150 }} />
              <Form.Control name="phoneNumber" placeholder="Phone" value={newStudent.phoneNumber} onChange={setNewField} style={{ maxWidth: 150 }} />
              <Form.Select name="classId" value={newStudent.classId} onChange={setNewField} required style={{ maxWidth: 200 }}>
                <option value="">Select class *</option>
                {allClasses.map((c) => (
                  <option key={c.catId} value={c.catId}>{c.title}</option>
                ))}
              </Form.Select>
              <Button type="submit" variant="primary">Create</Button>
            </div>
            {allClasses.length === 0 && (
              <p className="mb-0 mt-2 text-muted">No classes exist yet. Ask a platform admin to create classes first.</p>
            )}
          </Form>
        )}

        <Table striped bordered hover responsive className="mt-3">
          <thead>
            <tr>
              <th>ID</th>
              <th>Username</th>
              <th>Name</th>
              <th>Phone</th>
              <th>Class</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {students.map((s) =>
              editingId === s.userId ? (
                <tr key={s.userId}>
                  <td>{s.userId}</td>
                  <td><Form.Control name="username" value={form.username} onChange={setField} /></td>
                  <td style={{ display: "flex", gap: "4px" }}>
                    <Form.Control name="firstName" placeholder="First" value={form.firstName} onChange={setField} />
                    <Form.Control name="lastName" placeholder="Last" value={form.lastName} onChange={setField} />
                  </td>
                  <td><Form.Control name="phoneNumber" value={form.phoneNumber} onChange={setField} /></td>
                  <td>{classTitle(s.classId)}</td>
                  <td>{s.active ? "Active" : "Disabled"}</td>
                  <td style={{ display: "flex", gap: "4px", flexWrap: "wrap" }}>
                    <Button size="sm" variant="success" onClick={() => saveEdit(s)}>Save</Button>
                    <Button size="sm" variant="light" onClick={cancelEdit}>Cancel</Button>
                  </td>
                </tr>
              ) : (
                <tr key={s.userId}>
                  <td>{s.userId}</td>
                  <td>{s.username}</td>
                  <td>{`${s.firstName || ""} ${s.lastName || ""}`.trim()}</td>
                  <td>{s.phoneNumber}</td>
                  <td style={{ minWidth: 160 }}>
                    <Form.Select
                      size="sm"
                      value={s.classId || ""}
                      onChange={(e) => changeClass(s, e.target.value)}
                    >
                      <option value="">— unassigned —</option>
                      {allClasses.map((c) => (
                        <option key={c.catId} value={c.catId}>{c.title}</option>
                      ))}
                    </Form.Select>
                  </td>
                  <td>{s.active ? "Active" : "Disabled"}</td>
                  <td style={{ display: "flex", gap: "4px", flexWrap: "wrap" }}>
                    <Button size="sm" variant="primary" onClick={() => startEdit(s)}>Edit</Button>
                    <Button size="sm" variant="secondary" onClick={() => toggleStatus(s)}>
                      {s.active ? "Disable" : "Enable"}
                    </Button>
                    <Button size="sm" variant="warning" onClick={() => resetHandler(s)}>Reset PW</Button>
                    <Button size="sm" variant="danger" onClick={() => deleteHandler(s)}>Delete</Button>
                  </td>
                </tr>
              )
            )}
            {students.length === 0 && (
              <tr>
                <td colSpan="7" className="text-center">
                  No students yet. Click “+ Add Student” to create one.
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
