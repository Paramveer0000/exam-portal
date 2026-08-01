import React, { useEffect, useState } from "react";
import "./AdminQuestionsPage.css";
import { useDispatch, useSelector } from "react-redux";
import { useLocation, useNavigate } from "react-router-dom";
import { Button, Card } from "react-bootstrap";
import { BsSearch } from "react-icons/bs";
import swal from "sweetalert";
import { Form } from "react-bootstrap";
import { deleteQuestion, fetchQuestionsByQuiz } from "../../../actions/questionsActions";
import * as questionsConstants from "../../../constants/questionsConstants";
import Sidebar from "../../../components/Sidebar";
import Question from "../../../components/Question";
import Loader from "../../../components/Loader";

const AdminQuestionsPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch();
  const urlParams = new URLSearchParams(window.location.search);
  const quizId = urlParams.get("quizId");
  const quizTitle = urlParams.get("quizTitle");

  const questionsReducer = useSelector((state) => state.questionsReducer);
  const [questions, setQuestions] = useState(questionsReducer.questions);
  const [search, setSearch] = useState("");
  const [selected, setSelected] = useState(new Set());
  const [deleting, setDeleting] = useState(false);
  const storedUser = JSON.parse(localStorage.getItem("user") || "null");
  // Question CRUD is SUPER_ADMIN-only on the backend (see SecurityConfig); a plain ADMIN
  // can view but every write 403s. Hide the write controls instead of letting them silently fail.
  const canEdit = !!storedUser?.roles?.some((r) => r.roleName === "SUPER_ADMIN");
  let answers = {};

  const filtered = questions
    ? questions.filter((q) =>
        q.content.toLowerCase().includes(search.trim().toLowerCase())
      )
    : questions;

  const toggleSelected = (quesId) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(quesId)) next.delete(quesId);
      else next.add(quesId);
      return next;
    });
  };

  const toggleSelectAll = () => {
    if (!filtered) return;
    setSelected((prev) =>
      prev.size === filtered.length ? new Set() : new Set(filtered.map((q) => q.quesId))
    );
  };

  const deleteSelected = () => {
    const count = selected.size;
    swal({
      title: "Are you sure?",
      text: `Delete ${count} selected question${count > 1 ? "s" : ""}? This cannot be undone.`,
      icon: "warning",
      buttons: true,
      dangerMode: true,
    }).then(async (willDelete) => {
      if (!willDelete) return;
      setDeleting(true);
      let failures = 0;
      for (const quesId of selected) {
        const result = await deleteQuestion(dispatch, quesId);
        if (result.type !== questionsConstants.DELETE_QUESTION_SUCCESS) failures++;
      }
      setDeleting(false);
      setSelected(new Set());
      fetchQuestionsByQuiz(dispatch, quizId).then((data) =>
        setQuestions(data.payload)
      );
      if (failures > 0) {
        swal(
          "Some deletes failed",
          `${failures} of ${count} question(s) could not be deleted (not authorized or already removed).`,
          "error"
        );
      }
    });
  };

  const addNewQuestionHandler = () => {
    navigate(`/adminAddQuestion/?quizId=${quizId}&quizTitle=${quizTitle}`);
  };

  useEffect(() => {
    if (!localStorage.getItem("user")) navigate("/");
  }, []);

  // Re-fetch every time this page becomes active (including navigating back
  // from Add/Update Question), not just on first mount, so edits always show.
  useEffect(() => {
    fetchQuestionsByQuiz(dispatch, quizId).then((data) =>
      setQuestions(data.payload)
    );
  }, [location.key]);

  return (
    <div className="adminQuestionsPage__container">
      <div className="adminQuestionsPage__sidebar">
        <Sidebar />
      </div>
      <div className="mt-page">
        <Button
          variant="secondary"
          className="mb-3"
          onClick={() => navigate("/adminQuizzes")}
        >
          ← Back to Quizzes
        </Button>
        <h2 style={{ color: "var(--mt-primary)" }}>{`Questions : ${quizTitle}`}</h2>
        {!canEdit && (
          <p className="text-muted">
            Viewing only — question content is managed by the platform admin.
          </p>
        )}
        {canEdit && (
          <Button
            className="mb-3"
            variant="primary"
            onClick={addNewQuestionHandler}
          >
            Add Question
          </Button>
        )}
        {questions ? (
          <>
            <div className="mt-search mb-2">
              <BsSearch />
              <Form.Control
                type="search"
                placeholder="Search questions..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
            {canEdit && (
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "12px",
                  margin: "8px 0",
                }}
              >
                <Form.Check
                  type="checkbox"
                  label={`Select all (${filtered.length})`}
                  checked={filtered.length > 0 && selected.size === filtered.length}
                  onChange={toggleSelectAll}
                />
                {selected.size > 0 && (
                  <button
                    className="btn btn-danger btn-sm"
                    disabled={deleting}
                    onClick={deleteSelected}
                  >
                    {deleting ? "Deleting..." : `Delete selected (${selected.size})`}
                  </button>
                )}
              </div>
            )}
            <Card className="mt-card p-3">
            {filtered.map((q, index) => (
              <div key={q.quesId} style={{ display: "flex", alignItems: "flex-start", gap: "8px" }}>
                {canEdit && (
                  <Form.Check
                    type="checkbox"
                    className="mt-2"
                    checked={selected.has(q.quesId)}
                    onChange={() => toggleSelected(q.quesId)}
                  />
                )}
                <div style={{ flexGrow: 1 }}>
                  <Question
                    number={index + 1}
                    answers={answers}
                    question={q}
                    isAdmin={true}
                    canEdit={canEdit}
                  />
                </div>
              </div>
            ))}
            </Card>
          </>
        ) : (
          <Loader />
        )}
      </div>
    </div>
  );
};

export default AdminQuestionsPage;
