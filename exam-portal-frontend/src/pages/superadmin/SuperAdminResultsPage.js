import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Accordion, Badge, Form, Table } from "react-bootstrap";
import { BsSearch } from "react-icons/bs";
import SuperAdminSidebar from "../../components/SuperAdminSidebar";
import Loader from "../../components/Loader";
import Message from "../../components/Message";
import adminServices from "../../services/adminServices";

const initials = (name) =>
  (name || "?")
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join("");

const SuperAdminResultsPage = () => {
  const navigate = useNavigate();
  const token = JSON.parse(localStorage.getItem("jwtToken"));

  const [schools, setSchools] = useState(null);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");

  useEffect(() => {
    if (!token) navigate("/");
  }, []);

  useEffect(() => {
    adminServices.fetchResultsBySchool(token).then(({ data, error }) => {
      if (data) setSchools(data);
      else setError(error || "Could not load results");
    });
  }, []);

  const schoolAttempts = (s) =>
    (s.students || []).reduce((n, st) => n + (st.results || []).length, 0);

  const filteredSchools = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!schools) return schools;
    if (!q) return schools;
    return schools.filter((s) => (s.schoolName || "").toLowerCase().includes(q));
  }, [schools, search]);

  return (
    <div style={{ display: "flex" }}>
      <SuperAdminSidebar />
      <div className="mt-page">
        <h2 style={{ color: "var(--mt-primary)" }}>All Results by School</h2>
        <p className="text-muted">
          Every attempt, grouped by partner school and then by student.
        </p>

        {error && <Message variant="danger">{error}</Message>}
        {!schools && !error && <Loader />}

        {schools && schools.length === 0 && (
          <Message>No results yet.</Message>
        )}

        {schools && schools.length > 0 && (
          <>
            <div className="mt-search mb-3" style={{ maxWidth: 320 }}>
              <BsSearch />
              <Form.Control
                placeholder="Search schools"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
            <Accordion alwaysOpen className="mt-card">
            {filteredSchools.map((s, si) => (
              <Accordion.Item eventKey={String(si)} key={si}>
                <Accordion.Header>
                  {s.schoolName}{" "}
                  <Badge bg="secondary" className="ms-2">
                    {(s.students || []).length} student
                    {(s.students || []).length === 1 ? "" : "s"}
                  </Badge>
                  <Badge bg="info" className="ms-2">
                    {schoolAttempts(s)} attempt
                    {schoolAttempts(s) === 1 ? "" : "s"}
                  </Badge>
                </Accordion.Header>
                <Accordion.Body>
                  {(s.students || []).map((st) => (
                    <div key={st.studentId} className="mb-4">
                      <h6 className="d-flex align-items-center gap-2">
                        <span className="mt-avatar">{initials(st.studentName || st.username)}</span>
                        {st.studentName}{" "}
                        <span className="text-muted">({st.username})</span>
                      </h6>
                      <Table striped bordered size="sm" responsive>
                        <thead>
                          <tr>
                            <th>Class</th>
                            <th>Quiz</th>
                            <th>Date</th>
                            <th>Report</th>
                          </tr>
                        </thead>
                        <tbody>
                          {(st.results || []).map((r) => (
                            <tr key={r.quizResId}>
                              <td>{r.className}</td>
                              <td>{r.quizTitle}</td>
                              <td>{r.attemptDatetime}</td>
                              <td>
                                <a
                                  href={`/psychometricReport/${r.quizResId}`}
                                  onClick={(e) => {
                                    e.preventDefault();
                                    navigate(
                                      `/psychometricReport/${r.quizResId}`
                                    );
                                  }}
                                >
                                  View
                                </a>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </Table>
                    </div>
                  ))}
                </Accordion.Body>
              </Accordion.Item>
            ))}
            </Accordion>
          </>
        )}
      </div>
    </div>
  );
};

export default SuperAdminResultsPage;
