import React, { useState, useEffect } from "react";
import { Form } from "react-bootstrap";

const DimensionSelect = ({ id, value, onChange, blankLabel, isMulti = false }) => {
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

  // Multi-select mode for question dimensions
  const handleMultiChange = (e) => {
    const selected = Array.from(e.target.selectedOptions, (option) => option.value);
    onChange(new Set(selected));
  };

  const grouped = groupByType(dimensions);
  const selectedArray = value instanceof Set ? Array.from(value) : value || [];

  return (
    <Form.Select
      aria-label="Choose Dimensions"
      id={id}
      multiple
      value={selectedArray}
      onChange={handleMultiChange}
      size={Math.min(dimensions.length + 1, 10)}
    >
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
};

export default DimensionSelect;
