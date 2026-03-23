import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class Sintax {

    static String[] tokens;
    static int pos = 0;

    public static void main(String[] args) {
        String inputs[] = new String[]{"a + b * 3","(x1 - 12) / y2","a - + 2"};
        for (String input : inputs){
            System.out.println("Input: " + input);
            parse(input);
            System.out.println();
        }
    }

    public static void parse(String input) {
        Lex lexAnalyser = new Lex();
        tokens = lexAnalyser.lexAnalyser(input);
        pos = 0;

        System.out.println(Arrays.toString(tokens));

        E();

        if (pos != tokens.length) {
            error("Unexpected token at end: " + current());
        }

        System.out.println("SUCCESS");
    }

    // E → T E2
    static void E() {
        System.out.println("E -> T E2");
        T();
        Eprime();
    }

    // E2 → + T E2 | - T E2 | e
    static void Eprime() {
        if (match("ADD")) {
            System.out.println("E2 -> + T E2");
            T();
            Eprime();
        } else if (match("SUB")) {
            System.out.println("E2 -> - T E2");
            T();
            Eprime();
        } else {
            System.out.println("E2 -> e");
        }
    }

    // T → F T2
    static void T() {
        System.out.println("T -> F T2");
        F();
        Tprime();
    }

    // T2 → * F T2 | / F T2 | e
    static void Tprime() {
        if (match("MUL")) {
            System.out.println("T2 -> * F T2");
            F();
            Tprime();
        } else if (match("DIV")) {
            System.out.println("T2 -> / F T2");
            F();
            Tprime();
        } else {
            System.out.println("T2 -> e");
        }
    }

    // F → ( E ) | id | INTCONST
    static void F() {
        if (match("LeftParentheses")) {
            System.out.println("F -> ( E )");
            E();
            if (!match("RightParentheses")) {
                error("Missing closing )");
            }
        } else if (match("Identifier")) {
            System.out.println("F -> id");
        } else if (match("INTCONST")) {
            System.out.println("F -> INTCONST");
        } else {
            error("Unexpected token: " + current());
        }
    }

    static boolean match(String expected) {
        if (pos < tokens.length && tokens[pos].equals(expected)) {
            pos++;
            return true;
        }
        return false;
    }

    static String current() {
        if (pos < tokens.length) return tokens[pos];
        return "EOF";
    }

    static void error(String msg) {
        System.out.println("Error: " + msg);
        System.exit(1);
    }
}