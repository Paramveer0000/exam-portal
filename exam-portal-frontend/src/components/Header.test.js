import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { useDispatch, useSelector } from "react-redux";
import { MemoryRouter, useLocation } from "react-router-dom";
import swal from "sweetalert";
import adminServices from "../services/adminServices";
import platformServices from "../services/platformServices";
import { logout } from "../actions/authActions";
import Header from "./Header";

jest.mock("react-redux", () => ({ useDispatch: jest.fn(), useSelector: jest.fn() }));
jest.mock("../services/platformServices", () => ({
  getBranding: jest.fn(),
}));
jest.mock("../services/adminServices", () => ({ stopImpersonation: jest.fn() }));
jest.mock("../actions/authActions", () => ({ logout: jest.fn() }));
jest.mock("../hooks/useTheme", () => ({
  __esModule: true,
  default: () => "dark",
  logoForTheme: () => "/logo.png",
}));
jest.mock("sweetalert", () => jest.fn());

beforeEach(() => {
  platformServices.getBranding.mockResolvedValue({ companyLogo: null });
  useDispatch.mockReturnValue(jest.fn());
  useSelector.mockReturnValue({
    loggedIn: true,
    user: { firstName: "School" },
  });
  localStorage.setItem("user", JSON.stringify({ roles: [{ roleName: "ADMIN" }] }));
  localStorage.setItem("impersonatorBackup", JSON.stringify({ userId: 7 }));
});

afterEach(() => {
  jest.clearAllMocks();
  localStorage.clear();
});

const CurrentPath = () => <output data-testid="current-path">{useLocation().pathname}</output>;

test("logout waits to navigate until the session has been cleared", async () => {
  let finishLogout;
  const logoutPromise = new Promise((resolve) => {
    finishLogout = resolve;
  });
  logout.mockReturnValue(logoutPromise);

  render(
    <MemoryRouter initialEntries={["/psychometricReport/42"]}>
      <Header />
      <CurrentPath />
    </MemoryRouter>
  );

  fireEvent.click(screen.getByText("Logout"));

  expect(screen.getByTestId("current-path")).toHaveTextContent("/psychometricReport/42");

  finishLogout();

  await waitFor(() =>
    expect(screen.getByTestId("current-path")).toHaveTextContent("/login")
  );
});

test("failed return keeps recovery state and explains the failure", async () => {
  adminServices.stopImpersonation.mockResolvedValue({
    data: null,
    error: "Could not restore the super-admin session",
  });
  render(
    <MemoryRouter initialEntries={["/adminProfile"]}>
      <Header />
    </MemoryRouter>
  );

  fireEvent.click(screen.getByText(/return to super admin/i));

  await waitFor(() => expect(swal).toHaveBeenCalledWith(
    "Could not return",
    "Could not restore the super-admin session",
    "error"
  ));
  expect(localStorage.getItem("impersonatorBackup")).not.toBeNull();
});
