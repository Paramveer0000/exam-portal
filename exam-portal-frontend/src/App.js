import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import "./App.css";
import Header from "./components/Header";
import ProtectedRoute from "./components/ProtectedRoute";
import RequireStudentOnboarding from "./components/RequireStudentOnboarding";
import AdminAddCategoryPage from "./pages/admin/categories/AdminAddCategoryPage";
import AdminCategoriesPage from "./pages/admin/categories/AdminCategoriesPage";
import AdminUpdateCategoryPage from "./pages/admin/categories/AdminUpdateCategoryPage";
import AdminProfilePage from "./pages/admin/AdminProfilePage";
import AdminDashboardPage from "./pages/admin/AdminDashboardPage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import AdminQuizzesPage from "./pages/admin/quizzes/AdminQuizzesPage";
import AdminAddQuiz from "./pages/admin/quizzes/AdminAddQuiz";
import AdminUpdateQuiz from "./pages/admin/quizzes/AdminUpdateQuiz";
import AdminQuestionsPage from "./pages/admin/questions/AdminQuestionsPage";
import AdminAddQuestionsPage from "./pages/admin/questions/AdminAddQuestionsPage";
import AdminUpdateQuestionPage from "./pages/admin/questions/AdminUpdateQuestionPage";
import UserProfilePage from "./pages/users/UserProfilePage";
import UserQuizzesPage from "./pages/users/UserQuizzesPage";
import UserSubjectsPage from "./pages/users/UserSubjectsPage";
import UserQuizManualPage from "./pages/users/UserQuizManualPage";
import UserQuestionsPage from "./pages/users/UserQuestionsPage";
import UserQuizResultPage from "./pages/users/UserQuizResultPage";
import PsychometricReportPage from "./pages/users/PsychometricReportPage";
import OnboardingPage from "./pages/users/OnboardingPage";
import AdminQuizResultPage from "./pages/admin/AdminQuizResultPage";
import AdminReportsPage from "./pages/admin/AdminReportsPage";
import AdminStudentsPage from "./pages/admin/AdminStudentsPage";
import SuperAdminDashboardPage from "./pages/superadmin/SuperAdminDashboardPage";
import SuperAdminAdminsPage from "./pages/superadmin/SuperAdminAdminsPage";
import SuperAdminProfilePage from "./pages/superadmin/SuperAdminProfilePage";
import SuperAdminAiSettingsPage from "./pages/superadmin/SuperAdminAiSettingsPage";
import SuperAdminResultsPage from "./pages/superadmin/SuperAdminResultsPage";

const ADMIN_ROLES = ["ADMIN", "SUPER_ADMIN"];
const USER_ROLES = ["USER"];
const SUPER_ADMIN_ROLES = ["SUPER_ADMIN"];

const adminRoute = (element) => (
  <ProtectedRoute allowedRoles={ADMIN_ROLES}>{element}</ProtectedRoute>
);
const userRoute = (element) => (
  <ProtectedRoute allowedRoles={USER_ROLES}>
    <RequireStudentOnboarding>{element}</RequireStudentOnboarding>
  </ProtectedRoute>
);
const superAdminRoute = (element) => (
  <ProtectedRoute allowedRoles={SUPER_ADMIN_ROLES}>{element}</ProtectedRoute>
);

const App = () => {
  return (
    <Router>
      <Header />
      <Routes>
        <Route path="/" element={<LoginPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {/* Super Admin */}
        <Route
          path="/superadmin"
          element={superAdminRoute(<SuperAdminDashboardPage />)}
        />
        <Route
          path="/superadmin/admins"
          element={superAdminRoute(<SuperAdminAdminsPage />)}
        />
        <Route
          path="/superadmin/profile"
          element={superAdminRoute(<SuperAdminProfilePage />)}
        />
        <Route
          path="/superadmin/results"
          element={superAdminRoute(<SuperAdminResultsPage />)}
        />
        <Route
          path="/superadmin/ai-settings"
          element={superAdminRoute(<SuperAdminAiSettingsPage />)}
        />

        {/* Admin (Super Admin inherits access) */}
        <Route
          path="/adminDashboard"
          element={adminRoute(<AdminDashboardPage />)}
        />
        <Route
          path="/adminProfile"
          element={adminRoute(<AdminProfilePage />)}
        />
        <Route
          path="/adminCategories"
          element={adminRoute(<AdminCategoriesPage />)}
        />
        <Route
          path="/adminAddCategory"
          element={adminRoute(<AdminAddCategoryPage />)}
        />
        <Route
          path="/adminUpdateCategory/:catId"
          element={adminRoute(<AdminUpdateCategoryPage />)}
        />
        <Route path="/adminQuizzes" element={adminRoute(<AdminQuizzesPage />)} />
        <Route path="/adminAddQuiz" element={adminRoute(<AdminAddQuiz />)} />
        <Route
          path="/adminUpdateQuiz/:quizId"
          element={adminRoute(<AdminUpdateQuiz />)}
        />
        <Route
          path="/adminQuestions"
          element={adminRoute(<AdminQuestionsPage />)}
        />
        <Route
          path="/adminAddQuestion"
          element={adminRoute(<AdminAddQuestionsPage />)}
        />
        <Route
          path="/adminallResult"
          element={adminRoute(<AdminQuizResultPage />)}
        />
        <Route
          path="/adminReports"
          element={adminRoute(<AdminReportsPage />)}
        />
        <Route
          path="/adminStudents"
          element={adminRoute(<AdminStudentsPage />)}
        />
        <Route
          path="/adminUpdateQuestion/:quesId"
          element={adminRoute(<AdminUpdateQuestionPage />)}
        />

        {/* Student */}
        <Route
          path="/onboarding"
          element={
            <ProtectedRoute allowedRoles={USER_ROLES}>
              <OnboardingPage />
            </ProtectedRoute>
          }
        />
        <Route path="/profile" element={userRoute(<UserProfilePage />)} />
        <Route path="/subjects" element={userRoute(<UserSubjectsPage />)} />
        <Route path="/quizzes" element={userRoute(<UserQuizzesPage />)} />
        <Route path="/quiz/*" element={userRoute(<UserQuizzesPage />)} />
        <Route path="/quizManual/" element={userRoute(<UserQuizManualPage />)} />
        <Route path="/questions/" element={userRoute(<UserQuestionsPage />)} />
        {/* Report is visible to the student AND their teacher/super admin
            (server enforces per-row ownership). */}
        <Route
          path="/psychometricReport/:quizResId"
          element={
            <ProtectedRoute allowedRoles={["USER", "ADMIN", "SUPER_ADMIN"]}>
              <PsychometricReportPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/quizResults/"
          element={userRoute(<UserQuizResultPage />)}
        />
      </Routes>
    </Router>
  );
};

export default App;
