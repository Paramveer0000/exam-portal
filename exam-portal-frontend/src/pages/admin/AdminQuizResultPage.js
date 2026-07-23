import React, { useEffect, useState } from "react";
import SidebarUser from "../../components/SidebarUser";
import "../users/UserQuizResultPage.css";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { fetchQuizResult } from "../../actions/quizResultActions";
import * as quizResultConstants from "../../constants/quizResultConstants";
import Message from "../../components/Message";
import { Link } from "react-router-dom";
import { Table } from "react-bootstrap";
import Sidebar from "../../components/Sidebar";

// Groups quiz results by their class (category) so results render class-wise.
const groupResultsByClass = (results) => {
  const groups = {};
  const order = [];
  results.forEach((r) => {
    const cname =
      r.quiz && r.quiz.subject ? r.quiz.subject.title : "Uncategorized";
    if (!groups[cname]) {
      groups[cname] = [];
      order.push(cname);
    }
    groups[cname].push(r);
  });
  return order.map((cname) => ({ className: cname, results: groups[cname] }));
};

const AdminQuizResultPage = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const quizResultReducer = useSelector((state) => state.quizResultReducer);
  const [quizResults, setQuizResults] = useState(null);
  const token = JSON.parse(localStorage.getItem("jwtToken"));
  const user = JSON.parse(localStorage.getItem("user"));
  const userId = user ? user.userId : null;

  useEffect(() => {
    if (quizResults == null)
      fetchQuizResult(dispatch, null, token).then((data) => {
        if (data.type === quizResultConstants.FETCH_QUIZ_RESULT_SUCCESS) {
          setQuizResults(data.payload);
        }
      });
  }, []);

  useEffect(() => {
    if (!localStorage.getItem("jwtToken")) navigate("/");
  }, []);

  return (
    <div className="userQuizResultPage__container">
      <div className="userQuizResultPage__sidebar">
        <Sidebar />
      </div>

      <div className="userQuizResultPage__content">
        {quizResults && quizResults.length !== 0 ? (
          groupResultsByClass(quizResults).map((group) => (
            <div key={group.className} style={{ marginBottom: "28px" }}>
              <h4 style={{ marginBottom: "12px" }}>{group.className}</h4>
              <Table bordered className="userQuizResultPage__content--table">
                <thead>
                  <tr>
                    <th>Quiz Id</th>
                    <th>Subject</th>
                    <th>Obtained Marks</th>
                    <th>Total Marks</th>
                    <th>Result</th>
                    <th>Date</th>
                  </tr>
                </thead>
                <tbody>
                  {group.results.map((r, index) => (
                    <tr key={index}>
                      <td>{r.quiz.quizId}</td>
                      <td>{r.quiz.title}</td>
                      <td>{r.totalObtainedMarks}</td>
                      <td>{r.totalMarks}</td>
                      <td>{r.passed ? "Pass" : "Fail"}</td>
                      <td>{r.attemptDatetime}</td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </div>
          ))
        ) : (
          <Message>No results to display.</Message>
        )}
      </div>
    </div>
  );
};

export default AdminQuizResultPage;
