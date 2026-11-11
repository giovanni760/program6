package edu.psp.program6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TDistributionTest {

    @Test
    void constantForNineDofMatchesKit() {
        TDistribution t = new TDistribution(9);
        assertEquals(9, t.getDof());
        assertEquals(0.388035, t.getConstant(), 1e-6);
    }

    @Test
    void valuesMatchTable2() {
        TDistribution t = new TDistribution(9);
        assertEquals(0.38803, t.applyAsDouble(0.0), 1e-5);
        assertEquals(0.38544, t.applyAsDouble(0.11), 1e-5);
        assertEquals(0.30636, t.applyAsDouble(0.66), 1e-5);
        assertEquals(0.20652, t.applyAsDouble(1.1), 1e-5);
    }

    @Test
    void isSymmetric() {
        TDistribution t = new TDistribution(5);
        assertEquals(t.applyAsDouble(1.3), t.applyAsDouble(-1.3), 1e-12);
    }

    @Test
    void rejectsInvalidDof() {
        assertThrows(IllegalArgumentException.class, () -> new TDistribution(0));
        assertThrows(IllegalArgumentException.class, () -> new TDistribution(-3));
    }

    @Test
    void largeDofApproachesStandardNormal() {
        // Con dof = 342 el denominador se desbordaba si no se dividian primero las gammas
        assertEquals(1.0 / Math.sqrt(2 * Math.PI), new TDistribution(342).getConstant(), 1e-3);
    }

    @Test
    void rejectsDofBeyondGammaRange() {
        assertThrows(IllegalArgumentException.class, () -> new TDistribution(343));
    }
}
