import { render, screen } from "@testing-library/react";
import { Provider } from "react-redux";
import store from "./store";
import App from "./App";

jest.mock("./pages/LandingPage", () => () => <main>The Mentalist home</main>);

test("renders the public landing route", () => {
  render(
    <Provider store={store}>
      <App />
    </Provider>
  );

  expect(screen.getByText("The Mentalist home")).toBeInTheDocument();
});
