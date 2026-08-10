package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.dto.DimensionScoreView;
import com.project.examportalbackend.models.Dimension;
import com.project.examportalbackend.models.DimensionResult;
import com.project.examportalbackend.repository.DimensionRepository;
import com.project.examportalbackend.services.DimensionProfileService;
import com.project.examportalbackend.services.InterpretationEngine;
import com.project.examportalbackend.services.InterpretationEngine.Interpretation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DimensionProfileServiceImpl implements DimensionProfileService {

    @Autowired private DimensionRepository dimensionRepository;
    @Autowired private InterpretationEngine interpretationEngine;

    @Override
    public List<DimensionScoreView> buildScoreViews(List<DimensionResult> results, String dimensionType) {
        List<DimensionScoreView> views = new ArrayList<>();
        for (DimensionResult dr : results) {
            Dimension d = dimensionRepository.findById(dr.getDimensionCode()).orElse(null);
            if (d == null || !dimensionType.equals(d.getDimensionType())) {
                continue;
            }
            Interpretation interp = interpretationEngine.interpret(d.getDisplayName(), dr.getPercentage());

            DimensionScoreView view = new DimensionScoreView();
            view.setDimensionCode(d.getDimensionCode());
            view.setDimensionName(d.getDisplayName());
            view.setDimensionType(d.getDimensionType());
            view.setDescription(d.getDescription());
            view.setRawScore(dr.getRawScore());
            view.setMaxScore(dr.getMaxScore());
            view.setPercentage(dr.getPercentage());
            view.setInterpretationBand(interp.band.name());
            view.setInterpretationLabel(interp.bandLabel);
            view.setInterpretationDescription(interp.status);
            view.setWatchFor(interp.challenges.isEmpty() ? null : interp.challenges.get(0));
            view.setDevelopmentTip(interp.suggestions.isEmpty() ? null : interp.suggestions.get(0));
            views.add(view);
        }
        views.sort(Comparator.comparingDouble(DimensionScoreView::getPercentage).reversed());
        for (int i = 0; i < views.size(); i++) {
            views.get(i).setRank(i + 1);
        }
        return views;
    }
}
