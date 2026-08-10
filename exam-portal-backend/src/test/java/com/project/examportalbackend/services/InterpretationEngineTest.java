package com.project.examportalbackend.services;

import com.project.examportalbackend.services.InterpretationEngine.Band;
import com.project.examportalbackend.services.InterpretationEngine.BandRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pins the published interpretation bands and their exact boundaries. */
class InterpretationEngineTest {

    private final InterpretationEngine engine = new InterpretationEngine();

    @Test
    void bandBoundariesAreExact() {
        assertSame(Band.CRITICAL, engine.bandFor(0));
        assertSame(Band.CRITICAL, engine.bandFor(39.99));
        assertSame(Band.NEEDS_IMPROVEMENT, engine.bandFor(40));
        assertSame(Band.NEEDS_IMPROVEMENT, engine.bandFor(54.99));
        assertSame(Band.AVERAGE, engine.bandFor(55));
        assertSame(Band.AVERAGE, engine.bandFor(69.99));
        assertSame(Band.STRONG, engine.bandFor(70));
        assertSame(Band.STRONG, engine.bandFor(84.99));
        assertSame(Band.EXCELLENT, engine.bandFor(85));
        assertSame(Band.EXCELLENT, engine.bandFor(100));
    }

    @Test
    void labelsMatchTheReportsTerminology() {
        assertEquals("Critical", engine.bandLabel(Band.CRITICAL));
        assertEquals("Needs Improvement", engine.bandLabel(Band.NEEDS_IMPROVEMENT));
        assertEquals("Average", engine.bandLabel(Band.AVERAGE));
        assertEquals("Strong", engine.bandLabel(Band.STRONG));
        assertEquals("Excellent", engine.bandLabel(Band.EXCELLENT));
    }

    /**
     * The scale printed in the PDF must be the same one bandFor() applies --
     * a drifting second copy of these thresholds is the failure this guards.
     */
    @Test
    void publishedScaleAgreesWithBandFor() {
        List<BandRange> scale = engine.bandScale();
        assertEquals(5, scale.size());
        for (BandRange r : scale) {
            assertSame(r.getBand(), engine.bandFor(r.getFrom()),
                    "scale row " + r.getRange() + " disagrees with bandFor at its lower bound");
            assertSame(r.getBand(), engine.bandFor(r.getTo()),
                    "scale row " + r.getRange() + " disagrees with bandFor at its upper bound");
            assertEquals(engine.bandLabel(r.getBand()), r.getLabel());
        }
    }

    @Test
    void scaleCoversZeroToOneHundredWithoutGaps() {
        List<BandRange> scale = engine.bandScale();
        assertEquals(0, scale.get(0).getFrom());
        assertEquals(100, scale.get(scale.size() - 1).getTo());
        for (int i = 1; i < scale.size(); i++) {
            assertEquals(scale.get(i - 1).getTo() + 1, scale.get(i).getFrom(),
                    "gap or overlap between published bands");
        }
    }

    @Test
    void interpretationAlwaysSuppliesCopyForEveryBand() {
        for (double score : new double[]{10, 45, 60, 75, 95}) {
            InterpretationEngine.Interpretation i = engine.interpret("Communication", score);
            assertTrue(i.status != null && !i.status.isEmpty());
            assertTrue(!i.strengths.isEmpty() && !i.challenges.isEmpty() && !i.suggestions.isEmpty());
            assertEquals(engine.bandLabel(engine.bandFor(score)), i.bandLabel);
        }
    }
}
