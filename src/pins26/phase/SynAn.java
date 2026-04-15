package pins26.phase;

import java.util.*;

import pins26.common.*;

/**
 * Sintaksni analizator.
 */
public class SynAn implements AutoCloseable {

	/** Leksikalni analizator. */
	private final LexAn lexAn;

	/**
	 * Ustvari nov sintaksni analizator.
	 *
	 * @param srcFileName Ime izvorne datoteke.
	 */
	public SynAn(final String srcFileName) {
		this.lexAn = new LexAn(srcFileName);
	}

	@Override
	public void close() {
		lexAn.close();
	}

	/**
	 * Prevzame leksikalni analizator od leksikalnega analizatorja in preveri, ali
	 * je prave vrste.
	 *
	 * @param symbol Pricakovana vrsta leksikalnega simbola.
	 * @return Prevzeti leksikalni simbol.
	 */
	private Token check(Token.Symbol symbol) {
		final Token token = lexAn.takeToken();

		//Added for tracing
		traceToken(token);

		if (token.symbol() != symbol)
			throw new Report.Error(token, "Unexpected symbol '" + token.lexeme() + "'.");
		return token;
	}

	/**
	 * Opravi sintaksno analizo.
	 */
	public void parse() {
		program();

		if (lexAn.peekToken().symbol() != Token.Symbol.EOF)
			Report.warning(lexAn.peekToken(),
					"Unexpected text '" + lexAn.peekToken().lexeme() + "...' at the end of the program.");
	}

	//PROGRAM

	private void program() {
		trace("program");

		while (lexAn.peekToken().symbol() == Token.Symbol.FUN ||
				lexAn.peekToken().symbol() == Token.Symbol.VAR) {
			definition();

			if (lexAn.peekToken().symbol() == Token.Symbol.SEMIC)
				check(Token.Symbol.SEMIC);
		}
	}

	//DEFINITION

	private void definition() {
		trace("definition");

		if (lexAn.peekToken().symbol() == Token.Symbol.FUN) {
			check(Token.Symbol.FUN);
			check(Token.Symbol.IDENTIFIER);
			check(Token.Symbol.LPAREN);
			parameters();
			check(Token.Symbol.RPAREN);
			fun_def_opt();

		} else if (lexAn.peekToken().symbol() == Token.Symbol.VAR) {
			check(Token.Symbol.VAR);
			check(Token.Symbol.IDENTIFIER);
			check(Token.Symbol.ASSIGN);
			initializers();

		} else {
			throw new Report.Error(lexAn.peekToken(), "Expected definition.");
		}
	}

	private void fun_def_opt() {
		if (lexAn.peekToken().symbol() == Token.Symbol.ASSIGN) {
			check(Token.Symbol.ASSIGN);
			statements();
		}
	}

	private void def_opt() {
		while (lexAn.peekToken().symbol() == Token.Symbol.FUN ||
				lexAn.peekToken().symbol() == Token.Symbol.VAR) {
			definition();
		}
	}

	//PARAMETERS

	private void parameters() {
		trace("parameters");

		if (lexAn.peekToken().symbol() == Token.Symbol.IDENTIFIER) {
			check(Token.Symbol.IDENTIFIER);

			while (lexAn.peekToken().symbol() == Token.Symbol.COMMA) {
				check(Token.Symbol.COMMA);
				check(Token.Symbol.IDENTIFIER);
			}
		}
	}

	//STATEMENTS

	private void statements() {
		trace("statements");

		statement();

		while (lexAn.peekToken().symbol() == Token.Symbol.SEMIC) {
			check(Token.Symbol.SEMIC);

			if (isStatementStart(lexAn.peekToken().symbol()))
				statement();
			else
				break;
		}
	}

	private void statement() {
		trace("statement");

		Token.Symbol sym = lexAn.peekToken().symbol();

		if (sym == Token.Symbol.IF) {
			check(Token.Symbol.IF);
			expression();
			check(Token.Symbol.THEN);
			statements();
			else_opt();
			check(Token.Symbol.END);

		} else if (sym == Token.Symbol.WHILE) {
			check(Token.Symbol.WHILE);
			expression();
			check(Token.Symbol.DO);
			statements();
			check(Token.Symbol.END);

		} else if (sym == Token.Symbol.LET) {
			check(Token.Symbol.LET);
			definition();
			def_opt();
			check(Token.Symbol.IN);
			statements();
			check(Token.Symbol.END);

		} else {
			expression();

			if (lexAn.peekToken().symbol() == Token.Symbol.ASSIGN) {
				check(Token.Symbol.ASSIGN);
				expression();
			}
		}
	}

	private void else_opt() {
		if (lexAn.peekToken().symbol() == Token.Symbol.ELSE) {
			check(Token.Symbol.ELSE);
			statements();
		}
	}

	//EXPRESSIONS

	private void expression() {
		trace("expression");
		disjunction();
	}

	private void disjunction() {
		trace("disjunction");
		conjunction();

		while (lexAn.peekToken().symbol() == Token.Symbol.OR) {
			check(Token.Symbol.OR);
			conjunction();
		}
	}

	private void conjunction() {
		trace("conjunction");
		comparison();

		while (lexAn.peekToken().symbol() == Token.Symbol.AND) {
			check(Token.Symbol.AND);
			comparison();
		}
	}

	private void comparison() {
		trace("comparison");
		additive();

		Token.Symbol sym = lexAn.peekToken().symbol();

		if (sym == Token.Symbol.EQU || sym == Token.Symbol.NEQ ||
				sym == Token.Symbol.LTH || sym == Token.Symbol.GTH ||
				sym == Token.Symbol.LEQ || sym == Token.Symbol.GEQ) {
			check(sym);
			additive();
		}
	}

	private void additive() {
		trace("additive");
		multiplicative();

		while (lexAn.peekToken().symbol() == Token.Symbol.ADD ||
				lexAn.peekToken().symbol() == Token.Symbol.SUB) {
			check(lexAn.peekToken().symbol());
			multiplicative();
		}
	}

	private void multiplicative() {
		trace("multiplicative");
		prefix();

		while (lexAn.peekToken().symbol() == Token.Symbol.MUL ||
				lexAn.peekToken().symbol() == Token.Symbol.DIV ||
				lexAn.peekToken().symbol() == Token.Symbol.MOD) {
			check(lexAn.peekToken().symbol());
			prefix();
		}
	}

	private void prefix() {
		trace("prefix");

		if (lexAn.peekToken().symbol() == Token.Symbol.NOT ||
				lexAn.peekToken().symbol() == Token.Symbol.ADD ||
				lexAn.peekToken().symbol() == Token.Symbol.SUB ||
				lexAn.peekToken().symbol() == Token.Symbol.PTR) {
			check(lexAn.peekToken().symbol());
			prefix();
		} else {
			postfix();
		}
	}

	private void postfix() {
		trace("postfix");
		primary_exp();

		//Check if correct postfix operator implementation
		if (lexAn.peekToken().symbol() == Token.Symbol.PTR) {
			check(Token.Symbol.PTR);
		}
	}

	//PRIMARY

	private void primary_exp() {
		trace("primary_exp");

		Token.Symbol sym = lexAn.peekToken().symbol();

		if (sym == Token.Symbol.LPAREN) {
			check(Token.Symbol.LPAREN);
			expression();
			check(Token.Symbol.RPAREN);

		} else if (sym == Token.Symbol.INTCONST ||
				sym == Token.Symbol.CHARCONST ||
				sym == Token.Symbol.STRINGCONST) {
			check(sym);

		} else if (sym == Token.Symbol.IDENTIFIER) {
			check(Token.Symbol.IDENTIFIER);
			exp_args_opt();

		} else {
			throw new Report.Error(lexAn.peekToken(), "Expected primary expression.");
		}
	}

	private void exp_args_opt() {
		if (lexAn.peekToken().symbol() == Token.Symbol.LPAREN) {
			check(Token.Symbol.LPAREN);
			arguments();
			check(Token.Symbol.RPAREN);
		}
	}

	private void arguments() {
		trace("arguments");

		if (isExpressionStart(lexAn.peekToken().symbol())) {
			expression();

			while (lexAn.peekToken().symbol() == Token.Symbol.COMMA) {
				check(Token.Symbol.COMMA);
				expression();
			}
		}
	}

	//INITIALIZERS

	private void initializers() {
		trace("initializers");

		initializer();

		while (lexAn.peekToken().symbol() == Token.Symbol.COMMA) {
			check(Token.Symbol.COMMA);
			initializer();
		}
	}

	private void initializer() {
		trace("initializer");

		konst();

		if (lexAn.peekToken().symbol() == Token.Symbol.MUL) {
			check(Token.Symbol.MUL);
			konst();
		}
	}

	private void konst() {
		trace("const");

		Token.Symbol sym = lexAn.peekToken().symbol();

		if (sym == Token.Symbol.INTCONST ||
				sym == Token.Symbol.CHARCONST ||
				sym == Token.Symbol.STRINGCONST) {
			check(sym);
		} else {
			throw new Report.Error(lexAn.peekToken(), "Expected constant.");
		}
	}

	//HELPERS
	private boolean isExpressionStart(Token.Symbol sym) {
		return sym == Token.Symbol.IDENTIFIER ||
				sym == Token.Symbol.INTCONST ||
				sym == Token.Symbol.CHARCONST ||
				sym == Token.Symbol.STRINGCONST ||
				sym == Token.Symbol.LPAREN ||
				sym == Token.Symbol.NOT ||
				sym == Token.Symbol.ADD ||
				sym == Token.Symbol.SUB;
	}

	private boolean isStatementStart(Token.Symbol sym) {
		return isExpressionStart(sym) ||
				sym == Token.Symbol.IF ||
				sym == Token.Symbol.WHILE ||
				sym == Token.Symbol.LET;
	}

	private void trace(String msg) {
		System.out.println(msg);
	}

	private void traceToken(Token token) {
		System.out.println(token.symbol() + "(" + token.lexeme() + ")");
	}

        /*** TODO ***/

	// --- ZAGON ---

	/**
	 * Zagon sintaksnega analizatorja kot samostojnega programa.
	 *
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'26 compiler (syntax analysis):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
				synAn.parse();
			}

			// Upajmo, da kdaj pridemo to te tocke.
			// A zavedajmo se sledecega:
			// 1. Prevod je zaradi napak v programu lahko napacen :-o
			// 2. Izvorni program se zdalec ni tisto, kar je programer hotel, da bi bil ;-)
			Report.info("Done.");
		} catch (Report.Error error) {
			// Izpis opisa napake.
			System.err.println(error.getMessage());
			System.exit(1);
		}
	}

}
