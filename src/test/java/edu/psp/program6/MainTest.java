package edu.psp.program6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

class MainTest {

    /** Linea que imprime el programa para p = 0.45 y dof = 15 cuando el usuario captura los valores. */
    private static final String ROW_045_15 = "0.4500    15  1.75305";

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    private int run(String input, String... args) {
        Scanner in = new Scanner(input);
        return Main.run(args, in,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    private String out() {
        return out.toString(StandardCharsets.UTF_8);
    }

    private String err() {
        return err.toString(StandardCharsets.UTF_8);
    }

    private static int count(String text, String part) {
        return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }

    @Test
    void withoutArgsPrintsTable1AndExitsOnEmptyP() {
        assertEquals(0, run("\n"));
        assertTrue(out().contains("Tabla 1"));
        assertTrue(out().contains("0.2000     6    0.55338    0.55338"));
        assertTrue(out().contains("0.4500    15    1.75305    1.75305"));
        assertTrue(out().contains("0.4950     4    4.60409    4.60410"));
        assertTrue(out().contains("p: "));
        assertEquals("", err());
    }

    @Test
    void exitsWhenInputEnds() {
        assertEquals(0, run(""));
        assertTrue(out().contains("4.60410"));
    }

    @Test
    void computesUserValues() {
        assertEquals(0, run("0.45\n15\n0.20\n6\n\n"));
        String output = out();
        assertEquals(1, count(output, ROW_045_15));
        assertEquals(1, count(output, "0.2000     6  0.55338"));
        assertEquals("", err());
    }

    @Test
    void acceptsCommaAsDecimalSeparator() {
        assertEquals(0, run("0,45\n15\n"));
        assertEquals(1, count(out(), ROW_045_15));
    }

    @Test
    void reportsInvalidInputAndKeepsAsking() {
        assertEquals(0, run("abc\n9\n0.45\n0\n0.45\n15\n\n"));
        assertEquals(2, count(err(), "Entrada invalida"));
        assertEquals(1, count(out(), ROW_045_15));
    }

    @Test
    void reportsPOutOfRange() {
        assertEquals(0, run("0.7\n9\n0\n9\n\n"));
        assertEquals(2, count(err(), "p debe estar entre 0 y 0.5"));
    }

    @Test
    void exitsWhenInputEndsBeforeDof() {
        assertEquals(0, run("0.45\n"));
        assertTrue(out().contains("dof: "));
    }

    @Test
    void computesCaseFromArguments() {
        assertEquals(0, run("", "0.45", "15"));
        assertTrue(out().contains(ROW_045_15));
        assertFalse(out().contains("Tabla 1"));
        assertEquals("", err());
    }

    @Test
    void traceFlagPrintsOneRowPerTrial() {
        assertEquals(0, run("", "--trace", "0.45", "15"));
        assertTrue(out().contains("intento"));
        // Cada renglon de la tabla de traza empieza con espacios y el numero de intento
        TInverseSearcher.Result r = new TInverseSearcher().search(0.45, 15);
        assertEquals(r.trials(), out().lines().filter(l -> l.matches("\\s+\\d+\\s+-?\\d+\\.\\d{5}\\s+.*")).count());
    }

    @Test
    void traceFlagAlsoWorksWithTable1() {
        assertEquals(0, run("\n", "--trace"));
        assertEquals(3, count(out(), "intento         x"));
    }

    @Test
    void traceFlagAlsoWorksWithInteractiveValues() {
        assertEquals(0, run("0.45\n15\n\n", "--trace"));
        assertEquals(4, count(out(), "intento         x"));
    }

    @Test
    void invalidArgumentsReturnError() {
        assertEquals(1, run("", "0.45", "-3"));
        assertTrue(err().contains("Entrada invalida"));
        assertTrue(err().contains(Main.USAGE));
    }

    @Test
    void pOutOfRangeInArgumentsReturnsError() {
        assertEquals(1, run("", "0.7", "9"));
        assertTrue(err().contains("p debe estar entre 0 y 0.5"));
    }

    @Test
    void wrongNumberOfArgumentsPrintsUsage() {
        assertEquals(1, run("", "0.45"));
        assertEquals(Main.USAGE, err().trim());
        assertEquals("", out());
    }

    @Test
    void tooManyArgumentsPrintsUsage() {
        assertEquals(1, run("", "0.45", "15", "extra"));
        assertEquals(Main.USAGE, err().trim());
    }

    @Test
    void warnsWhenTheSearchDoesNotConverge() {
        TInverseSearcher limited = new TInverseSearcher(new SimpsonIntegrator(),
                TInverseSearcher.DEFAULT_START, TInverseSearcher.DEFAULT_STEP,
                TInverseSearcher.DEFAULT_ERROR, 3);
        int status = Main.run(new String[] {"0.495", "4"}, new Scanner(""),
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8), limited);
        assertEquals(0, status);
        assertTrue(err().contains("no convergio en 3 intentos"));
        assertTrue(out().contains("0.4950     4"));
    }

    @Test
    void mainWithValidArgumentsDoesNotExit() {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        try {
            System.setIn(new ByteArrayInputStream(new byte[0]));
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            Main.main(new String[] {"0.45", "15"});
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
        assertTrue(out().contains("1.75305"));
    }
}
