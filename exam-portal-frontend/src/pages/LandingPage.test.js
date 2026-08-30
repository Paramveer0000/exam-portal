import React from "react";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import LandingPage from "./LandingPage";

beforeEach(() => {
  localStorage.clear();
  window.IntersectionObserver = class {
    observe() {}
    disconnect() {}
  };
  window.CSS = { escape: (value) => value };
});

test("shows the Jalandhar center address and direct contact details in the footer", () => {
  render(
    <MemoryRouter>
      <LandingPage />
    </MemoryRouter>
  );

  expect(screen.getByText("SCF- 84, Urban Estate, PH-1, Jalandhar.")).toBeInTheDocument();
  expect(screen.getByRole("link", { name: /thementalistofficial21@gmail\.com/i })).toHaveAttribute(
    "href",
    "mailto:thementalistofficial21@gmail.com"
  );
  expect(screen.getByRole("link", { name: /@official_thementalist/i })).toHaveAttribute(
    "href",
    "https://instagram.com/Official_thementalist"
  );
});
