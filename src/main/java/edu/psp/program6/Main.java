package edu.psp.program6;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Scanner;

/**
 * Programa 6: encuentra el valor de x para el cual la integral de la funcion t
 * de 0 a x da como resultado p, usando el algoritmo de busqueda del kit.
 *
 * <p>Uso: {@code java -jar program6.jar [--trace] [p dof]}
 * <ul>
 *   <li>Con {@code p dof} calcula solo ese caso.</li>
 *   <li>Sin ellos ejecuta los casos de la Tabla 1 y despues pide valores de p y
 *       dof por teclado hasta que se deje p vacio.</li>
 *   <li>Con {@code --trace} muestra ademas la tabla de intentos de la busqueda.</li>
 * </ul>
 */
public final class Main {

    static final String USAGE = "Uso: java -jar program6.jar [--trace] [p dof]";
    static final String TRACE_FLAG = "--trace";

    /** Casos de la Tabla 1 del kit: p, dof y valor esperado de x. */
    private static final double[][] TABLE1 = {
            {0.20, 6, 0.55338},
            {0.45, 15, 1.75305},
            {0.495, 4, 4.60409}
    };

    private Main() {
    }

    public static void main(String[] args) {
        int status = run(args, new Scanner(System.in), System.out, System.err);
        if (status != 0) {
            System.exit(status);
        }
    }

    /**
     * Ejecuta el programa leyendo de {@code in} y escribiendo en {@code out}/{@code err}.
     *
     * @return codigo de salida (0 si todo salio bien)
     */
    static int run(String[] args, Scanner in, PrintStream out, PrintStream err) {
        return run(args, in, out, err, new TInverseSearcher());
    }

    /**
     * Igual que {@link #run(String[], Scanner, PrintStream, PrintStream)} pero con el
     * buscador indicado, para poder probar el programa con otra configuracion.
     */
    static int run(String[] args, Scanner in, PrintStream out, PrintStream err, TInverseSearcher searcher) {
        boolean trace = false;
        int count = 0;
        String[] values = new String[args.length];
        for (String arg : args) {
            if (TRACE_FLAG.equals(arg)) {
                trace = true;
            } else {
                values[count++] = arg;
            }
        }

        if (count == 2) {
            try {
                printResult(out, err, searcher, parseP(values[0]), parseDof(values[1]), trace);
                return 0;
            } catch (IllegalArgumentException e) {
                // NumberFormatException tambien es IllegalArgumentException
                err.println("Entrada invalida: " + e.getMessage());
                err.println(USAGE);
                return 1;
            }
        }
        if (count != 0) {
            err.println(USAGE);
            return 1;
        }

        out.println("Tabla 1");
        printTable1Header(out);
        for (double[] c : TABLE1) {
            TInverseSearcher.Result r = searcher.search(c[0], (int) c[1]);
            printTable1Row(out, c[0], (int) c[1], c[2], r.x());
            if (trace) {
                printTrace(out, r);
            }
        }

        out.println();
        out.println("Ingresa tus valores (deja p vacio para salir)");
        while (true) {
            String p = prompt(in, out, "p: ");
            if (p == null || p.isEmpty()) {
                return 0;
            }
            String dof = prompt(in, out, "dof: ");
            if (dof == null) {
                return 0;
            }
            try {
                printResult(out, err, searcher, parseP(p), parseDof(dof), trace);
            } catch (IllegalArgumentException e) {
                err.println("Entrada invalida: " + e.getMessage());
            }
            out.println();
        }
    }

    private static String prompt(Scanner in, PrintStream out, String label) {
        out.print(label);
        out.flush();
        return in.hasNextLine() ? in.nextLine().trim() : null;
    }

    /** Acepta coma o punto como separador decimal (0,45 o 0.45). */
    private static double parseP(String text) {
        return Double.parseDouble(text.trim().replace(',', '.'));
    }

    private static int parseDof(String text) {
        return Integer.parseInt(text.trim());
    }

    private static void printResult(PrintStream out, PrintStream err, TInverseSearcher searcher,
                                    double p, int dof, boolean trace) {
        TInverseSearcher.Result r = searcher.search(p, dof);
        out.println("     p   dof         x  intentos");
        out.println(String.format(Locale.US, "%6.4f  %4d  %.5f  %8d", p, dof, r.x(), r.trials()));
        if (trace) {
            printTrace(out, r);
        }
        if (!r.converged()) {
            err.println("Aviso: la busqueda no convergio en " + r.trials() + " intentos");
        }
    }

    private static void printTable1Header(PrintStream out) {
        out.println("     p   dof   esperado     actual");
    }

    private static void printTable1Row(PrintStream out, double p, int dof, double expected, double actual) {
        out.println(String.format(Locale.US, "%6.4f  %4d    %.5f    %.5f", p, dof, expected, actual));
    }

    /** Imprime la tabla de traza: un renglon por cada intento de la busqueda. */
    private static void printTrace(PrintStream out, TInverseSearcher.Result r) {
        out.println("  intento         x       integral        error        d");
        for (TInverseSearcher.Trial t : r.history()) {
            out.println(String.format(Locale.US, "  %7d  %.5f      %.5f  %+.6f  %.5f",
                    t.number(), t.x(), t.integral(), t.error(), t.step()));
        }
    }
}
