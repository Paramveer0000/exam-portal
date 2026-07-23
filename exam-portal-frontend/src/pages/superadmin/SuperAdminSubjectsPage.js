import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button, Form, Table } from "react-bootstrap";
import swal from "sweetalert";
import SuperAdminSidebar from "../../components/SuperAdminSidebar";
import subjectsServices from "../../services/subjectsServices";
import categoriesServices from "../../services/categoriesServices";

const emptyForm = { title: "", description: "", classId: "" };

const SuperAdminSubjectsPage = () => {
  const navigate = useNavigate();
  const token = JSON.parse(localStorage.getItem("jwtToken"));

  const [subjects, setSubjects] = useState([]);
  const [classes, setClasses] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);

  useEffect(() => {
    if (!token) navigate("/");
    load();
    categoriesServices.fetchCategories(token).then((data) => {
      if (Array.isArray(data)) setClasses(data);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const load = () => {
    subjectsServices.fetchSubjects(token).then(({ data }) => {
      if (Array.isArray(data)) setSubjects(data);
    });
  };

  const classTitle = (classId) => {
    const c = classes.find((x) => x.catId === classId);
    return c ? c.title : `#${classId}`;
  };

  const setField = (e) =>
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));

  const resetForm = () => {
    setForm(emptyForm);
    setEditingId(null);
  };

  const submit = (e) => {
    e.preventDefault();
    if (!form.classId) {
      swal("Class required", "Pick a class for the subject", "warning");
      return;
    }
    const payload = { ...form, classId: Number(form.classId) };
    const call = editingId
      ? subjectsServices.updateSubject({ ...payload, subjectId: editingId }, token)
      : subjectsServices.addSubject(payload, token);
    call.then(({ data, error }) => {
      if (data) {
        swal("Saved", `${data.title} saved`, "success");
        resetForm();
        load();
      } else {
        swal("Not saved", error || "Could not save subject", "error");
      }
    });
  };

  const startEdit = (s) => {
    setEditingId(s.subjectId);
    setForm({ title: s.title, description: s.description || "", classId: s.classId });
  };

  const remove = (s) => {
    swal({
      title: "Delete this subject?",
      text: `${s.title} and its quizzes will be removed.`,
      icon: "warning",
      buttons: true,
      dangerMode: true,
    }).then((confirmed) => {
      if (confirmed) {
        subjectsServices.deleteSubject(s.subjectId, token).then(({ ok, error }) => {
          if (ok) {
            swal("Deleted", `${s.title} removed`, "success");
            load();
          } else {
            swal("Not deleted", error || "Could not delete", "error");
          }
        });
      }
    });
  };

  return (
    <SuperAdminSidebar>
      <div style={{ padding: "1.5rem", flexGrow: 1 }}>
        <h2>Subjects</h2>
        <p className="text-muted">Subjects live under a class. Quizzes live under a subject.</p>

        <Form onSubmit={submit} className="p-3 mb-3" style={{ background: "#f6f9ff", borderRadius: 8 }}>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "10px", alignItems: "center" }}>
            <Form.Control name="title" placeholder="Subject title *" value={form.title} onChange={setField} required style={{ maxWidth: 200 }} />
            <Form.Control name="description" placeholder="Description" value={form.description} onChange={setField} style={{ maxWidth: 260 }} />
            <Form.Select name="classId" value={form.classId} onChange={setField} required style={{ maxWidth: 200 }}>
              <option value="">Select class *</option>
              {classes.map((c) => (
                <option key={c.catId} value={c.catId}>{c.title}</option>
              ))}
            </Form.Select>
            <Button type="submit" variant="primary">{editingId ? "Update" : "Add"}</Button>
            {editingId && <Button variant="light" onClick={resetForm}>Cancel</Button>}
          </div>
          {classes.length === 0 && (
            <p className="mb-0 mt-2 text-muted">No classes exist yet. Create a class first.</p>
          )}
        </Form>

        <Table striped bordered hover responsive>
          <thead>
            <tr><th>ID</th><th>Subject</th><th>Class</th><th>Description</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {subjects.map((s) => (
              <tr key={s.subjectId}>
                <td>{s.subjectId}</td>
                <td>{s.title}</td>
                <td>{classTitle(s.classId)}</td>
                <td>{s.description}</td>
                <td style={{ display: "flex", gap: "4px" }}>
                  <Button size="sm" variant="primary" onClick={() => startEdit(s)}>Edit</Button>
                  <Button size="sm" variant="danger" onClick={() => remove(s)}>Delete</Button>
                </td>
              </tr>
            ))}
            {subjects.length === 0 && (
              <tr><td colSpan="5" className="text-center">No subjects yet.</td></tr>
            )}
          </tbody>
        </Table>
      </div>
    </SuperAdminSidebar>
  );
};

export default SuperAdminSubjectsPage;
