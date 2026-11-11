package edu.psp.program6;

import java.util.function.DoubleUnaryOperator;

/**
 * Integracion numerica con la regla de Simpson. Duplica num_seg hasta que la
 * diferencia entre dos resultados consecutivos sea menor que E.
 *
 * <p>El Programa 6 integra de 0 a x ({@link #integrate(DoubleUnaryOperator, double)}),
 * pero tambien se puede integrar entre dos limites cualesquiera y obtener el
 * detalle de la ultima iteracion con {@link #integrateWithDetails}.
 */
public final class SimpsonIntegrator {

    public static final int DEFAULT_NUM_SEG = 10;
    public static final double DEFAULT_ERROR = 0.00001;
    private static final int MAX_NUM_SEG = 1 << 24;

    /**
     * Resultado de una integracion.
     *
     * @param value      valor aproximado de la integral
     * @param numSeg     num_seg usado en la ultima pasada
     * @param iterations numero de veces que se duplico num_seg
     * @param difference diferencia entre las dos ultimas pasadas
     * @param converged  true si la diferencia quedo por debajo de E
     */
    public record Result(double value, int numSeg, int iterations, double difference, boolean converged) {
    }

    private final int initialNumSeg;
    private final double error;

    public SimpsonIntegrator() {
        this(DEFAULT_NUM_SEG, DEFAULT_ERROR);
    }

    /**
     * @param initialNumSeg numero inicial de segmentos (par, mayor que 0)
     * @param error         error aceptable E (mayor que 0)
     */
    public SimpsonIntegrator(int initialNumSeg, double error) {
        validateNumSeg(initialNumSeg);
        if (Double.isNaN(error) || error <= 0) {
            throw new IllegalArgumentException("E debe ser positivo: " + error);
        }
        this.initialNumSeg = initialNumSeg;
        this.error = error;
    }

    public int getInitialNumSeg() {
        return initialNumSeg;
    }

    public double getError() {
        return error;
    }

    /**
     * Integra f de 0 a x hasta alcanzar el error aceptable.
     *
     * @throws IllegalArgumentException si x es NaN o infinito
     */
    public double integrate(DoubleUnaryOperator f, double x) {
        return integrate(f, 0.0, x);
    }

    /**
     * Integra f de {@code from} a {@code to} hasta alcanzar el error aceptable.
     *
     * @throws IllegalArgumentException si algun limite es NaN o infinito
     */
    public double integrate(DoubleUnaryOperator f, double from, double to) {
        return integrateWithDetails(f, from, to).value();
    }

    /**
     * Integra f de {@code from} a {@code to} y devuelve el valor junto con el
     * num_seg final, las iteraciones y la ultima diferencia.
     *
     * @throws IllegalArgumentException si algun limite es NaN o infinito
     */
    public Result integrateWithDetails(DoubleUnaryOperator f, double from, double to) {
        validateLimit("x", to);
        validateLimit("inicio", from);
        int numSeg = initialNumSeg;
        int iterations = 0;
        double previous = simpson(f, from, to, numSeg);
        while (true) {
            numSeg *= 2;
            iterations++;
            double current = simpson(f, from, to, numSeg);
            double difference = Math.abs(current - previous);
            boolean converged = difference < error;
            if (converged || numSeg >= MAX_NUM_SEG) {
                return new Result(current, numSeg, iterations, difference, converged);
            }
            previous = current;
        }
    }

    /**
     * Una sola pasada de Simpson de 0 a x:
     * p = W/3 [F(0) + sum 4F(iW) (i impar) + sum 2F(iW) (i par) + F(x)]
     */
    public static double simpson(DoubleUnaryOperator f, double x, int numSeg) {
        return simpson(f, 0.0, x, numSeg);
    }

    /**
     * Una sola pasada de Simpson de {@code from} a {@code to}, con W = (to - from) / num_seg.
     *
     * @throws IllegalArgumentException si num_seg no es par y positivo
     */
    public static double simpson(DoubleUnaryOperator f, double from, double to, int numSeg) {
        validateNumSeg(numSeg);
        double w = (to - from) / numSeg;
        double ends = f.applyAsDouble(from) + f.applyAsDouble(to);
        double odd = sumTerms(f, from, w, numSeg, 1);
        double even = sumTerms(f, from, w, numSeg, 2);
        return w / 3.0 * (ends + 4.0 * odd + 2.0 * even);
    }

    /** Suma F(from + iW) para i = first, first + 2, ... menor que num_seg. */
    private static double sumTerms(DoubleUnaryOperator f, double from, double w, int numSeg, int first) {
        double sum = 0.0;
        for (int i = first; i < numSeg; i += 2) {
            sum += f.applyAsDouble(from + i * w);
        }
        return sum;
    }

    private static void validateNumSeg(int numSeg) {
        if (numSeg <= 0 || numSeg % 2 != 0) {
            throw new IllegalArgumentException("num_seg debe ser par y positivo: " + numSeg);
        }
    }

    private static void validateLimit(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " debe ser un numero finito: " + value);
        }
    }
}
