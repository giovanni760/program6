package edu.psp.program6;

import java.util.function.DoubleUnaryOperator;

/**
 * Funcion de densidad de la distribucion t:
 * F(x) = Gamma((dof+1)/2) / ((dof*pi)^(1/2) * Gamma(dof/2)) * (1 + x^2/dof)^(-(dof+1)/2)
 */
public final class TDistribution implements DoubleUnaryOperator {

    private final int dof;
    private final double constant;

    /**
     * @param dof grados de libertad (entre 1 y 342; para valores mayores Gamma excede el rango de double)
     */
    public TDistribution(int dof) {
        if (dof <= 0) {
            throw new IllegalArgumentException("dof debe ser mayor que 0: " + dof);
        }
        this.dof = dof;
        // Se dividen primero las gammas para no desbordar el denominador con dof grandes
        this.constant = Gamma.of((dof + 1) / 2.0) / Gamma.of(dof / 2.0)
                / Math.sqrt(dof * Math.PI);
    }

    public int getDof() {
        return dof;
    }

    /** Parte constante de la funcion (0.388035 para dof = 9). */
    public double getConstant() {
        return constant;
    }

    @Override
    public double applyAsDouble(double x) {
        return constant * Math.pow(1.0 + (x * x) / dof, -(dof + 1) / 2.0);
    }
}
