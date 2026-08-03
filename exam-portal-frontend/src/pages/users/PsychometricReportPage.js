import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { Button, Spinner, Table } from "react-bootstrap";
import Loader from "../../components/Loader";
import Message from "../../components/Message";
import { fetchPsychReport } from "../../actions/psychometricReportActions";
import aiServices from "../../services/aiServices";
import mentalistReportServices from "../../services/mentalistReportServices";
import "./PsychometricReportPage.css";

// Human names for the MI dimension codes (our own wording throughout).
const MI_LABELS = {
  LOGICAL: "Logical-Mathematical",
  MUSICAL: "Musical-Rhythmic",
  NATURALIST: "Naturalistic",
  VERBAL: "Verbal-Linguistic",
  INTERPERSONAL: "Interpersonal",
  KINESTHETIC: "Bodily-Kinesthetic",
  SPATIAL: "Visual-Spatial",
  INTRAPERSONAL: "Intrapersonal",
  EXISTENTIAL: "Existential",
};

const DOMAIN_HINTS = {
  Analytical: "How you process information: reasoning, patterns and systems.",
  Interactive: "How you engage the world: language, people and action.",
  Introspective: "How you look inward: imagery, self-knowledge and meaning.",
};

const PsychometricReportPage = () => {
  const { quizResId } = useParams();
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { loading, report, error } = useSelector(
    (state) => state.psychometricReportReducer
  );

  const [aiSummary, setAiSummary] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);
  const [pdfLoading, setPdfLoading] = useState(false);
  const [pdfError, setPdfError] = useState(null);

  const generateAi = (regenerate = false) => {
    setAiLoading(true);
    return aiServices
      .generateSummary(quizResId, regenerate)
      .then(({ summary, error }) => {
        setAiLoading(false);
        if (summary) {
          setAiSummary(summary);
          return summary;
        }
        setAiSummary(`__error__:${error || "Could not generate report"}`);
        return null;
      });
  };

  // Download only appears once the AI narrative exists. Generates the
  // 15-page PDF server-side (or reuses the previously generated one) then
  // saves it via the browser.
  const downloadPdf = async () => {
    setPdfLoading(true);
    setPdfError(null);
    const gen = await mentalistReportServices.generateReport(quizResId, {});
    if (!gen.data) {
      setPdfLoading(false);
      setPdfError(gen.error || "Could not generate the report");
      return;
    }
    const { blob, error } = await mentalistReportServices.downloadReport(quizResId);
    setPdfLoading(false);
    if (!blob) {
      setPdfError(error || "Could not download the report");
      return;
    }
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `mentalist-report-${quizResId}.pdf`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  };

  useEffect(() => {
    if (!localStorage.getItem("user")) navigate("/");
  }, []);

  useEffect(() => {
    fetchPsychReport(dispatch, quizResId);
  }, [quizResId]);

  if (loading || (!report && !error)) return <Loader />;
  if (error) return <Message variant="danger">{error}</Message>;

  return (
    <div className="psychReport__container">
      <Button
        variant="secondary"
        className="psychReport__backBtn"
        onClick={() => navigate(-1)}
      >
        ← Back
      </Button>

      <div className="psychReport__header">
        <h1>Psychometric &amp; Career Guidance Report</h1>
        <div className="psychReport__meta">
          <span>
            <strong>Student:</strong> {report.studentName}
          </span>
          <span>
            <strong>Test:</strong> {report.quizTitle}
          </span>
          <span>
            <strong>Attempt:</strong> #{report.attemptNumber} —{" "}
            {report.attemptDatetime}
          </span>
          <span>
            <strong>Interest Code:</strong> {report.hollandCode}
          </span>
        </div>
      </div>

      <div className="psychReport__actions">
        {aiSummary && !aiSummary.startsWith("__error__:") ? (
          <Button variant="success" onClick={downloadPdf} disabled={pdfLoading}>
            {pdfLoading ? (
              <>
                <Spinner as="span" size="sm" animation="border" /> Preparing PDF…
              </>
            ) : (
              "Download The Mentalist Report (PDF)"
            )}
          </Button>
        ) : (
          <Button
            variant="primary"
            disabled={aiLoading}
            onClick={() => generateAi(false)}
          >
            {aiLoading ? (
              <>
                <Spinner as="span" size="sm" animation="border" /> Generating…
              </>
            ) : (
              "✨ Generate AI Report"
            )}
          </Button>
        )}
      </div>

      {pdfError && (
        <div className="psychReport__section">
          <Message variant="warning">{pdfError}</Message>
        </div>
      )}

      {aiSummary &&
        (aiSummary.startsWith("__error__:") ? (
          <div className="psychReport__section">
            <Message variant="warning">
              {aiSummary.replace("__error__:", "")}
            </Message>
          </div>
        ) : (
          <div className="psychReport__section psychReport__ai">
            <h2>AI Counsellor's Summary</h2>
            {aiSummary.split(/\n\s*\n/).map((para, i) => (
              <p key={i}>{para}</p>
            ))}
          </div>
        ))}

      <div className="psychReport__section">
        <h2>Multiple Intelligences</h2>
        <p>
          Your responses are grouped into nine ways of thinking and learning.
          The percentage shows each one's share of your overall profile; rank 1
          is your strongest.
        </p>
        <Table striped bordered size="sm">
          <thead>
            <tr>
              <th>Intelligence</th>
              <th style={{ width: "40%" }}>Strength</th>
              <th>Percent</th>
              <th>Rank</th>
            </tr>
          </thead>
          <tbody>
            {report.multipleIntelligences.map((row) => (
              <tr key={row.dimension}>
                <td>{MI_LABELS[row.dimension] || row.dimension}</td>
                <td>
                  <div className="psychReport__bar">
                    <div style={{ width: `${Math.min(100, row.percent * 4)}%` }} />
                  </div>
                </td>
                <td>{row.percent}%</td>
                <td>{row.rank}</td>
              </tr>
            ))}
          </tbody>
        </Table>
      </div>

      <div className="psychReport__section">
        <h2>Learning Domains</h2>
        <Table bordered size="sm">
          <thead>
            <tr>
              <th>Domain</th>
              <th>Share</th>
              <th>What it covers</th>
            </tr>
          </thead>
          <tbody>
            {report.domains.map((d) => (
              <tr key={d.name}>
                <td>
                  <strong>{d.name}</strong>
                </td>
                <td>{d.percent}%</td>
                <td>{DOMAIN_HINTS[d.name]}</td>
              </tr>
            ))}
          </tbody>
        </Table>
      </div>

      <div className="psychReport__section">
        <h2>Career Interest Profile (RIASEC)</h2>
        <p>
          Six work-personality types scored as a percentage. Your three
          strongest (highlighted) form your interest code:{" "}
          <strong>{report.hollandCode}</strong>.
        </p>
        <Table bordered size="sm">
          <thead>
            <tr>
              <th>Type</th>
              <th style={{ width: "40%" }}>Score</th>
              <th>Percent</th>
            </tr>
          </thead>
          <tbody>
            {report.riasec.map((r) => (
              <tr
                key={r.letter}
                className={r.dominant ? "psychReport__riasec--dominant" : ""}
              >
                <td>
                  {r.letter} — {r.name}
                </td>
                <td>
                  <div className="psychReport__bar">
                    <div style={{ width: `${r.score * 10}%` }} />
                  </div>
                </td>
                <td>{r.score * 10}%</td>
              </tr>
            ))}
          </tbody>
        </Table>
      </div>

      <div className="psychReport__section">
        <h2>Your Quotients</h2>
        <Table bordered size="sm">
          <thead>
            <tr>
              <th>Quotient</th>
              <th style={{ width: "40%" }}>Level</th>
              <th>Percent</th>
            </tr>
          </thead>
          <tbody>
            {report.quotients.map((q) => (
              <tr key={q.code}>
                <td>
                  <strong>{q.code}</strong> — {q.name}
                </td>
                <td>
                  <div className="psychReport__bar">
                    <div style={{ width: `${q.percent}%` }} />
                  </div>
                </td>
                <td>{q.percent}%</td>
              </tr>
            ))}
          </tbody>
        </Table>
      </div>

      <div className="psychReport__section psychReport__careers">
        <h2>Recommended Career Fields</h2>
        <p>
          Fields are ranked by how strongly your dominant intelligences and
          interests support them. More stars = a better fit.
        </p>
        {report.careers.map((cRow, i) => (
          <div className="psychReport__careerRow" key={cRow.field}>
            <div>
              <strong>
                {i + 1}. {cRow.field}
              </strong>
              <div style={{ color: "#666", fontSize: "0.9rem" }}>
                {cRow.label}
              </div>
            </div>
            <span className="psychReport__stars" title={`${cRow.score}/100`}>
              {"★".repeat(cRow.stars)}
              {"☆".repeat(5 - cRow.stars)}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};

export default PsychometricReportPage;
