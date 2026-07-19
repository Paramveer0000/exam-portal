import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Button, Col, Form, Row, Table } from "react-bootstrap";
import Sidebar from "../../components/Sidebar";
import { fetchQuizzes } from "../../actions/quizzesActions";
import { fetchQuizReport, exportQuizReport } from "../../actions/reportsActions";

const AdminReportsPage = () => {
  const dispatch = useDispatch();
  const token = JSON.parse(localStorage.getItem("jwtToken"));
  const quizzesReducer = useSelector((state) => state.quizzesReducer);
  const reportsReducer = useSelector((state) => state.reportsReducer);

  const [quizId, setQuizId] = useState("");
  const [passed, setPassed] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");

  useEffect(() => {
    if (quizzesReducer.quizzes.length === 0) {
      fetchQuizzes(dispatch, token);
    }
  }, []);

  const filters = { passed, from, to };

  const runReport = (e) => {
    if (e) e.preventDefault();
    if (quizId) {
      fetchQuizReport(dispatch, quizId, filters, token);
    }
  };

  const exportHandler = () => {
    if (quizId) {
      exportQuizReport(quizId, filters, token);
    }
  };

  // Group subjects by class for the dropdown (class-wise <optgroup>s).
  const subjectGroups = (() => {
    const groups = {};
    const order = [];
    quizzesReducer.quizzes.forEach((q) => {
      const cname = q.category ? q.category.title : "Uncategorized";
      if (!groups[cname]) {
        groups[cname] = [];
        order.push(cname);
      }
      groups[cname].push(q);
    });
    return order.map((cname) => ({ className: cname, subjects: groups[cname] }));
  })();

  return (
    <div style={{ display: "flex" }}>
      <Sidebar />
      <div style={{ padding: "1.5rem", flexGrow: 1 }}>
        <h2>Reports</h2>
        <Form onSubmit={runReport}>
          <Row className="align-items-end">
            <Col md={4} className="mb-2">
              <Form.Label>Subject</Form.Label>
              <Form.Select
                value={quizId}
                onChange={(e) => setQuizId(e.target.value)}
              >
                <option value="">Choose a subject</option>
                {subjectGroups.map((group) => (
                  <optgroup key={group.className} label={group.className}>
                    {group.subjects.map((q) => (
                      <option key={q.quizId} value={q.quizId}>
                        {q.title}
                      </option>
                    ))}
                  </optgroup>
                ))}
              </Form.Select>
            </Col>
            <Col md={2} className="mb-2">
              <Form.Label>Result</Form.Label>
              <Form.Select
                value={passed}
                onChange={(e) => setPassed(e.target.value)}
              >
                <option value="">All</option>
                <option value="true">Passed</option>
                <option value="false">Failed</option>
              </Form.Select>
            </Col>
            <Col md={2} className="mb-2">
              <Form.Label>From</Form.Label>
              <Form.Control
                type="date"
                value={from}
                onChange={(e) => setFrom(e.target.value)}
              />
            </Col>
            <Col md={2} className="mb-2">
              <Form.Label>To</Form.Label>
              <Form.Control
                type="date"
                value={to}
                onChange={(e) => setTo(e.target.value)}
              />
            </Col>
            <Col md={2} className="mb-2">
              <Button type="submit" variant="primary" className="me-2">
                Run
              </Button>
              <Button variant="success" onClick={exportHandler} disabled={!quizId}>
                Export
              </Button>
            </Col>
          </Row>
        </Form>

        {reportsReducer.error && (
          <p style={{ color: "red" }}>{reportsReducer.error}</p>
        )}

        <Table striped bordered hover responsive className="mt-3">
          <thead>
            <tr>
              <th>Student</th>
              <th>Subject</th>
              <th>Score</th>
              <th>Total</th>
              <th>Result</th>
              <th>Attempt</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            {reportsReducer.rows.map((r) => (
              <tr key={r.resultId}>
                <td>{r.studentName}</td>
                <td>{r.quizTitle}</td>
                <td>{r.totalObtainedMarks}</td>
                <td>{r.totalMarks}</td>
                <td>{r.passed ? "Pass" : "Fail"}</td>
                <td>{r.attemptNumber}</td>
                <td>{r.attemptDatetime}</td>
              </tr>
            ))}
            {reportsReducer.rows.length === 0 && (
              <tr>
                <td colSpan="7" className="text-center">
                  No results. Choose a quiz and click Run.
                </td>
              </tr>
            )}
          </tbody>
        </Table>
      </div>
    </div>
  );
};

export default AdminReportsPage;
