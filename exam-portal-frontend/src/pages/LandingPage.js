import React, { useEffect } from "react";
import { Container, Row, Col, Button } from "react-bootstrap";
import { Link, useNavigate } from "react-router-dom";
import {
  FaBrain,
  FaChartLine,
  FaFilePdf,
  FaUserShield,
  FaRegLightbulb,
  FaClipboardCheck,
} from "react-icons/fa";
import { homePathForRoles } from "../components/ProtectedRoute";
import "./LandingPage.css";

/*
 * Public marketing homepage at "/".
 * Functionality note: this page is presentational only. Real auth/business
 * logic lives in the existing pages; here we just route visitors to /login
 * and /register. Already-authenticated visitors are bounced to their
 * dashboard (same rule the Login page uses).
 */

const FEATURES = [
  {
    icon: <FaBrain />,
    title: "Validated psychometric engine",
    text: "Assessments scored against established trait models — not ad-hoc quizzes. Consistent, repeatable results every time.",
  },
  {
    icon: <FaChartLine />,
    title: "Clear, visual profiles",
    text: "Every result becomes an at-a-glance profile: strengths, tendencies, and growth areas mapped across each dimension.",
  },
  {
    icon: <FaFilePdf />,
    title: "Full report, one click",
    text: "Generate a detailed multi-page PDF report per student — ready to share with guardians, counsellors, or the classroom.",
  },
  {
    icon: <FaUserShield />,
    title: "Role-based access",
    text: "Students, teachers, and administrators each see exactly what they should. Ownership is enforced on the server, every request.",
  },
  {
    icon: <FaClipboardCheck />,
    title: "Guided assessment flow",
    text: "A calm, distraction-free experience walks each student through the assessment from onboarding to submission.",
  },
  {
    icon: <FaRegLightbulb />,
    title: "Insight that guides action",
    text: "Turn scores into next steps. Reports surface what to nurture and where support helps most.",
  },
];

const STEPS = [
  {
    title: "Students take the assessment",
    text: "A guided, self-paced flow — accessible on any device, no setup required for the student.",
  },
  {
    title: "The engine scores the results",
    text: "Responses are scored against the psychometric model the moment the assessment is submitted.",
  },
  {
    title: "Reports reach the right people",
    text: "Students see their profile; teachers and administrators get the reporting and PDF export they need.",
  },
];

const AUDIENCE = [
  {
    tag: "Students",
    title: "Understand yourself better",
    text: "A clear, encouraging picture of your strengths and how you learn — no jargon, no pass or fail.",
  },
  {
    tag: "Teachers & Schools",
    title: "Support every learner",
    text: "See your whole cohort at a glance and step in with the right support, backed by real data.",
  },
  {
    tag: "Administrators",
    title: "Run it with confidence",
    text: "Manage assessments, users, and results across the platform from one secure dashboard.",
  },
];

const LandingPage = () => {
  const navigate = useNavigate();

  // Authenticated visitors don't need the marketing page — send them home.
  useEffect(() => {
    const token = localStorage.getItem("jwtToken");
    if (!token) return;
    try {
      const user = JSON.parse(localStorage.getItem("user"));
      const roles = user && user.roles ? user.roles.map((r) => r.roleName) : [];
      navigate(homePathForRoles(roles), { replace: true });
    } catch (e) {
      /* not logged in with a valid user blob; stay on the landing page */
    }
  }, [navigate]);

  return (
    <main className="mt-landing">
      {/* ---------- Hero ---------- */}
      <section className="mt-hero">
        <Container>
          <Row className="align-items-center g-5">
            <Col lg={6}>
              <span className="mt-eyebrow">The Mentalist Wellness Platform</span>
              <h1 className="mt-hero-title">
                Psychometric insight that{" "}
                <span className="accent">helps students grow</span>.
              </h1>
              <p className="mt-hero-lead">
                The Mentalist turns a single assessment into a clear, actionable
                profile — so students understand themselves and educators know
                exactly how to help.
              </p>
              <div className="mt-hero-cta">
                <Link to="/register">
                  <Button className="mt-btn-lg mt-btn-accent">Get started</Button>
                </Link>
                <Link to="/login">
                  <Button className="mt-btn-lg mt-btn-ghost">Sign in</Button>
                </Link>
              </div>
              <p className="mt-hero-note">
                Built for schools, counsellors, and the students they serve.
              </p>
            </Col>

            <Col lg={6}>
              <div className="mt-hero-card" role="img" aria-label="Example psychometric profile showing trait scores">
                {[
                  ["Openness", 82],
                  ["Conscientiousness", 74],
                  ["Resilience", 68],
                  ["Focus", 90],
                  ["Empathy", 79],
                ].map(([trait, score]) => (
                  <div className="rowline" key={trait}>
                    <span className="trait">{trait}</span>
                    <span className="bar">
                      <span style={{ width: `${score}%` }} />
                    </span>
                    <span className="score">{score}</span>
                  </div>
                ))}
              </div>
            </Col>
          </Row>
        </Container>
      </section>

      {/* ---------- Features ---------- */}
      <section className="mt-section" id="features">
        <Container>
          <div className="mt-section-head">
            <span className="mt-eyebrow">Why The Mentalist</span>
            <h2 className="mt-section-title">
              Everything you need to assess, understand, and act
            </h2>
            <p className="mt-section-sub">
              One platform from the first question to the final report.
            </p>
          </div>
          <Row className="g-4">
            {FEATURES.map((f) => (
              <Col md={6} lg={4} key={f.title}>
                <div className="mt-feature">
                  <div className="mt-feature-icon" aria-hidden="true">
                    {f.icon}
                  </div>
                  <h3>{f.title}</h3>
                  <p>{f.text}</p>
                </div>
              </Col>
            ))}
          </Row>
        </Container>
      </section>

      {/* ---------- How it works ---------- */}
      <section className="mt-section" id="how" style={{ background: "var(--mt-surface)" }}>
        <Container>
          <div className="mt-section-head">
            <span className="mt-eyebrow">How it works</span>
            <h2 className="mt-section-title">From assessment to insight in three steps</h2>
          </div>
          <Row className="mt-steps g-5">
            {STEPS.map((s) => (
              <Col md={4} key={s.title}>
                <div className="mt-step">
                  <h3>{s.title}</h3>
                  <p>{s.text}</p>
                </div>
              </Col>
            ))}
          </Row>
        </Container>
      </section>

      {/* ---------- Audience ---------- */}
      <section className="mt-section" id="audience">
        <Container>
          <div className="mt-section-head">
            <span className="mt-eyebrow">Built for everyone involved</span>
            <h2 className="mt-section-title">One platform, three points of view</h2>
          </div>
          <Row className="g-4">
            {AUDIENCE.map((a) => (
              <Col md={4} key={a.tag}>
                <div className="mt-audience">
                  <span className="tag">{a.tag}</span>
                  <h3>{a.title}</h3>
                  <p>{a.text}</p>
                </div>
              </Col>
            ))}
          </Row>
        </Container>
      </section>

      {/* ---------- Final CTA ---------- */}
      <section className="mt-section" id="cta">
        <Container>
          <div className="mt-cta">
            <h2>Ready to see what an assessment can reveal?</h2>
            <p>
              Create an account and run your first psychometric profile today.
            </p>
            <div className="mt-hero-cta justify-content-center">
              <Link to="/register">
                <Button className="mt-btn-lg mt-btn-accent">Get started free</Button>
              </Link>
              <Link to="/login">
                <Button className="mt-btn-lg mt-btn-ghost">Sign in</Button>
              </Link>
            </div>
          </div>
        </Container>
      </section>

      {/* ---------- Footer ---------- */}
      <footer className="mt-foot">
        <Container>
          <Row className="align-items-center g-3">
            <Col md={6}>
              <span className="brand">The Mentalist</span>
              <p className="mb-0 mt-2" style={{ maxWidth: "38ch" }}>
                Psychometric assessment and reporting for schools and the
                students they serve.
              </p>
            </Col>
            <Col md={6} className="text-md-end">
              <Link to="/login" className="me-4">Sign in</Link>
              <Link to="/register">Create account</Link>
              <p className="mb-0 mt-3" style={{ opacity: 0.6 }}>
                © {new Date().getFullYear()} The Mentalist. All rights reserved.
              </p>
            </Col>
          </Row>
        </Container>
      </footer>
    </main>
  );
};

export default LandingPage;
