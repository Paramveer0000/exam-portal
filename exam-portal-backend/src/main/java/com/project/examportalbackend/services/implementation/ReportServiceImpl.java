package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.ReportRowDto;
import com.project.examportalbackend.models.Quiz;
import com.project.examportalbackend.models.QuizResult;
import com.project.examportalbackend.models.User;
import com.project.examportalbackend.repository.QuizRepository;
import com.project.examportalbackend.repository.QuizResultRepository;
import com.project.examportalbackend.repository.UserRepository;
import com.project.examportalbackend.security.AuthFacade;
import com.project.examportalbackend.services.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private QuizResultRepository quizResultRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthFacade authFacade;

    @Override
    public List<ReportRowDto> quizReport(Long quizId, Boolean passed, String from, String to) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
        // Only the owning admin (or a super admin) may view this quiz's report.
        authFacade.assertCanManage(quiz.getCreatedBy());

        List<QuizResult> results = quizResultRepository.findByQuiz_QuizId(quizId);
        return toRows(results, passed, from, to);
    }

    @Override
    public List<ReportRowDto> studentReport(Long studentUserId, Boolean passed, String from, String to) {
        // A teacher may only view reports for students that belong to them.
        if (!authFacade.isSuperAdmin()) {
            User student = userRepository.findById(studentUserId).orElse(null);
            Long me = authFacade.getCurrentUserId();
            if (student == null || !me.equals(student.getTeacherId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "This student is not in your class");
            }
        }
        List<QuizResult> results = quizResultRepository.findByUserId(studentUserId);
        return toRows(results, passed, from, to);
    }

    @Override
    public String toCsv(List<ReportRowDto> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("resultId,userId,studentName,quizId,quizTitle,obtainedMarks,totalMarks,passed,attempt,attemptDatetime\n");
        for (ReportRowDto r : rows) {
            sb.append(r.getResultId()).append(',')
                    .append(r.getUserId()).append(',')
                    .append(csv(r.getStudentName())).append(',')
                    .append(r.getQuizId()).append(',')
                    .append(csv(r.getQuizTitle())).append(',')
                    .append(r.getTotalObtainedMarks()).append(',')
                    .append(r.getTotalMarks()).append(',')
                    .append(r.isPassed()).append(',')
                    .append(r.getAttemptNumber()).append(',')
                    .append(csv(r.getAttemptDatetime())).append('\n');
        }
        return sb.toString();
    }

    private List<ReportRowDto> toRows(List<QuizResult> results, Boolean passed, String from, String to) {
        Map<Long, User> studentCache = new HashMap<>();
        return results.stream()
                .filter(r -> passed == null || r.isPassed() == passed)
                .filter(r -> withinRange(r.getAttemptDatetime(), from, to))
                .map(r -> {
                    User student = studentCache.computeIfAbsent(r.getUserId(),
                            id -> userRepository.findById(id).orElse(null));
                    return ReportRowDto.from(r, student);
                })
                .collect(Collectors.toList());
    }

    /**
     * attemptDatetime is stored as "yyyy-MM-dd HH:mm:ss"; from/to are "yyyy-MM-dd".
     * ISO date ordering matches lexical ordering, so a prefix compare is sufficient.
     */
    private boolean withinRange(String attemptDatetime, String from, String to) {
        if (attemptDatetime == null || attemptDatetime.length() < 10) {
            return !StringUtils.hasText(from) && !StringUtils.hasText(to);
        }
        String date = attemptDatetime.substring(0, 10);
        if (StringUtils.hasText(from) && date.compareTo(from) < 0) {
            return false;
        }
        return !StringUtils.hasText(to) || date.compareTo(to) <= 0;
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
