package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.MentalistReportDto;
import com.project.examportalbackend.models.MentalistReport;
import com.project.examportalbackend.repository.MentalistReportRepository;
import com.project.examportalbackend.services.MentalistReportService;
import com.project.examportalbackend.services.PdfReportService;
import com.project.examportalbackend.services.PsychometricReportService;
import com.project.examportalbackend.services.ReportDataAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

@Service
public class MentalistReportServiceImpl implements MentalistReportService {

    @Autowired private ReportDataAssembler reportDataAssembler;
    @Autowired private PdfReportService pdfReportService;
    @Autowired private MentalistReportRepository mentalistReportRepository;
    @Autowired private PsychometricReportService psychometricReportService;

    @Value("${mentalist.reports.dir}")
    private String reportsDir;

    @Override
    public MentalistReportDto preview(Long quizResId) {
        return reportDataAssembler.assemble(quizResId);
    }

    @Override
    public MentalistReportDto generate(Long quizResId, String counsellorName, String counsellorRemarks, boolean regenerate) {
        MentalistReport row = mentalistReportRepository.findByQuizResId(quizResId);
        boolean isNew = row == null;
        if (isNew) {
            row = new MentalistReport();
            row.setQuizResId(quizResId);
            row.setReportNumber("TM-" + quizResId + "-" + LocalDate.now().toString().replace("-", ""));
            row.setPdfPath("pending"); // overwritten below; column is NOT NULL
        }
        if (StringUtils.hasText(counsellorName)) row.setCounsellorName(counsellorName);
        if (StringUtils.hasText(counsellorRemarks)) row.setCounsellorRemarks(counsellorRemarks);

        if (!isNew && !regenerate) {
            // Already generated and no regeneration requested: leave the stored PDF
            // untouched, just return fresh preview data (ownership enforced inside).
            return reportDataAssembler.assemble(quizResId);
        }

        mentalistReportRepository.save(row); // assigns reportId (and generatedAt on insert) before assembling

        MentalistReportDto dto = reportDataAssembler.assemble(quizResId);
        byte[] pdf = pdfReportService.render(dto);
        row.setPdfPath(writeToDisk(quizResId, row.getReportId(), pdf));
        mentalistReportRepository.save(row);

        dto.setReportId(row.getReportId());
        return dto;
    }

    @Override
    public byte[] downloadPdf(Long quizResId) {
        // Ownership scoping, same rule as the report data itself.
        psychometricReportService.getReport(quizResId);

        MentalistReport row = mentalistReportRepository.findByQuizResId(quizResId);
        if (row == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report has not been generated yet");
        }
        try {
            return Files.readAllBytes(Paths.get(row.getPdfPath()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored report file is missing");
        }
    }

    private String writeToDisk(Long quizResId, Long reportId, byte[] pdf) {
        try {
            Path dir = Paths.get(reportsDir);
            Files.createDirectories(dir);
            Path file = dir.resolve("report-" + quizResId + "-" + reportId + ".pdf");
            Files.write(file, pdf);
            return file.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store generated report");
        }
    }
}
