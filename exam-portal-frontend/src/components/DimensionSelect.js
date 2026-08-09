import React, { useState, useEffect } from "react";
import { Form, Button, Badge } from "react-bootstrap";

const DimensionSelect = ({ id, value, onChange, blankLabel, isMulti = false, weights, onWeightsChange }) => {
  const [dimensions, setDimensions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch("/api/dimensions")
      .then((res) => res.json())
      .then((data) => {
        setDimensions(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error("Failed to fetch dimensions:", err);
        setLoading(false);
      });
  }, []);

  const groupByType = (dims) => {
    const groups = {};
    dims.forEach((d) => {
      if (!groups[d.dimensionType]) {
        groups[d.dimensionType] = [];
      }
      groups[d.dimensionType].push(d);
    });
    return groups;
  };

  const typeLabels = {
    MI: "Multiple Intelligences",
    RIASEC: "Career Interest (RIASEC)",
    LEARNING_PREF: "Learning Preference",
    CAREER_INTEREST: "Career Interest Assessment",
    EQ: "EQ",
    LEADERSHIP: "Leadership & Soft Skills",
  };

  if (loading) {
    return <Form.Select disabled>{blankLabel || "Loading..."}</Form.Select>;
  }

  if (!isMulti) {
    // Single-select mode for option dimension overrides
    const grouped = groupByType(dimensions);
    return (
      <Form.Select aria-label="Choose Dimension" id={id} value={value || ""} onChange={onChange}>
        <option value="">{blankLabel || "Optional"}</option>
        {Object.keys(grouped).map((type) => (
          <optgroup key={type} label={typeLabels[type] || type}>
            {grouped[type].map((d) => (
              <option key={d.dimensionCode} value={d.dimensionCode}>
                {d.displayName}
              </option>
            ))}
          </optgroup>
        ))}
      </Form.Select>
    );
  }

  // Add-one-by-one mode for question dimensions: pick from dropdown, click
  // Add, chosen dimension shows as a removable chip below.
  return (
    <DimensionMultiPicker
      id={id}
      value={value}
      onChange={onChange}
      dimensions={dimensions}
      groupByType={groupByType}
      typeLabels={typeLabels}
      weights={weights}
      onWeightsChange={onWeightsChange}
    />
  );
};

const DimensionMultiPicker = ({
  id,
  value,
  onChange,
  dimensions,
  groupByType,
  typeLabels,
  weights,
  onWeightsChange,
}) => {
  const [pending, setPending] = useState("");
  const selectedSet = value instanceof Set ? value : new Set(value || []);
  const grouped = groupByType(dimensions);
  const byCode = Object.fromEntries(dimensions.map((d) => [d.dimensionCode, d]));

  const addPending = () => {
    if (!pending) return;
    const next = new Set(selectedSet);
    next.add(pending);
    onChange(next);
    setPending("");
  };

  const removeCode = (code) => {
    const next = new Set(selectedSet);
    next.delete(code);
    onChange(next);
    if (onWeightsChange && weights) {
      const nextWeights = { ...weights };
      delete nextWeights[code];
      onWeightsChange(nextWeights);
    }
  };

  const weightsEnabled = !!onWeightsChange;
  const setWeight = (code, raw) => {
    const next = { ...(weights || {}) };
    if (raw === "") {
      delete next[code];
    } else {
      next[code] = Number(raw) / 100;
    }
    onWeightsChange(next);
  };

  const codes = Array.from(selectedSet);
  const anyWeightEntered = weightsEnabled && codes.some((c) => weights && weights[c] !== undefined);
  const totalPercent = anyWeightEntered
    ? Math.round(codes.reduce((sum, c) => sum + ((weights && weights[c]) || 0), 0) * 1000) / 10
    : null;
  const totalValid = totalPercent !== null && Math.abs(totalPercent - 100) < 0.1;

  return (
    <div>
      <div className="d-flex gap-2">
        <Form.Select
          aria-label="Choose a dimension to add"
          id={id}
          value={pending}
          onChange={(e) => setPending(e.target.value)}
        >
          <option value="">Choose a dimension…</option>
          {Object.keys(grouped).map((type) => (
            <optgroup key={type} label={typeLabels[type] || type}>
              {grouped[type].map((d) => (
                <option
                  key={d.dimensionCode}
                  value={d.dimensionCode}
                  disabled={selectedSet.has(d.dimensionCode)}
                >
                  {d.displayName}
                </option>
              ))}
            </optgroup>
          ))}
        </Form.Select>
        <Button variant="outline-primary" onClick={addPending} disabled={!pending}>
          Add
        </Button>
      </div>
      <div className="mt-2 d-flex flex-wrap gap-2 align-items-center">
        {codes.map((code) => (
          <Badge key={code} bg="secondary" className="d-flex align-items-center gap-1 p-2">
            {byCode[code] ? byCode[code].displayName : code}
            {weightsEnabled && (
              <Form.Control
                type="number"
                min="0"
                max="100"
                step="1"
                aria-label={`Weight for ${code}`}
                placeholder="%"
                value={weights && weights[code] !== undefined ? Math.round(weights[code] * 100) : ""}
                onChange={(e) => setWeight(code, e.target.value)}
                style={{ width: "60px", height: "24px", padding: "0 4px", fontSize: "0.8rem" }}
              />
            )}
            <span
              role="button"
              aria-label={`Remove ${code}`}
              style={{ cursor: "pointer" }}
              onClick={() => removeCode(code)}
            >
              &times;
            </span>
          </Badge>
        ))}
        {codes.length === 0 && (
          <span className="form-text">No dimensions added yet.</span>
        )}
      </div>
      {anyWeightEntered && (
        <div className={`mt-1 ${totalValid ? "text-success" : "text-danger"}`}>
          Total: {totalPercent}% {totalValid ? "✓" : "✕"}
          {!totalValid && (
            <div className="form-text text-danger">Dimension weights must total 100%.</div>
          )}
        </div>
      )}
      {weightsEnabled && !anyWeightEntered && codes.length > 0 && (
        <div className="form-text">
          No weights set — this question splits its score equally across the {codes.length} dimension
          {codes.length > 1 ? "s" : ""} above. Enter a % per dimension to weight them instead (must total 100%).
        </div>
      )}
    </div>
  );
};

export default DimensionSelect;
