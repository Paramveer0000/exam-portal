import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Badge, Card, Col, Row, Table } from "react-bootstrap";
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

  if (!students || !results) return <Loader />;

  const attempts = results.length;
  const passed = results.filter((r) => r.passed || r.isPassed).length;
  const passRate = attempts ? ((passed / attempts) * 100).toFixed(1) : "0.0";

  // Per-student attempt / pass tally.
  const nameById = {};
  students.forEach((s) => {
    nameById[s.userId] = `${s.firstName || ""} ${s.lastName || ""}`.trim() || s.username;
  });
  const perStudent = {};
  results.forEach((r) => {
    const id = r.userId;
    if (!perStudent[id]) perStudent[id] = { attempts: 0, passed: 0 };
    perStudent[id].attempts += 1;
    if (r.passed || r.isPassed) perStudent[id].passed += 1;
  });

  const tiles = [
    { label: "My Students", value: students.length },
    { label: "Exam Attempts", value: attempts },
    { label: "Passed", value: passed },
    { label: "Pass Rate", value: `${passRate}%` },
    { label: "Classes Available", value: classCount },
  ];

  return (
    <div style={{ display: "flex" }}>
      <Sidebar />
      <div style={{ padding: "1.5rem", flexGrow: 1, maxWidth: "1100px" }}>
        <h2>School Dashboard</h2>

        <Row className="my-3">
          {tiles.map((t) => (
            <Col key={t.label} xs={6} md={2} className="mb-3">
              <Card body className="text-center">
                <div style={{ fontSize: "1.4rem", fontWeight: 600 }}>{t.value}</div>
                <div style={{ color: "#666" }}>{t.label}</div>
              </Card>
            </Col>
          ))}
        </Row>

        <Card body className="my-3">
          <h4>My Students</h4>
          <Table striped bordered hover responsive className="mt-2">
            <thead>
              <tr>
                <th>Student</th>
                <th>Username</th>
                <th>Status</th>
                <th>Attempts</th>
                <th>Passed</th>
              </tr>
            </thead>
            <tbody>
              {students.map((s) => (
                <tr key={s.userId}>
                  <td>{nameById[s.userId]}</td>
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
