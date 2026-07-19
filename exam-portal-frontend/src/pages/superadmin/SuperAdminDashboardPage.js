import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Button, Card, Col, Form, Row, Table } from "react-bootstrap";
import swal from "sweetalert";
import SuperAdminSidebar from "../../components/SuperAdminSidebar";
import {
  fetchAdmins,
  fetchAnalytics,
  fetchMetrics,
  fetchUnowned,
  reassignUnowned,
} from "../../actions/adminActions";

const SuperAdminDashboardPage = () => {
  const dispatch = useDispatch();
  const token = JSON.parse(localStorage.getItem("jwtToken"));
  const { admins, metrics, analytics, unowned } = useSelector(
    (state) => state.adminReducer
  );
  const [reassignTarget, setReassignTarget] = useState("");

  useEffect(() => {
    fetchAdmins(dispatch, token);
    fetchMetrics(dispatch, token);
    fetchAnalytics(dispatch, token);
    fetchUnowned(dispatch, token);
  }, []);

  const reassignHandler = () => {
    if (!reassignTarget) {
      swal("Pick an admin", "Choose an admin to receive the content", "info");
      return;
    }
    reassignUnowned(dispatch, reassignTarget, token).then((data) => {
      if (data.type === "REASSIGN_UNOWNED_SUCCESS") {
        swal(
          "Reassigned",
          `${data.payload.categoriesReassigned} categories and ${data.payload.quizzesReassigned} quizzes reassigned`,
          "success"
        );
        fetchUnowned(dispatch, token);
      } else {
        swal("Failed", data.payload || "Could not reassign", "error");
      }
    });
  };

  const metricCards = [
    { label: "Schools", value: metrics?.totalAdmins },
    { label: "Students", value: metrics?.totalStudents },
    { label: "Classes", value: metrics?.totalCategories },
    { label: "Subjects", value: metrics?.totalQuizzes },
    { label: "Attempts", value: metrics?.totalAttempts },
    {
      label: "Pass rate",
      value: metrics ? `${metrics.passRate.toFixed(1)}%` : undefined,
    },
  ];

  return (
    <div style={{ display: "flex" }}>
      <SuperAdminSidebar />
      <div style={{ padding: "1.5rem", flexGrow: 1, maxWidth: "1100px" }}>
        <h2>Super Admin Dashboard</h2>

        <Row className="my-3">
          {metricCards.map((m) => (
            <Col key={m.label} xs={6} md={2} className="mb-3">
              <Card body className="text-center">
                <div style={{ fontSize: "1.4rem", fontWeight: 600 }}>
                  {m.value ?? "-"}
                </div>
                <div style={{ color: "#666" }}>{m.label}</div>
              </Card>
            </Col>
          ))}
        </Row>

        <Card body className="my-3">
          <h4>School performance</h4>
          <Table striped bordered hover responsive className="mt-2">
            <thead>
              <tr>
                <th>School</th>
                <th>Students</th>
                <th>Classes</th>
                <th>Subjects</th>
                <th>Exams conducted</th>
                <th>Attempts</th>
                <th>Pass rate</th>
              </tr>
            </thead>
            <tbody>
              {analytics.map((a) => (
                <tr key={a.adminId}>
                  <td>{a.name ? `${a.name} (${a.username})` : a.username}</td>
                  <td>{a.students}</td>
                  <td>{a.classes}</td>
                  <td>{a.subjects}</td>
                  <td>{a.examsConducted}</td>
                  <td>{a.attempts}</td>
                  <td>{a.passRate.toFixed(1)}%</td>
                </tr>
              ))}
              {analytics.length === 0 && (
                <tr>
                  <td colSpan="7" className="text-center">
                    No schools yet.
                  </td>
                </tr>
              )}
            </tbody>
          </Table>
        </Card>

        <Card body className="my-3">
          <h4>Unassigned (legacy) content</h4>
          {unowned &&
          unowned.categories.length === 0 &&
          unowned.quizzes.length === 0 ? (
            <p className="mb-0">
              Nothing unassigned — every class and subject has an owner.
            </p>
          ) : (
            <>
              <p>
                These were created before ownership existed and are visible only
                to Super Admins. Assign them to an admin to make them manageable.
              </p>
              <Row>
                <Col md={6}>
                  <strong>
                    Classes ({unowned ? unowned.categories.length : 0})
                  </strong>
                  <ul>
                    {unowned &&
                      unowned.categories.map((c) => (
                        <li key={c.id}>{c.title}</li>
                      ))}
                  </ul>
                </Col>
                <Col md={6}>
                  <strong>
                    Subjects ({unowned ? unowned.quizzes.length : 0})
                  </strong>
                  <ul>
                    {unowned &&
                      unowned.quizzes.map((q) => <li key={q.id}>{q.title}</li>)}
                  </ul>
                </Col>
              </Row>
              <Row className="align-items-end">
                <Col md={5} className="mb-2">
                  <Form.Select
                    value={reassignTarget}
                    onChange={(e) => setReassignTarget(e.target.value)}
                  >
                    <option value="">Choose a school…</option>
                    {admins.map((a) => (
                      <option key={a.userId} value={a.userId}>
                        {a.username}
                      </option>
                    ))}
                  </Form.Select>
                </Col>
                <Col md={4} className="mb-2">
                  <Button variant="primary" onClick={reassignHandler}>
                    Assign all to selected school
                  </Button>
                </Col>
              </Row>
            </>
          )}
        </Card>
      </div>
    </div>
  );
};

export default SuperAdminDashboardPage;
