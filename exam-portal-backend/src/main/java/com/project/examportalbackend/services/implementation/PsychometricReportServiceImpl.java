package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.PsychometricReportDto;
import com.project.examportalbackend.dto.PsychometricReportDto.CareerRow;
import com.project.examportalbackend.dto.PsychometricReportDto.DomainRow;
import com.project.examportalbackend.dto.PsychometricReportDto.MiRow;
import com.project.examportalbackend.dto.PsychometricReportDto.QuotientRow;
import com.project.examportalbackend.dto.PsychometricReportDto.RiasecRow;
import com.project.examportalbackend.models.CareerSuggestion;
import com.project.examportalbackend.models.PsychometricReport;
import com.project.examportalbackend.models.Question;
import com.project.examportalbackend.models.QuizResult;
import com.project.examportalbackend.models.User;
import com.project.examportalbackend.repository.CareerSuggestionRepository;
import com.project.examportalbackend.repository.PsychometricReportRepository;
import com.project.examportalbackend.repository.QuestionRepository;
import com.project.examportalbackend.repository.QuizResultRepository;
import com.project.examportalbackend.repository.UserRepository;
import com.project.examportalbackend.security.AuthFacade;
import com.project.examportalbackend.services.PsychometricReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure-Java psychometric scoring. Each answered question contributes a Likert
 * point value equal to the ordinal of the chosen option (option1=1 .. option4=4);
 * submissions may carry either the option label or the option text (shuffled
 * exams), both are resolved.
 */
@Service
public class PsychometricReportServiceImpl implements PsychometricReportService {

    static final List<String> MI_DIMS = Arrays.asList(
            "LOGICAL", "MUSICAL", "NATURALIST", "VERBAL", "INTERPERSONAL",
            "KINESTHETIC", "SPATIAL", "INTRAPERSONAL", "EXISTENTIAL");
    static final List<String> RIASEC_LETTERS = Arrays.asList("R", "I", "A", "S", "E", "C");

    private static final Map<String, String> RIASEC_NAMES = new LinkedHashMap<>();
    static {
        RIASEC_NAMES.put("R", "Realistic");
        RIASEC_NAMES.put("I", "Investigative");
        RIASEC_NAMES.put("A", "Artistic");
        RIASEC_NAMES.put("S", "Social");
        RIASEC_NAMES.put("E", "Enterprising");
        RIASEC_NAMES.put("C", "Conventional");
    }

    @Autowired private PsychometricReportRepository reportRepository;
    @Autowired private CareerSuggestionRepository careerSuggestionRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private QuizResultRepository quizResultRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthFacade authFacade;

    // ------------------------------------------------------------------ score

    @Override
    public void scoreAndPersist(QuizResult quizResult, Map<String, String> answers) {
        Long quizId = quizResult.getQuiz().getQuizId();

        // Raw Likert sums per dimension, and per-dimension answer counts.
        Map<String, Double> raw = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();

        for (Map.Entry<String, String> entry : answers.entrySet()) {
            Question q = questionRepository.findById(Long.valueOf(entry.getKey())).orElse(null);
            if (q == null || q.getQuiz() == null || !quizId.equals(q.getQuiz().getQuizId())) {
                continue; // ignore answers that don't belong to this quiz
            }
            int ordinal = resolveOrdinal(q, entry.getValue());
            if (ordinal == 0) {
                continue; // unrecognisable submission
            }
            String dim = q.getDimension();
            raw.merge(dim, (double) ordinal, Double::sum);
            counts.merge(dim, 1, Integer::sum);
        }

        // MI percents: each dimension's share of the total MI points.
        double totalMi = MI_DIMS.stream().mapToDouble(d -> raw.getOrDefault(d, 0.0)).sum();
        Map<String, Double> miPercent = new HashMap<>();
        for (String d : MI_DIMS) {
            miPercent.put(d, totalMi == 0 ? 0 : round1(raw.getOrDefault(d, 0.0) / totalMi * 100));
        }

        // Self-check: shares must sum to ~100 when any MI question was answered.
        double sum = miPercent.values().stream().mapToDouble(Double::doubleValue).sum();
        assert totalMi == 0 || Math.abs(sum - 100.0) < 1.0
                : "MI percents sum to " + sum + ", expected ~100";

        // RIASEC: mean of the letter's Likert points mapped onto /10 (4 -> 10).
        Map<String, Double> riasec = new HashMap<>();
        for (String l : RIASEC_LETTERS) {
            int n = counts.getOrDefault(l, 0);
            riasec.put(l, n == 0 ? 0 : round1(raw.getOrDefault(l, 0.0) / n / 4.0 * 10));
        }

        PsychometricReport report = new PsychometricReport();
        report.setQuizResId(quizResult.getQuizResId());
        report.setMiLogical(miPercent.get("LOGICAL"));
        report.setMiMusical(miPercent.get("MUSICAL"));
        report.setMiNaturalist(miPercent.get("NATURALIST"));
        report.setMiVerbal(miPercent.get("VERBAL"));
        report.setMiInterpersonal(miPercent.get("INTERPERSONAL"));
        report.setMiKinesthetic(miPercent.get("KINESTHETIC"));
        report.setMiSpatial(miPercent.get("SPATIAL"));
        report.setMiIntrapersonal(miPercent.get("INTRAPERSONAL"));
        report.setMiExistential(miPercent.get("EXISTENTIAL"));
        report.setRiasecR(riasec.get("R"));
        report.setRiasecI(riasec.get("I"));
        report.setRiasecA(riasec.get("A"));
        report.setRiasecS(riasec.get("S"));
        report.setRiasecE(riasec.get("E"));
        report.setRiasecC(riasec.get("C"));

        applyQuotients(report, miPercent);
        reportRepository.save(report);
    }

    /**
     * Quotients are fixed sums of two MI shares, then normalized so the
     * strongest quotient reads 100%:
     *   IQ = LOGICAL + VERBAL          (reasoning + language)
     *   EQ = INTERPERSONAL + INTRAPERSONAL (others + self)
     *   AQ = NATURALIST + KINESTHETIC  (adversity: field + body)
     *   CQ = MUSICAL + SPATIAL         (creativity)
     *   SQ = EXISTENTIAL + INTRAPERSONAL (spiritual/meaning)
     */
    private void applyQuotients(PsychometricReport r, Map<String, Double> mi) {
        double iq = mi.get("LOGICAL") + mi.get("VERBAL");
        double eq = mi.get("INTERPERSONAL") + mi.get("INTRAPERSONAL");
        double aq = mi.get("NATURALIST") + mi.get("KINESTHETIC");
        double cq = mi.get("MUSICAL") + mi.get("SPATIAL");
        double sq = mi.get("EXISTENTIAL") + mi.get("INTRAPERSONAL");
        double max = Math.max(1, Math.max(iq, Math.max(eq, Math.max(aq, Math.max(cq, sq)))));
        r.setQuotIq(round1(iq / max * 100));
        r.setQuotEq(round1(eq / max * 100));
        r.setQuotAq(round1(aq / max * 100));
        r.setQuotCq(round1(cq / max * 100));
        r.setQuotSq(round1(sq / max * 100));
    }

    /** Chosen option's ordinal 1..4; accepts "optionN" labels or option text. 0 = unknown. */
    private int resolveOrdinal(Question q, String submitted) {
        if (submitted == null) return 0;
        switch (submitted) {
            case "option1": return 1;
            case "option2": return 2;
            case "option3": return 3;
            case "option4": return 4;
            default:
                if (submitted.equals(q.getOption1())) return 1;
                if (submitted.equals(q.getOption2())) return 2;
                if (submitted.equals(q.getOption3())) return 3;
                if (submitted.equals(q.getOption4())) return 4;
                return 0;
        }
    }

    // ------------------------------------------------------------------- read

    @Override
    public PsychometricReportDto getReport(Long quizResId) {
        QuizResult result = quizResultRepository.findById(quizResId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found"));
        User student = userRepository.findById(result.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        // Ownership: the student themself, their teacher, or a super admin.
        if (!authFacade.isSuperAdmin()) {
            Long me = authFacade.getCurrentUserId();
            boolean own = me.equals(result.getUserId());
            boolean myStudent = authFacade.hasRole(AuthFacade.ROLE_ADMIN)
                    && me.equals(student.getTeacherId());
            if (!own && !myStudent) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You may not view this report");
            }
        }

        PsychometricReport report = reportRepository.findByQuizResId(quizResId);
        if (report == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No psychometric report exists for this attempt");
        }

        Map<String, Double> mi = miMap(report);

        // MI rows with rank (1 = strongest).
        List<String> byScore = new ArrayList<>(MI_DIMS);
        byScore.sort(Comparator.comparingDouble((String d) -> mi.get(d)).reversed());
        List<MiRow> miRows = new ArrayList<>();
        for (String d : MI_DIMS) {
            miRows.add(MiRow.of(d, mi.get(d), byScore.indexOf(d) + 1));
        }

        // Mackenzie domains: fixed sums of three MI shares each.
        List<DomainRow> domains = Arrays.asList(
                DomainRow.of("Analytical", round1(mi.get("LOGICAL") + mi.get("MUSICAL") + mi.get("NATURALIST"))),
                DomainRow.of("Interactive", round1(mi.get("VERBAL") + mi.get("INTERPERSONAL") + mi.get("KINESTHETIC"))),
                DomainRow.of("Introspective", round1(mi.get("SPATIAL") + mi.get("INTRAPERSONAL") + mi.get("EXISTENTIAL"))));

        // RIASEC rows; dominant = top-3 letters, which also form the Holland code.
        Map<String, Double> ri = riasecMap(report);
        List<String> topLetters = new ArrayList<>(RIASEC_LETTERS);
        topLetters.sort(Comparator.comparingDouble((String l) -> ri.get(l)).reversed());
        String hollandCode = String.join("", topLetters.subList(0, 3));
        List<RiasecRow> riasecRows = new ArrayList<>();
        for (String l : RIASEC_LETTERS) {
            riasecRows.add(RiasecRow.of(l, RIASEC_NAMES.get(l), ri.get(l),
                    hollandCode.contains(l)));
        }

        List<QuotientRow> quotients = Arrays.asList(
                QuotientRow.of("IQ", "Intelligence Quotient", report.getQuotIq()),
                QuotientRow.of("EQ", "Emotional Quotient", report.getQuotEq()),
                QuotientRow.of("AQ", "Adversity Quotient", report.getQuotAq()),
                QuotientRow.of("CQ", "Creativity Quotient", report.getQuotCq()),
                QuotientRow.of("SQ", "Spiritual Quotient", report.getQuotSq()));

        return PsychometricReportDto.from(report, result, student, miRows, domains,
                riasecRows, hollandCode, quotients, rankCareers(mi, ri));
    }

    /**
     * Ranks the career_suggestions table by base_weight x the mean "prominence"
     * of the row's driving dimensions. MI shares and RIASEC scores live on
     * different scales (MI ~ a share around 100/9 %, RIASEC a mean on /10), so a
     * raw average lets whichever system a field happens to use dominate. We
     * instead express each dimension relative to its OWN system's average
     * (1.0 = average, >1 = above average), which puts MI- and RIASEC-driven
     * fields on comparable footing and lets a spike in either lift its careers.
     * Normalized so the best field = 100.
     */
    private List<CareerRow> rankCareers(Map<String, Double> mi, Map<String, Double> ri) {
        double miAvg = 100.0 / MI_DIMS.size(); // MI shares sum to ~100 over 9 dims
        double riMean = RIASEC_LETTERS.stream().mapToDouble(l -> ri.getOrDefault(l, 0.0)).average().orElse(0);
        double riDenom = riMean > 0 ? riMean : 1;

        List<CareerSuggestion> all = careerSuggestionRepository.findAll();
        Map<CareerSuggestion, Double> scores = new LinkedHashMap<>();
        for (CareerSuggestion cs : all) {
            String[] dims = cs.getDimensions().split(",");
            double total = 0;
            int n = 0;
            for (String dim : dims) {
                String d = dim.trim();
                if (mi.containsKey(d)) {
                    total += mi.get(d) / miAvg;   // prominence within MI
                    n++;
                } else if (ri.containsKey(d)) {
                    total += ri.get(d) / riDenom; // prominence within RIASEC
                    n++;
                }
            }
            scores.put(cs, n == 0 ? 0 : cs.getBaseWeight() * total / n);
        }
        double max = Math.max(1, scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1));
        return scores.entrySet().stream()
                .sorted(Map.Entry.<CareerSuggestion, Double>comparingByValue().reversed())
                .limit(6)
                .map(e -> {
                    double norm = round1(e.getValue() / max * 100);
                    int stars = Math.max(1, Math.min(5, (int) Math.ceil(norm / 20)));
                    return CareerRow.of(e.getKey().getField(), e.getKey().getLabel(), norm, stars);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private Map<String, Double> miMap(PsychometricReport r) {
        Map<String, Double> m = new HashMap<>();
        m.put("LOGICAL", r.getMiLogical());
        m.put("MUSICAL", r.getMiMusical());
        m.put("NATURALIST", r.getMiNaturalist());
        m.put("VERBAL", r.getMiVerbal());
        m.put("INTERPERSONAL", r.getMiInterpersonal());
        m.put("KINESTHETIC", r.getMiKinesthetic());
        m.put("SPATIAL", r.getMiSpatial());
        m.put("INTRAPERSONAL", r.getMiIntrapersonal());
        m.put("EXISTENTIAL", r.getMiExistential());
        return m;
    }

    private Map<String, Double> riasecMap(PsychometricReport r) {
        Map<String, Double> m = new HashMap<>();
        m.put("R", r.getRiasecR());
        m.put("I", r.getRiasecI());
        m.put("A", r.getRiasecA());
        m.put("S", r.getRiasecS());
        m.put("E", r.getRiasecE());
        m.put("C", r.getRiasecC());
        return m;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
