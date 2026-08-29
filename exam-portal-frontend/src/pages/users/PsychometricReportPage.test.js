import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { useDispatch, useSelector } from "react-redux";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { fetchPsychReport } from "../../actions/psychometricReportActions";
import mentalistReportServices from "../../services/mentalistReportServices";
import PsychometricReportPage from "./PsychometricReportPage";

jest.mock("react-redux", () => ({
  useDispatch: jest.fn(),
  useSelector: jest.fn(),
}));
jest.mock("../../actions/psychometricReportActions", () => ({
  fetchPsychReport: jest.fn(),
}));
jest.mock("../../services/mentalistReportServices", () => ({
  generateReport: jest.fn(),
  downloadReport: jest.fn(),
}));

const report = {
  studentName: "Meenkashi Mona",
  quizTitle: "IQ Plus",
  attemptNumber: 1,
  attemptDatetime: "2026-08-03 12:42:13",
  hollandCode: "RIA",
  multipleIntelligences: [],
  domains: [],
  riasec: [],
  quotients: [],
  careers: [],
};

const renderPage = (roleName) => {
  localStorage.setItem("user", JSON.stringify({ roles: [{ roleName }] }));
  useDispatch.mockReturnValue(jest.fn());
  useSelector.mockReturnValue({ loading: false, report, error: null });
  fetchPsychReport.mockResolvedValue(undefined);
  return render(
    <MemoryRouter initialEntries={["/psychometricReport/41"]}>
      <Routes>
        <Route path="/psychometricReport/:quizResId" element={<PsychometricReportPage />} />
      </Routes>
    </MemoryRouter>
  );
};

afterEach(() => {
  jest.clearAllMocks();
  localStorage.clear();
});

test("school sees issued-report download only and never triggers PDF generation", async () => {
  mentalistReportServices.downloadReport.mockResolvedValue({
    blob: null,
    error: "Report not prepared yet.",
  });
  renderPage("ADMIN");

  expect(screen.queryByRole("button", { name: /generate ai report/i })).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: /rebuild pdf/i })).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: /download the mentalist report/i }));

  await waitFor(() => expect(screen.getByText("Report not prepared yet.")).toBeInTheDocument());
  expect(mentalistReportServices.generateReport).not.toHaveBeenCalled();
  expect(mentalistReportServices.downloadReport).toHaveBeenCalledWith("41");
});

test("super admin sees the AI report generation control", () => {
  renderPage("SUPER_ADMIN");

  expect(screen.getByRole("button", { name: /generate ai report/i })).toBeInTheDocument();
});
