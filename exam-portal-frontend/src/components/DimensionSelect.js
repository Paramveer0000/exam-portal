import React from "react";
import { Form } from "react-bootstrap";

// Shared MI/RIASEC dimension list, used by the main "Dimension this question
// measures" select and by each option's optional override select.
const DimensionSelect = ({ id, value, onChange, blankLabel }) => (
  <Form.Select aria-label="Choose Dimension" id={id} value={value} onChange={onChange}>
    <option value="">{blankLabel}</option>
    <optgroup label="Multiple Intelligences">
      <option value="LOGICAL">Logical-Mathematical</option>
      <option value="MUSICAL">Musical-Rhythmic</option>
      <option value="NATURALIST">Naturalistic</option>
      <option value="VERBAL">Verbal-Linguistic</option>
      <option value="INTERPERSONAL">Interpersonal</option>
      <option value="KINESTHETIC">Bodily-Kinesthetic</option>
      <option value="SPATIAL">Visual-Spatial</option>
      <option value="INTRAPERSONAL">Intrapersonal</option>
      <option value="EXISTENTIAL">Existential</option>
    </optgroup>
    <optgroup label="Career Interest (RIASEC)">
      <option value="R">R — Realistic</option>
      <option value="I">I — Investigative</option>
      <option value="A">A — Artistic</option>
      <option value="S">S — Social</option>
      <option value="E">E — Enterprising</option>
      <option value="C">C — Conventional</option>
    </optgroup>
  </Form.Select>
);

export default DimensionSelect;
