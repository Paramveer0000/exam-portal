import React, { useEffect, useState } from "react";
import { Badge, Button, Form, Table } from "react-bootstrap";
import { BsSearch, BsSortDown, BsSortUp } from "react-icons/bs";
import swal from "sweetalert";
import RoleSidebar from "../../components/RoleSidebar";
import studentsServices from "../../services/studentsServices";
import categoriesServices from "../../services/categoriesServices";
import authServices from "../../services/authServices";

const emptyNew = {
  username: "",
  password: "",
  firstName: "",
  lastName: "",
  phoneNumber: "",
  classId: "",
};

const AdminStudentsPage = () => {
  const [students, setStudents] = useState([]);
  const [error, setError] = useState("");
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState({});
  const [allClasses, setAllClasses] = useState([]);
  const [showCreate, setShowCreate] = useState(false);
  const [newStudent, setNewStudent] = useState(emptyNew);
  const [schoolsById, setSchoolsById] = useState({});
  const [search, setSearch] = useState("");
  const [sort, setSort] = useState({ key: "name", dir: 1 });

  const currentUser = JSON.parse(localStorage.getItem("user"));
  const isSuperAdmin =
    currentUser &&
    currentUser.roles &&
    currentUser.roles.some((r) => r.roleName === "SUPER_ADMIN");

  const load = () => {
    studentsServices.fetchStudents().then(({ data, error }) => {
      if (data) setStudents(data);
      else setError(error || "Could not load students");
    });
  };

  useEffect(() => {
    load();
    categoriesServices.fetchCategories().then((data) => {
      if (Array.isArray(data)) setAllClasses(data);
    });
    if (isSuperAdmin) {
      authServices.getTeachers().then((data) => {
        if (Array.isArray(data)) {
          const map = {};
          data.forEach((t) => (map[t.userId] = t.name));
          setSchoolsById(map);
        }
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const classTitle = (classId) => {
    const c = allClasses.find((x) => x.catId === classId);
    return c ? c.title : classId ? `#${classId}` : "—";
  };

  const schoolName = (teacherId) =>
    teacherId ? schoolsById[teacherId] || `#${teacherId}` : "—";

  const setNewField = (e) =>
    setNewStudent((prev) => ({ ...prev, [e.target.name]: e.target.value }));

  const createHandler = (e) => {
    e.preventDefault();
    if (!newStudent.classId) {
      swal("Class required", "Pick a class for the student", "warning");
      return;
    }
    studentsServices
      .createStudent({ ...newStudent, classId: Number(newStudent.classId) })
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
    studentsServices.setClass(s.userId, Number(classId)).then(({ data, error }) => {
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
    studentsServices.updateStudent(s.userId, form).then(({ data, error }) => {
      if (data) {
        setStudents((prev) => prev.map((x) => (x.userId === data.userId ? data : x)));
        cancelEdit();
      } else {
        swal("Not saved", error || "Could not update student", "error");
      }
    });
  };

  const toggleStatus = (s) => {
    studentsServices.setStatus(s.userId, !s.active).then(({ data }) => {
      if (data)
        setStudents((prev) => prev.map((x) => (x.userId === data.userId ? data : x)));
    });
  };

  const resetHandler = (s) => {
    const newPassword = window.prompt(`New password for ${s.username}`);
    if (newPassword) {
      studentsServices.resetPassword(s.userId, newPassword).then(({ ok, error }) => {
        swal(
          ok ? "Password reset" : "Failed",
          ok ? `Updated for ${s.username}` : error || "Could not reset",
          ok ? "success" : "error"
        );
      });
    }
  };

  const toggleSort = (key) =>
    setSort((prev) => (prev.key === key ? { key, dir: -prev.dir } : { key, dir: 1 }));

  const sortIcon = (key) =>
    sort.key !== key ? null : sort.dir === 1 ? (
      <BsSortUp className="ms-1" />
    ) : (
      <BsSortDown className="ms-1" />
    );

  const studentName = (s) => `${s.firstName || ""} ${s.lastName || ""}`.trim() || s.username;

  const initials = (s) =>
    ((s.firstName?.[0] || s.username?.[0] || "?") + (s.lastName?.[0] || "")).toUpperCase();

  const filtered = students
    .filter((s) => {
      const q = search.trim().toLowerCase();
      if (!q) return true;
      const name = `${s.firstName || ""} ${s.lastName || ""} ${s.username || ""}`.toLowerCase();
      return name.includes(q);
    })
    .sort((a, b) => {
      const dir = sort.dir;
      if (sort.key === "id") return dir * (a.userId - b.userId);
      return dir * studentName(a).localeCompare(studentName(b));
    });

  const groups = [];
  const groupIndex = {};
  filtered.forEach((s) => {
    const key = isSuperAdmin ? s.teacherId || "unassigned" : s.classId || "unassigned";
    if (!(key in groupIndex)) {
      groupIndex[key] = groups.length;
      groups.push({ key, classId: s.classId, teacherId: s.teacherId, students: [] });
    }
    groups[groupIndex[key]].students.push(s);
  });
  groups.sort((a, b) =>
    isSuperAdmin
      ? schoolName(a.teacherId).localeCompare(schoolName(b.teacherId))
      : classTitle(a.classId).localeCompare(classTitle(b.classId))
  );

  const deleteHandler = (s) => {
    swal({
      title: "Delete this student?",
      text: `${s.username} and all their exam results will be removed.`,
      icon: "warning",
      buttons: true,
      dangerMode: true,
    }).then((confirmed) => {
      if (confirmed) {
        studentsServices.deleteStudent(s.userId).then(({ ok, error }) => {
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
      <RoleSidebar />
      <div className="mt-page">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <h2 style={{ color: "var(--mt-primary)" }}>My Students</h2>
          {!isSuperAdmin && (
            <Button variant="success" onClick={() => setShowCreate((v) => !v)}>
              {showCreate ? "Close" : "+ Add Student"}
            </Button>
          )}
        </div>
        {error && <p style={{ color: "red" }}>{error}</p>}

        <div className="mt-search mt-3" style={{ maxWidth: 300 }}>
          <BsSearch />
          <Form.Control
            placeholder="Search by student name"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

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

        {groups.map((g) => (
        <div key={g.key} className="mt-4">
          <h5>
            {isSuperAdmin ? schoolName(g.teacherId) : classTitle(g.classId)}{" "}
            <span className="text-muted">({g.students.length})</span>
          </h5>
          <Table striped bordered hover responsive>
          <thead>
            <tr>
              <th className="mt-sort-th" onClick={() => toggleSort("id")}>
                ID {sortIcon("id")}
              </th>
              <th>Username</th>
              <th className="mt-sort-th" onClick={() => toggleSort("name")}>
                Name {sortIcon("name")}
              </th>
              <th>Phone</th>
              <th>Class</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {g.students.map((s) =>
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
                  <td>
                    <Badge bg={s.active ? "success" : "secondary"}>
                      {s.active ? "Active" : "Disabled"}
                    </Badge>
                  </td>
                  <td style={{ display: "flex", gap: "4px", flexWrap: "wrap" }}>
                    <Button size="sm" variant="success" onClick={() => saveEdit(s)}>Save</Button>
                    <Button size="sm" variant="light" onClick={cancelEdit}>Cancel</Button>
                  </td>
                </tr>
              ) : (
                <tr key={s.userId}>
                  <td>{s.userId}</td>
                  <td>{s.username}</td>
                  <td>
                    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                      <div className="mt-avatar">{initials(s)}</div>
                      {studentName(s)}
                    </div>
                  </td>
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
                  <td>
                    <Badge bg={s.active ? "success" : "secondary"}>
                      {s.active ? "Active" : "Disabled"}
                    </Badge>
                  </td>
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
          </tbody>
          </Table>
        </div>
        ))}
        {groups.length === 0 && (
          <p className="text-center text-muted mt-4">
            {students.length === 0
              ? 'No students yet. Click "+ Add Student" to create one.'
              : "No students match your search."}
          </p>
        )}
      </div>
    </div>
  );
};

export default AdminStudentsPage;
