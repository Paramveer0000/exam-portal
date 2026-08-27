import api from "./api";
import authServices from "./authServices";

jest.mock("./api", () => ({ post: jest.fn() }));

afterEach(() => {
  jest.clearAllMocks();
  localStorage.clear();
});

test("logout removes stale impersonation recovery state", async () => {
  localStorage.setItem("user", "{}");
  localStorage.setItem("impersonatorBackup", JSON.stringify({ userId: 7 }));
  api.post.mockResolvedValue({});

  await authServices.logout();

  expect(localStorage.getItem("user")).toBeNull();
  expect(localStorage.getItem("impersonatorBackup")).toBeNull();
});
