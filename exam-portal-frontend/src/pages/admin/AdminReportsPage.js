import React, { useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Button, Col, Form, Row, Table } from "react-bootstrap";
import { BsSortDown, BsSortUp } from "react-icons/bs";
import Sidebar from "../../components/Sidebar";
import { fetchQuizzes } from "../../actions/quizzesActions";
import { fetchQuizReport, exportQuizReport } from "../../actions/reportsActions";

const AdminReportsPage = () => {
  const dispatch = useDispatch();
  const quizzesReducer = useSelector((state) => state.quizzesReducer);
  const reportsReducer = useSelector((state) => state.reportsReducer);

  const [quizId, setQuizId] = useState("");
  const [passed, setPassed] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [sort, setSort] = useState({ key: "studentName", dir: 1 });

  useEffect(() => {
    if (quizzesReducer.quizzes.length === 0) {
      fetchQuizzes(dispatch);
    }
  }, []);

  const filters = { passed, from, to };

  const runReport = (e) => {
    if (e) e.preventDefault();
    if (quizId) {
      fetchQuizReport(dispatch, quizId, filters);
    }
  };

  const exportHandler = () => {
    if (quizId) {
      exportQuizReport(quizId, filters);
    }
  };

  // Group quizzes by class for the dropdown (class-wise <optgroup>s).
  const quizGroups = (() => {
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
    return order.map((cname) => ({ className: cname, quizzes: groups[cname] }));
  })();

  const toggleSort = (key) =>
    setSort((prev) => (prev.key === key ? { key, dir: -prev.dir } : { key, dir: 1 }));

  const sortIcon = (key) =>
    sort.key !== key ? null : sort.dir === 1 ? (
      <BsSortUp className="ms-1" />
    ) : (
      <BsSortDown className="ms-1" />
    );

  const sortedRows = useMemo(() => {
    const dir = sort.dir;
    return [...reportsReducer.rows].sort((a, b) => {
      if (sort.key === "attemptNumber")
        return dir * ((a.attemptNumber || 0) - (b.attemptNumber || 0));
      return dir * `${a[sort.key] || ""}`.localeCompare(`${b[sort.key] || ""}`);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reportsReducer.rows, sort]);

  return (
    <div style={{ display: "flex" }}>
      <Sidebar />
      <div className="mt-page">
        <h2 style={{ color: "var(--mt-primary)" }}>Reports</h2>
        <Form onSubmit={runReport}>
          <Row className="align-items-end">
            <Col md={4} className="mb-2">
              <Form.Label>Quiz</Form.Label>
              <Form.Select
                value={quizId}
                onChange={(e) => setQuizId(e.target.value)}
              >
                <option value="">Choose a quiz</option>
                {quizGroups.map((group) => (
                  <optgroup key={group.className} label={group.className}>
                    {group.quizzes.map((q) => (
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
              <th className="mt-sort-th" onClick={() => toggleSort("studentName")}>
                Student {sortIcon("studentName")}
              </th>
              <th className="mt-sort-th" onClick={() => toggleSort("quizTitle")}>
                Quiz {sortIcon("quizTitle")}
              </th>
              <th className="mt-sort-th" onClick={() => toggleSort("attemptNumber")}>
                Attempt {sortIcon("attemptNumber")}
              </th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            {sortedRows.map((r) => (
              <tr key={r.resultId}>
                <td>{r.studentName}</td>
                <td>{r.quizTitle}</td>
                <td>{r.attemptNumber}</td>
                <td>{r.attemptDatetime}</td>
              </tr>
            ))}
            {reportsReducer.rows.length === 0 && (
              <tr>
                <td colSpan="4" className="text-center">
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
