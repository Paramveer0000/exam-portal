package com.project.examportalbackend.services.implementation;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import com.project.examportalbackend.dto.MentalistReportDto;
import com.project.examportalbackend.services.PdfReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;

@Service
public class PdfReportServiceImpl implements PdfReportService {

    @Autowired private TemplateEngine templateEngine;

    @Override
    public byte[] render(MentalistReportDto dto) {
        // Strip a leading UTF-8 BOM if present - it breaks strict XHTML parsing below.
        String html = templateEngine.process("report/report", buildContext(dto));
        if (!html.isEmpty() && html.charAt(0) == '﻿') {
            html = html.substring(1);
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, "about:blank");
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render report PDF", e);
        }
    }

    private Context buildContext(MentalistReportDto dto) {
        Context ctx = new Context();
        ctx.setVariable("reportNumber", dto.getReportNumber());
        ctx.setVariable("assessmentDate", dto.getAssessmentDate());
        ctx.setVariable("profile", dto.getProfile());
        ctx.setVariable("personalityType", dto.getPersonalityType());
        ctx.setVariable("personality", dto.getPersonality());
        ctx.setVariable("emotionalIntelligence", dto.getEmotionalIntelligence());
        ctx.setVariable("learningStyle", dto.getLearningStyle());
        ctx.setVariable("multipleIntelligence", dto.getMultipleIntelligence());
        ctx.setVariable("communicationLeadership", dto.getCommunicationLeadership());
        ctx.setVariable("careers", dto.getCareers());
        ctx.setVariable("careerInterest", dto.getCareerInterest());
        ctx.setVariable("behaviourAnalysis", dto.getBehaviourAnalysis());
        ctx.setVariable("mentalWellness", dto.getMentalWellness());
        ctx.setVariable("swot", dto.getSwot());
        ctx.setVariable("developmentPlan", dto.getDevelopmentPlan());
        ctx.setVariable("careerGuidance", dto.getCareerGuidance());
        ctx.setVariable("counsellorSummary", dto.getCounsellorSummary());
        // Phase D presentation layer
        ctx.setVariable("companyLogo", dto.getCompanyLogo());
        ctx.setVariable("atAGlance", dto.getAtAGlance());
        ctx.setVariable("bandScale", dto.getBandScale());
        ctx.setVariable("dimensionGroups", dto.getDimensionGroups());
        ctx.setVariable("classGuidance", dto.getClassGuidance());
        ctx.setVariable("parentGuide", dto.getParentGuide());
        ctx.setVariable("nextSteps", dto.getNextSteps());
        // Content Engine V2
        ctx.setVariable("synthesis", dto.getSynthesis());
        ctx.setVariable("careerClusters", dto.getCareerClusters());
        ctx.setVariable("streamOptions", dto.getStreamOptions());
        ctx.setVariable("parentGuideContent", dto.getParentGuideContent());
        ctx.setVariable("teacherGuideContent", dto.getTeacherGuideContent());
        ctx.setVariable("howToReadContent", dto.getHowToReadContent());
        return ctx;
    }
}
