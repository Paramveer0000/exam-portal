import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Badge, Button, Card, Form } from "react-bootstrap";
import swal from "sweetalert";
import SuperAdminSidebar from "../../components/SuperAdminSidebar";
import Loader from "../../components/Loader";
import aiServices from "../../services/aiServices";
import platformServices from "../../services/platformServices";

const DEFAULT_SYSTEM_PROMPT =
  "You are a supportive school career counsellor writing for a student and their parents. " +
  "Interpret ONLY the scores provided; never invent numbers. Use warm, plain, encouraging " +
  "language (no clinical jargon). Structure: a short opening about their strongest " +
  "intelligences, a note on their interest style, then concrete, realistic career directions " +
  "to explore, and one gentle growth tip. 150-250 words, a few short paragraphs.";

const SAMPLE_INPUT =
  "Student: Jane Doe\nTest: Grade 9 Psychometric\n\n" +
  "Multiple Intelligences (percent, rank):\n- LOGICAL: 22% (rank 1)\n- VERBAL: 18% (rank 2)\n...\n\n" +
  "Learning domains:\n- Analytical: 74%\n...\n\n" +
  "RIASEC interest code: IAR\n- I Investigative: 8/10\n...\n\n" +
  "Quotients:\n- IQ: 82%\n...\n\n" +
  "Top recommended career fields (best first):\n- Engineering (5/5)\n...\n\n" +
  "Write the counsellor's narrative now.";

const PROVIDERS = [
  { value: "openai", label: "OpenAI", baseUrl: "https://api.openai.com/v1", model: "gpt-4o-mini" },
  { value: "openrouter", label: "OpenRouter", baseUrl: "https://openrouter.ai/api/v1", model: "openai/gpt-4o-mini" },
  { value: "groq", label: "Groq", baseUrl: "https://api.groq.com/openai/v1", model: "llama-3.1-8b-instant" },
  { value: "custom", label: "Custom (OpenAI-compatible)", baseUrl: "", model: "" },
];

const SuperAdminAiSettingsPage = () => {
  const navigate = useNavigate();

  const [loaded, setLoaded] = useState(false);
  const [provider, setProvider] = useState("openai");
  const [baseUrl, setBaseUrl] = useState("");
  const [model, setModel] = useState("");
  const [systemPrompt, setSystemPrompt] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [keyConfigured, setKeyConfigured] = useState(false);
  const [keyHint, setKeyHint] = useState(null);
  const [saving, setSaving] = useState(false);
  const [testing, setTesting] = useState(false);
  const [testStatus, setTestStatus] = useState(null); // "live" | "down" | null

  const [companyLogo, setCompanyLogo] = useState(null);
  const [logoBusy, setLogoBusy] = useState(false);

  useEffect(() => {
    // Auth guard handled by ProtectedRoute; cookie-based auth needs no check here.
  }, []);

  useEffect(() => {
    platformServices.getBranding().then(({ companyLogo }) =>
      setCompanyLogo(companyLogo)
    );
  }, []);

  const onLogoChange = (e) => {
    const file = e.target.files && e.target.files[0];
    e.target.value = "";
    if (!file) return;
    if (file.type !== "image/png") {
      swal("PNG only", "Please choose a PNG image.", "info");
      return;
    }
    if (file.size > 1024 * 1024) {
      swal("Too large", "Please choose a PNG under 1 MB.", "info");
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      setLogoBusy(true);
      platformServices.updateBranding(reader.result).then(({ companyLogo, error }) => {
        setLogoBusy(false);
        if (error) swal("Upload failed", error, "error");
        else {
          setCompanyLogo(companyLogo);
          swal("Saved", "Company logo updated. Reload to see it in the header.", "success").then(
            () => window.location.reload()
          );
        }
      });
    };
    reader.readAsDataURL(file);
  };

  const clearLogo = () => {
    setLogoBusy(true);
    platformServices.updateBranding(null).then(({ error }) => {
      setLogoBusy(false);
      if (error) swal("Failed", error, "error");
      else {
        setCompanyLogo(null);
        swal("Cleared", "Company logo removed.", "success").then(() =>
          window.location.reload()
        );
      }
    });
  };

  useEffect(() => {
    aiServices.getSettings().then(({ data, error }) => {
      if (data) {
        setProvider(data.provider || "openai");
        setBaseUrl(data.baseUrl || "");
        setModel(data.model || "");
        setSystemPrompt(data.systemPrompt || "");
        setKeyConfigured(data.keyConfigured);
        setKeyHint(data.keyHint);
      } else {
        swal("Could not load", error || "Try again", "error");
      }
      setLoaded(true);
    });
  }, []);

  const onProviderChange = (val) => {
    setProvider(val);
    const preset = PROVIDERS.find((p) => p.value === val);
    if (preset && preset.value !== "custom") {
      setBaseUrl(preset.baseUrl);
      if (!model) setModel(preset.model);
    }
  };

  const testConnection = async () => {
    setTesting(true);
    const { data, error } = await aiServices.testSettings();
    setTesting(false);
    if (data) {
      setTestStatus("live");
      swal("Live", `Provider responded: ${data.reply}`, "success");
    } else {
      setTestStatus("down");
      swal("Test failed", error || "Try again", "error");
    }
  };

  const save = (e) => {
    e.preventDefault();
    setSaving(true);
    // apiKey blank => keep the stored key unchanged.
    const payload = { provider, baseUrl, model, systemPrompt };
    if (apiKey.trim()) payload.apiKey = apiKey.trim();
    aiServices.updateSettings(payload).then(({ data, error }) => {
      setSaving(false);
      if (data) {
        setKeyConfigured(data.keyConfigured);
        setKeyHint(data.keyHint);
        setSystemPrompt(data.systemPrompt || "");
        setApiKey("");
        swal("Saved", "AI settings updated.", "success");
      } else {
        swal("Save failed", error || "Try again", "error");
      }
    });
  };

  return (
    <div style={{ display: "flex" }}>
      <SuperAdminSidebar />
      <div className="mt-page" style={{ maxWidth: "640px" }}>
        <h2 style={{ color: "var(--mt-primary)" }}>Platform Settings</h2>

        <Card body className="mb-4 mt-card">
          <h4>Company Logo</h4>
          <p className="text-muted">
            Shown in the top-left header on every page for all users. Clicking it
            takes the user to their dashboard.
          </p>
          <div style={{ display: "flex", alignItems: "center", gap: "16px", flexWrap: "wrap" }}>
            <div
              style={{
                width: "160px",
                height: "60px",
                border: "1px solid #ddd",
                borderRadius: "6px",
                background: "#222",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
              }}
            >
              {companyLogo ? (
                <img
                  src={companyLogo}
                  alt="Company logo"
                  style={{ maxHeight: "48px", maxWidth: "150px", objectFit: "contain" }}
                />
              ) : (
                <span style={{ color: "#fff" }}>Exam-Portal</span>
              )}
            </div>
            <div>
              <Form.Label
                htmlFor="company-logo-upload"
                className="btn btn-outline-primary btn-sm mb-0"
                style={{ cursor: logoBusy ? "default" : "pointer" }}
              >
                {logoBusy ? "Uploading…" : companyLogo ? "Change logo" : "Upload logo"}
              </Form.Label>
              <input
                id="company-logo-upload"
                type="file"
                accept="image/png"
                hidden
                disabled={logoBusy}
                onChange={onLogoChange}
              />
              {companyLogo && (
                <Button
                  variant="link"
                  size="sm"
                  className="text-danger"
                  disabled={logoBusy}
                  onClick={clearLogo}
                >
                  Remove
                </Button>
              )}
              <div style={{ fontSize: "0.8rem", color: "#888", marginTop: "4px" }}>
                PNG only · wide/landscape recommended (e.g. 200×60 px) · max 1 MB
              </div>
            </div>
          </div>
        </Card>

        <h4>AI Settings</h4>
        <p className="text-muted">
          One platform-wide LLM key lets super admins prepare premium AI reports
          for schools. Uses an OpenAI-compatible chat API. The key is stored
          securely on the server and never shown again after saving.
        </p>
        {!loaded ? (
          <Loader />
        ) : (
          <Card body className="mt-card">
            <Form onSubmit={save}>
              <Form.Group className="mb-3">
                <Form.Label>Provider</Form.Label>
                <Form.Select
                  value={provider}
                  onChange={(e) => onProviderChange(e.target.value)}
                >
                  {PROVIDERS.map((p) => (
                    <option key={p.value} value={p.value}>
                      {p.label}
                    </option>
                  ))}
                </Form.Select>
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Base URL</Form.Label>
                <Form.Control
                  value={baseUrl}
                  required
                  placeholder="https://api.openai.com/v1"
                  onChange={(e) => setBaseUrl(e.target.value)}
                />
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Model</Form.Label>
                <Form.Control
                  value={model}
                  required
                  placeholder="gpt-4o-mini"
                  onChange={(e) => setModel(e.target.value)}
                />
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>System Prompt</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={6}
                  value={systemPrompt}
                  placeholder={DEFAULT_SYSTEM_PROMPT}
                  onChange={(e) => setSystemPrompt(e.target.value)}
                />
                <Form.Text muted>
                  Instructions the AI follows when writing a student's report narrative. Leave
                  blank to use the default shown above. The student's actual scores are always
                  appended automatically as the input — see below.
                </Form.Text>
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Report input (auto-generated, not editable)</Form.Label>
                <Form.Control as="textarea" rows={6} value={SAMPLE_INPUT} readOnly disabled />
                <Form.Text muted>
                  Example of what gets sent as the user message for each student — built from
                  their own quiz scores at report time.
                </Form.Text>
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>API Key</Form.Label>
                <Form.Control
                  type="password"
                  value={apiKey}
                  placeholder={
                    keyConfigured
                      ? `Key set (${keyHint}). Leave blank to keep it.`
                      : "Paste your API key"
                  }
                  onChange={(e) => setApiKey(e.target.value)}
                />
                <Form.Text muted>
                  {keyConfigured
                    ? "A key is configured. Enter a new one only to replace it."
                    : "No key configured yet — AI reports are disabled until you add one."}
                </Form.Text>
              </Form.Group>

              <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                <Button type="submit" variant="primary" disabled={saving}>
                  {saving ? "Saving..." : "Save settings"}
                </Button>
                <Button
                  type="button"
                  variant="outline-secondary"
                  disabled={testing || !keyConfigured}
                  onClick={testConnection}
                >
                  {testing ? "Testing..." : "Test connection"}
                </Button>
                {testStatus && (
                  <Badge bg={testStatus === "live" ? "success" : "danger"}>
                    {testStatus === "live" ? "Live" : "Down"}
                  </Badge>
                )}
              </div>
            </Form>
          </Card>
        )}
      </div>
    </div>
  );
};

export default SuperAdminAiSettingsPage;
