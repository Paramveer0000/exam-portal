import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Badge, Card, Col, Row, Table } from "react-bootstrap";
import {
  BsCollection,
  BsLightningCharge,
  BsPeopleFill,
  BsSortDown,
  BsSortUp,
} from "react-icons/bs";
import Sidebar from "../../components/Sidebar";
import Loader from "../../components/Loader";
import studentsServices from "../../services/studentsServices";
import categoriesServices from "../../services/categoriesServices";
import quizResultServices from "../../services/quizResultServices";

// Dashboard for a school (ADMIN): its own students, their attempts and results.
// Everything is scoped server-side to this school's students.
const AdminDashboardPage = () => {
  const navigate = useNavigate();
  const token = JSON.parse(localStorage.getItem("jwtToken"));

  const [students, setStudents] = useState(null);
  const [results, setResults] = useState(null);
  const [classCount, setClassCount] = useState(0);
  const [sort, setSort] = useState({ key: "name", dir: 1 });

  useEffect(() => {
    if (!token) navigate("/");
  }, []);

  useEffect(() => {
    studentsServices.fetchStudents(token).then(({ data }) => setStudents(data || []));
    quizResultServices.fetchQuizResult(null, token).then((data) =>
      setResults(Array.isArray(data) ? data : [])
    );
    categoriesServices.fetchCategories(token).then((data) =>
      setClassCount(Array.isArray(data) ? data.length : 0)
    );
  }, []);

  const attempts = results ? results.length : 0;

  // Per-student attempt tally.
  const nameById = {};
  (students || []).forEach((s) => {
    nameById[s.userId] = `${s.firstName || ""} ${s.lastName || ""}`.trim() || s.username;
  });
  const perStudent = {};
  (results || []).forEach((r) => {
    const id = r.userId;
    if (!perStudent[id]) perStudent[id] = { attempts: 0 };
    perStudent[id].attempts += 1;
  });

  const sortedStudents = useMemo(() => {
    const dir = sort.dir;
    return [...(students || [])].sort((a, b) => {
      if (sort.key === "attempts")
        return dir * ((perStudent[a.userId]?.attempts || 0) - (perStudent[b.userId]?.attempts || 0));
      return dir * nameById[a.userId].localeCompare(nameById[b.userId]);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [students, sort]);

  if (!students || !results) return <Loader />;

  const tiles = [
    { label: "My Students", value: students.length, icon: <BsPeopleFill />, color: "var(--mt-primary)" },
    { label: "Exam Attempts", value: attempts, icon: <BsLightningCharge />, color: "var(--mt-danger)" },
    { label: "Classes Available", value: classCount, icon: <BsCollection />, color: "var(--mt-accent-dark)" },
  ];

  const toggleSort = (key) =>
    setSort((prev) => (prev.key === key ? { key, dir: -prev.dir } : { key, dir: 1 }));

  const sortIcon = (key) =>
    sort.key !== key ? null : sort.dir === 1 ? (
      <BsSortUp className="ms-1" />
    ) : (
      <BsSortDown className="ms-1" />
    );

  return (
    <div style={{ display: "flex" }}>
      <Sidebar />
      <div className="mt-page">
        <h2 style={{ color: "var(--mt-primary)" }}>School Dashboard</h2>

        <Row className="my-3">
          {tiles.map((t) => (
            <Col key={t.label} xs={6} md={2} className="mb-3">
              <Card body className="mt-card mt-card-hover">
                <div className="mt-stat-icon" style={{ background: t.color }}>
                  {t.icon}
                </div>
                <div style={{ fontSize: "var(--mt-fs-2xl)", fontWeight: 700, color: "var(--mt-primary)" }}>
                  {t.value}
                </div>
                <div style={{ color: "var(--mt-text-muted)", fontSize: "var(--mt-fs-sm)" }}>
                  {t.label}
                </div>
              </Card>
            </Col>
          ))}
        </Row>

        <Card body className="my-3 mt-card">
          <h4>My Students</h4>
          <Table striped bordered hover responsive className="mt-2">
            <thead>
              <tr>
                <th className="mt-sort-th" onClick={() => toggleSort("name")}>
                  Student {sortIcon("name")}
                </th>
                <th>Username</th>
                <th>Status</th>
                <th className="mt-sort-th" onClick={() => toggleSort("attempts")}>
                  Attempts {sortIcon("attempts")}
                </th>
                <th>Passed</th>
              </tr>
            </thead>
            <tbody>
              {sortedStudents.map((s) => (
                <tr key={s.userId}>
                  <td>
                    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                      <div className="mt-avatar">
                        {(s.firstName?.[0] || s.username?.[0] || "?").toUpperCase()}
                        {(s.lastName?.[0] || "").toUpperCase()}
                      </div>
                      {nameById[s.userId]}
                    </div>
                  </td>
                  <td>{s.username}</td>
                  <td>
                    <Badge bg={s.active ? "success" : "secondary"}>
                      {s.active ? "Active" : "Disabled"}
                    </Badge>
                  </td>
                  <td>{perStudent[s.userId]?.attempts || 0}</td>
                  <td>{perStudent[s.userId]?.passed || 0}</td>
                </tr>
              ))}
              {students.length === 0 && (
                <tr>
                  <td colSpan="5" className="text-center">
                    No students yet. Students appear here once they register under
                    you.
                  </td>
                </tr>
              )}
            </tbody>
          </Table>
          <p className="text-muted mb-0">
            Assign classes to students from the <strong>Students</strong> page.
          </p>
        </Card>
      </div>
    </div>
  );
};

export default AdminDashboardPage;
