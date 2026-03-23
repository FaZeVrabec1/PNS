import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class Lex {
    public static void main(String[] args) {
        String inputs[] = new String[]{"a+b*(12-3)/x","12*(a/2)-_a","1a-2","3-2?1"};

        for (String input : inputs){
            String output = Arrays.toString(lexAnalyser(input));
            System.out.println(input + ": " + output);
        }

    }

    /*
    Sestavite leksikalni analizator, ki razpoznava

celoštevilčne konstante (neprazno zaporedje števk),
imena (neprazno zaporedje črk, števk in podčrtajev, ki se ne začne s številko),
štiri osnovne aritmetične operatorje (+, -, *, /) in
oklepaje,
obenem pa izloći belo besedilo (presledke in znake za konec vrstice).

Leksikalni analizator napišite tako, da bo ob vsakem klicu vrnil naslednji leksikalni simbol,
ob napaki pa bo izpisal obvestilo o napaki in končal izvajanje programa.
    */


    public static String lexAnalyserStep(String input){

        //Number
        if (input.matches("[0-9]+")) {
            if (input.length() > 1 && input.charAt(0) == '0') {
                return "INTCONST can't start with 0";
            }
            return "INTCONST";
        }

        //ARITHMETIC SYMBOLS
        switch (input) {
            case "+":
                return "ADD";
            case "-":
                return "SUB";
            case "*":
                return "MUL";
            case "/":
                return "DIV";
        }

        //Parentheses
        switch (input){
            case "(":
                return "LeftParentheses";
            case ")":
                return "RightParentheses";
        }

        //Identifier
        if (input.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            return "Identifier";
        }

        //it's not defined
        return "Invalid token";
    }

    public static String[] lexAnalyser(String input) {
        List<String> tokens = new ArrayList<>();
        int i = 0;

        while (i < input.length()) {
            char c = input.charAt(i);

            // Ignore whitespace
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // Arethmic
            if (c == '+' || c == '-' || c == '*' || c == '/') {
                tokens.add(lexAnalyserStep(String.valueOf(c)));
                i++;
                continue;
            }

            // Parentheses
            if (c == '(' || c == ')') {
                tokens.add(lexAnalyserStep(String.valueOf(c)));
                i++;
                continue;
            }

            // Numbers
            if (Character.isDigit(c)) {
                StringBuilder number = new StringBuilder();
                while (i < input.length() && Character.isDigit(input.charAt(i))) {
                    number.append(input.charAt(i));
                    i++;
                }

                if (i < input.length() && Character.isLetter(input.charAt(i))) {
                    tokens.add("Identifiers cannot start with a number");
                    return tokens.toArray(new String[tokens.size()]);
                }

                tokens.add(lexAnalyserStep(number.toString()));
                continue;
            }

            // Identifiers
            if (Character.isLetter(c) || c == '_') {
                StringBuilder id = new StringBuilder();
                while (i < input.length() && (Character.isLetterOrDigit(input.charAt(i)) || input.charAt(i) == '_')) {
                    id.append(input.charAt(i));
                    i++;
                }
                tokens.add(lexAnalyserStep(id.toString()));
                continue;
            }

            // Invalid character
            tokens.add("Invalid character: " + c);
            return tokens.toArray(new String[tokens.size()]);
        }

        return tokens.toArray(new String[tokens.size()]);
    }

}