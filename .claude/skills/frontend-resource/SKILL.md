---
name: frontend-resource
description: Wire a new Redux-thunk resource slice (or add an action to an existing one) in the Exam Portal React frontend. Use when the frontend needs to call a new backend endpoint and hold the result in Redux. Mirrors the constants→service→action→reducer→store pattern.
---

# frontend-resource

Every backend resource maps to a 4-file slice, wired by domain name. `categories` is the
cleanest reference (`*Constants.js`, `categoriesServices.js`, `categoriesActions.js`,
`categoriesReducer.js`). No Redux Toolkit, no shared axios instance, no TypeScript.

## The 4 files (+ store)

1. **`constants/<x>Constants.js`** — three string consts per operation:
   ```js
   export const FETCH_XS_REQUEST = "FETCH_XS_REQUEST";
   export const FETCH_XS_SUCCESS = "FETCH_XS_SUCCESS";
   export const FETCH_XS_FAILURE = "FETCH_XS_FAILURE";
   ```

2. **`services/<x>Services.js`** — raw axios, relative `/api/...` path, hand-built auth header.
   The token is passed **in** (callers read it from localStorage). Return a plain shape:
   ```js
   import axios from "axios";
   const fetchXs = async (token) => {
     try {
       const config = { headers: { Authorization: `Bearer ${token}` } };
       const { data } = await axios.get("/api/x/", config);
       return data;
     } catch (error) {
       // NOTE: existing code often does error.response.statusText unguarded — that throws
       // when the backend is down (error.response undefined). Prefer the guarded shape:
       const message =
         (error.response && error.response.data && error.response.data.message) ||
         (error.response && error.response.statusText) || "Request failed";
       return message;
     }
   };
   export default { fetchXs };
   ```
   Trailing slash on the path matters — the backend maps `/api/category/`, `/api/quiz/`, etc.

3. **`actions/<x>Actions.js`** — thunk with signature `(dispatch, ...args, token)`. Dispatches
   REQUEST, then SUCCESS or FAILURE off the service result:
   ```js
   export const fetchXs = async (dispatch, token) => {
     dispatch({ type: c.FETCH_XS_REQUEST });
     const data = await xServices.fetchXs(token);
     return dispatch({ type: data ? c.FETCH_XS_SUCCESS : c.FETCH_XS_FAILURE, payload: data });
   };
   ```
   These are **plain async functions**, not `redux-thunk` `dispatch =>` returns — the component
   calls `action(dispatch, args, token)` directly.

4. **`reducers/<x>Reducer.js`** — switch on the constants, classic `{ loading, data, error }` shape.

5. **`store.js`** — register the reducer under its domain key in `combineReducers`.

## In the component
```js
const token = JSON.parse(localStorage.getItem("jwtToken"));
useEffect(() => { fetchXs(dispatch, token); }, []);
```
Show `<Loader/>` while the slice is null/loading, `<Message variant="danger">` for errors.
Role-gate the route with `adminRoute`/`userRoute`/`superAdminRoute` in `App.js` (UI convenience
only — the backend is the real gate).

## Don't
- Add axios `baseURL`/interceptors or an env `REACT_APP_API_URL` unless doing a deliberate,
  whole-app refactor — the app relies on relative `/api` + the dev proxy (`setupProxy.js`).
- Read the token from Redux for API calls; it's always pulled fresh from `localStorage`.

## Log
After every frontend change or live test, **append an entry to `.claude/testing-log.md`** (newest at
bottom, never rewrite past entries). Use the format at the top of that file:
`what` / `files` / `result` (did CRA compile clean? did the UI action work end-to-end?) / `notes`.
The frontend hot-reloads on save — check the dev-server output for `Compiled successfully` (warnings
like the pre-existing `useEffect` dep are fine; `Failed to compile` is not). Read the log before
starting to inherit current runtime facts. Never write secrets — reference the env var name.
