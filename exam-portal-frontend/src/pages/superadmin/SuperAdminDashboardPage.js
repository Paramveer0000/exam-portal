import React, { useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Button, Card, Col, Form, Row, Table } from "react-bootstrap";
import {
  BsBuilding,
  BsPeopleFill,
  BsCollection,
  BsJournalBookmark,
  BsLightningCharge,
  BsSearch,
  BsSortDown,
  BsSortUp,
  BsTrophy,
} from "react-icons/bs";
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
  const { admins, metrics, analytics, unowned } = useSelector(
    (state) => state.adminReducer
  );
  const [reassignTarget, setReassignTarget] = useState("");
  const [search, setSearch] = useState("");
  const [sort, setSort] = useState({ key: "students", dir: -1 });

  useEffect(() => {
    fetchAdmins(dispatch);
    fetchMetrics(dispatch);
    fetchAnalytics(dispatch);
    fetchUnowned(dispatch);
  }, []);

  const reassignHandler = () => {
    if (!reassignTarget) {
      swal("Pick an admin", "Choose an admin to receive the content", "info");
      return;
    }
    reassignUnowned(dispatch, reassignTarget).then((data) => {
      if (data.type === "REASSIGN_UNOWNED_SUCCESS") {
        swal(
          "Reassigned",
          `${data.payload.categoriesReassigned} categories and ${data.payload.quizzesReassigned} quizzes reassigned`,
          "success"
        );
        fetchUnowned(dispatch);
      } else {
        swal("Failed", data.payload || "Could not reassign", "error");
      }
    });
  };

  const metricCards = [
    {
      label: "Schools",
      value: metrics?.totalAdmins,
      icon: <BsBuilding />,
      color: "var(--mt-primary)",
    },
    {
      label: "Students",
      value: metrics?.totalStudents,
      icon: <BsPeopleFill />,
      color: "var(--mt-secondary)",
    },
    {
      label: "Classes",
      value: metrics?.totalCategories,
      icon: <BsCollection />,
      color: "var(--mt-accent-dark)",
    },
    {
      label: "Quizzes",
      value: metrics?.totalQuizzes,
      icon: <BsJournalBookmark />,
      color: "var(--mt-info)",
    },
    {
      label: "Attempts",
      value: metrics?.totalAttempts,
      icon: <BsLightningCharge />,
      color: "var(--mt-danger)",
    },
  ];

  const toggleSort = (key) =>
    setSort((prev) => (prev.key === key ? { key, dir: -prev.dir } : { key, dir: -1 }));

  const sortIcon = (key) =>
    sort.key !== key ? null : sort.dir === 1 ? (
      <BsSortUp className="ms-1" />
    ) : (
      <BsSortDown className="ms-1" />
    );

  const maxStudents = Math.max(1, ...analytics.map((a) => a.students || 0));

  const rows = useMemo(() => {
    const q = search.trim().toLowerCase();
    const list = analytics.filter((a) => {
      if (!q) return true;
      return `${a.name || ""} ${a.username}`.toLowerCase().includes(q);
    });
    const dir = sort.dir;
    return [...list].sort((a, b) => {
      if (sort.key === "name") {
        const an = a.name || a.username;
        const bn = b.name || b.username;
        return dir * an.localeCompare(bn);
      }
      return dir * ((a[sort.key] || 0) - (b[sort.key] || 0));
    });
  }, [analytics, search, sort]);

  return (
    <div style={{ display: "flex" }}>
      <SuperAdminSidebar />
      <div style={{ padding: "1.5rem", flexGrow: 1, minWidth: 0 }}>
        <h2 style={{ color: "var(--mt-primary)" }}>Super Admin Dashboard</h2>

        <Row className="my-3">
          {metricCards.map((m) => (
            <Col key={m.label} xs={6} md={2} className="mb-3">
              <Card
                body
                style={{
                  borderRadius: "var(--mt-radius-md)",
                  border: "1px solid var(--mt-border)",
                  boxShadow: "var(--mt-shadow-sm)",
                  transition: "transform .15s ease, box-shadow .15s ease",
                  cursor: "default",
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.transform = "translateY(-3px)";
                  e.currentTarget.style.boxShadow = "var(--mt-shadow-lg)";
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.transform = "translateY(0)";
                  e.currentTarget.style.boxShadow = "var(--mt-shadow-sm)";
                }}
              >
                <div
                  style={{
                    width: 40,
                    height: 40,
                    borderRadius: "50%",
                    background: m.color,
                    color: "#fff",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontSize: "1.1rem",
                    marginBottom: 10,
                  }}
                >
                  {m.icon}
                </div>
                <div style={{ fontSize: "var(--mt-fs-2xl)", fontWeight: 700, color: "var(--mt-primary)" }}>
                  {m.value ?? "-"}
                </div>
                <div style={{ color: "var(--mt-text-muted)", fontSize: "var(--mt-fs-sm)" }}>
                  {m.label}
                </div>
              </Card>
            </Col>
          ))}
        </Row>

        <Card
          body
          className="my-3"
          style={{
            borderRadius: "var(--mt-radius-md)",
            border: "1px solid var(--mt-border)",
            boxShadow: "var(--mt-shadow-sm)",
          }}
        >
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              flexWrap: "wrap",
              gap: 8,
            }}
          >
            <h4 className="mb-0">School performance</h4>
            <div style={{ position: "relative", maxWidth: 260, width: "100%" }}>
              <BsSearch
                style={{
                  position: "absolute",
                  left: 12,
                  top: "50%",
                  transform: "translateY(-50%)",
                  color: "var(--mt-text-muted)",
                }}
              />
              <Form.Control
                placeholder="Search schools"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                style={{ paddingLeft: 34 }}
              />
            </div>
          </div>
          <Table striped bordered hover responsive className="mt-3">
            <thead>
              <tr>
                <th role="button" onClick={() => toggleSort("name")}>
                  School {sortIcon("name")}
                </th>
                <th role="button" onClick={() => toggleSort("students")}>
                  Students {sortIcon("students")}
                </th>
                <th role="button" onClick={() => toggleSort("classes")}>
                  Classes {sortIcon("classes")}
                </th>
                <th role="button" onClick={() => toggleSort("quizzes")}>
                  Quizzes {sortIcon("quizzes")}
                </th>
                <th role="button" onClick={() => toggleSort("examsConducted")}>
                  Exams conducted {sortIcon("examsConducted")}
                </th>
                <th role="button" onClick={() => toggleSort("attempts")}>
                  Attempts {sortIcon("attempts")}
                </th>
              </tr>
            </thead>
            <tbody>
              {rows.map((a, i) => (
                <tr key={a.adminId}>
                  <td>
                    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                      {i === 0 && a.students > 0 && (
                        <BsTrophy style={{ color: "var(--mt-accent-dark)" }} title="Top school" />
                      )}
                      {a.name ? `${a.name} (${a.username})` : a.username}
                    </div>
                  </td>
                  <td style={{ minWidth: 160 }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                      <span style={{ minWidth: 18 }}>{a.students}</span>
                      <div
                        style={{
                          flexGrow: 1,
                          height: 6,
                          borderRadius: 3,
                          background: "var(--mt-border)",
                          overflow: "hidden",
                        }}
                      >
                        <div
                          style={{
                            width: `${((a.students || 0) / maxStudents) * 100}%`,
                            height: "100%",
                            background: "var(--mt-secondary)",
                          }}
                        />
                      </div>
                    </div>
                  </td>
                  <td>{a.classes}</td>
                  <td>{a.quizzes}</td>
                  <td>{a.examsConducted}</td>
                  <td>{a.attempts}</td>
                </tr>
              ))}
              {rows.length === 0 && (
                <tr>
                  <td colSpan="6" className="text-center">
                    {analytics.length === 0 ? "No schools yet." : "No schools match your search."}
                  </td>
                </tr>
              )}
            </tbody>
          </Table>
        </Card>

        <Card
          body
          className="my-3"
          style={{
            borderRadius: "var(--mt-radius-md)",
            border: "1px solid var(--mt-border)",
            boxShadow: "var(--mt-shadow-sm)",
          }}
        >
          <h4>Unassigned (legacy) content</h4>
          {unowned &&
          unowned.categories.length === 0 &&
          unowned.quizzes.length === 0 ? (
            <p className="mb-0">
              Nothing unassigned — every class and quiz has an owner.
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
                    Quizzes ({unowned ? unowned.quizzes.length : 0})
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
