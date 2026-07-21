import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Accordion, Badge, Table } from "react-bootstrap";
import SuperAdminSidebar from "../../components/SuperAdminSidebar";
import Loader from "../../components/Loader";
import Message from "../../components/Message";
import adminServices from "../../services/adminServices";

const SuperAdminResultsPage = () => {
  const navigate = useNavigate();
  const token = JSON.parse(localStorage.getItem("jwtToken"));

  const [schools, setSchools] = useState(null);
  const [error, setError] = useState("");

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

  return (
    <div style={{ display: "flex" }}>
      <SuperAdminSidebar />
      <div style={{ padding: "1.5rem", flexGrow: 1, maxWidth: "1000px" }}>
        <h2>All Results by School</h2>
        <p className="text-muted">
          Every attempt, grouped by partner school and then by student.
        </p>

        {error && <Message variant="danger">{error}</Message>}
        {!schools && !error && <Loader />}

        {schools && schools.length === 0 && (
          <Message>No results yet.</Message>
        )}

        {schools && schools.length > 0 && (
          <Accordion alwaysOpen>
            {schools.map((s, si) => (
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
                      <h6>
                        {st.studentName}{" "}
                        <span className="text-muted">({st.username})</span>
                      </h6>
                      <Table striped bordered size="sm" responsive>
                        <thead>
                          <tr>
                            <th>Class</th>
                            <th>Subject</th>
                            <th>Marks</th>
                            <th>Result</th>
                            <th>Date</th>
                            <th>Report</th>
                          </tr>
                        </thead>
                        <tbody>
                          {(st.results || []).map((r) => (
                            <tr key={r.quizResId}>
                              <td>{r.className}</td>
                              <td>{r.quizTitle}</td>
                              <td>
                                {r.obtainedMarks} / {r.totalMarks}
                              </td>
                              <td>
                                <Badge bg={r.passed ? "success" : "danger"}>
                                  {r.passed ? "Passed" : "Failed"}
                                </Badge>
                              </td>
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
        )}
      </div>
    </div>
  );
};

export default SuperAdminResultsPage;
