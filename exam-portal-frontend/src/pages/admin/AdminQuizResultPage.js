import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Accordion, Badge, Table } from "react-bootstrap";
import RoleSidebar from "../../components/RoleSidebar";
import Loader from "../../components/Loader";
import Message from "../../components/Message";
import adminServices from "../../services/adminServices";

const AdminQuizResultPage = () => {
  const navigate = useNavigate();
  const token = JSON.parse(localStorage.getItem("jwtToken"));

  const [classes, setClasses] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!token) {
      navigate("/");
      return;
    }
    adminServices.fetchResultsByClass(token).then(({ data, error }) => {
      if (data) setClasses(data);
      else setError(error || "Could not load results");
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const classAttempts = (c) =>
    (c.students || []).reduce((n, st) => n + (st.results || []).length, 0);

  return (
    <div style={{ display: "flex" }}>
      <RoleSidebar />
      <div className="mt-page">
        <h2 style={{ color: "var(--mt-primary)" }}>All Results by Class</h2>
        <p className="text-muted">
          Your students' attempts, grouped by class and then by student.
        </p>

        {error && <Message variant="danger">{error}</Message>}
        {!classes && !error && <Loader />}

        {classes && classes.length === 0 && <Message>No results yet.</Message>}

        {classes && classes.length > 0 && (
          <Accordion alwaysOpen className="mt-card">
            {classes.map((c, ci) => (
              <Accordion.Item eventKey={String(ci)} key={ci}>
                <Accordion.Header>
                  {c.className}{" "}
                  <Badge bg="secondary" className="ms-2">
                    {(c.students || []).length} student
                    {(c.students || []).length === 1 ? "" : "s"}
                  </Badge>
                  <Badge bg="info" className="ms-2">
                    {classAttempts(c)} attempt
                    {classAttempts(c) === 1 ? "" : "s"}
                  </Badge>
                </Accordion.Header>
                <Accordion.Body>
                  {(c.students || []).map((st) => (
                    <div key={st.studentId} className="mb-4">
                      <h6 style={{ display: "flex", alignItems: "center", gap: 8 }}>
                        <div className="mt-avatar">
                          {(st.studentName?.[0] || st.username?.[0] || "?").toUpperCase()}
                        </div>
                        {st.studentName}{" "}
                        <span className="text-muted">({st.username})</span>
                      </h6>
                      <Table striped bordered size="sm" responsive>
                        <thead>
                          <tr>
                            <th>Subject</th>
                            <th>Date</th>
                            <th>Report</th>
                          </tr>
                        </thead>
                        <tbody>
                          {(st.results || []).map((r) => (
                            <tr key={r.quizResId}>
                              <td>{r.quizTitle}</td>
                              <td>{r.attemptDatetime}</td>
                              <td>
                                <a
                                  href={`/psychometricReport/${r.quizResId}`}
                                  onClick={(e) => {
                                    e.preventDefault();
                                    navigate(`/psychometricReport/${r.quizResId}`);
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
        )}
      </div>
    </div>
  );
};

export default AdminQuizResultPage;
