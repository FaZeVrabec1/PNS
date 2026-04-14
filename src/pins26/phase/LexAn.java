package pins26.phase;

import java.io.*;

import pins26.common.*;

/**
 * Leksikalni analizator.
 */
public class LexAn implements AutoCloseable {

	/** Izvorna datoteka. */
	private final Reader srcFile;

	/**
	 * Ustvari nov leksikalni analizator.
	 * 
	 * @param srcFileName Ime izvorne datoteke.
	 */
	public LexAn(final String srcFileName) {
		try {
			srcFile = new BufferedReader(new InputStreamReader(new FileInputStream(new File(srcFileName))));
			nextChar(); // Pripravi prvi znak izvorne datoteke (glej {@link nextChar}).
		} catch (FileNotFoundException __) {
			throw new Report.Error("Source file '" + srcFileName + "' not found.");
		}
	}

	@Override
	public void close() {
		try {
			srcFile.close();
		} catch (IOException __) {
			throw new Report.Error("Cannot close source file.");
		}
	}

	/** Trenutni znak izvorne datoteke (glej {@link nextChar}). */
	private int buffChar = -2;

	/** Vrstica trenutnega znaka izvorne datoteke (glej {@link nextChar}). */
	private int buffCharLine = 0;

	/** Stolpec trenutnega znaka izvorne datoteke (glej {@link nextChar}). */
	private int buffCharColumn = 0;

	/**
	 * Prebere naslednji znak izvorne datoteke.
	 * 
	 * Izvorno datoteko beremo znak po znak. Trenutni znak izvorne datoteke je
	 * shranjen v spremenljivki {@link buffChar}, vrstica in stolpec trenutnega
	 * znaka izvorne datoteke sta shranjena v spremenljivkah {@link buffCharLine} in
	 * {@link buffCharColumn}.
	 * 
	 * Zacetne vrednosti {@link buffChar}, {@link buffCharLine} in
	 * {@link buffCharColumn} so {@code '\n'}, {@code 0} in {@code 0}: branje prvega
	 * znaka izvorne datoteke bo na osnovi vrednosti {@code '\n'} spremenljivke
	 * {@link buffChar} prvemu znaku izvorne datoteke priredilo vrstico 1 in stolpec
	 * 1.
	 * 
	 * Pri branju izvorne datoteke se predpostavlja, da je v spremenljivki
	 * {@link buffChar} ves "cas veljaven znak. Zunaj metode {@link nextChar} so vse
	 * spremenljivke {@link buffChar}, {@link buffCharLine} in
	 * {@link buffCharColumn} namenjene le branju.
	 * 
	 * Vrednost {@code -1} v spremenljivki {@link buffChar} pomeni konec datoteke
	 * (vrednosti spremenljivk {@link buffCharLine} in {@link buffCharColumn} pa
	 * nista ve"c veljavni).
	 */
	private void nextChar() {
		try {
			switch (buffChar) {
			case -2: // Noben znak "se ni bil prebran.
				buffChar = srcFile.read();
				buffCharLine = buffChar == -1 ? 0 : 1;
				buffCharColumn = buffChar == -1 ? 0 : 1;
				return;
			case -1: // Konec datoteke je bil "ze viden.
				return;
			case '\n': // Prejsnji znak je koncal vrstico, zacne se nova vrstica.
				buffChar = srcFile.read();
				buffCharLine = buffChar == -1 ? buffCharLine : buffCharLine + 1;
				buffCharColumn = buffChar == -1 ? buffCharColumn : 1;
				return;
			case '\t': // Prejsnji znak je tabulator, ta znak je morda potisnjen v desno.
				buffChar = srcFile.read();
				while (buffCharColumn % 4 != 0)
					buffCharColumn += 1;
				buffCharColumn += 1;
				return;
			default: // Prejsnji znak je brez posebnosti.
				buffChar = srcFile.read();
				buffCharColumn += 1;
				return;
			}
		} catch (IOException __) {
			throw new Report.Error("Cannot read source file.");
		}
	}

	/**
	 * Trenutni leksikalni simbol.
	 * 
	 * "Ce vrednost spremenljivke {@code buffToken} ni {@code null}, je simbol "ze
	 * prebran iz vhodne datoteke, ni pa "se predan naprej sintaksnemu analizatorju.
	 * Ta simbol je dostopen z metodama {@link peekToken} in {@link takeToken}.
	 */
	private Token buffToken = null;

	/**
	 * Prebere naslednji leksikalni simbol, ki je nato dostopen preko metod
	 * {@link peekToken} in {@link takeToken}.
	 */
	private void nextToken() {

		// Follow the order of: whitespace -> EOF -> ID & keywords -> INT -> Char -> String -> Comments -> Symbols


		int startLine = buffCharLine;
		int startColumn = buffCharColumn;
		StringBuilder lexeme = new StringBuilder();

		// Skip whitespace and comments
		while (true) {

			// Skip whitespace
			while (Character.isWhitespace(buffChar)) {
				nextChar();
			}

			// Handle comments
			if (buffChar == '/') {
				startLine = buffCharLine;
				startColumn = buffCharColumn;

				nextChar();

				if (buffChar == '/') {
					// Skip comment until end of line
					while (buffChar != '\n' && buffChar != -1) {
						nextChar();
					}
					// Continue outer loop (treat comment like whitespace)
					continue;
				} else {
					// It's DIV, not a comment so return token
					buffToken = new Token(
							new Report.Location(startLine, startColumn),
							Token.Symbol.DIV,
							"/"
					);
					return;
				}
			}

			// No more whitespace/comments
			break;
		}

		// End of file
		if (buffChar == -1) {
			buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Token.Symbol.EOF, "");
			return;
		}

		startLine = buffCharLine;
		startColumn = buffCharColumn;
		//Identifiers and Keywords
		if (Character.isLetter(buffChar) || buffChar == '_') {
			lexeme = new StringBuilder();
			while (Character.isLetterOrDigit(buffChar) || buffChar == '_') {
				lexeme.append((char) buffChar);
				nextChar();
			}
			String lexemeStr = lexeme.toString();

			Token.Symbol symbol;
			switch (lexemeStr) {
				case "fun":   symbol = Token.Symbol.FUN; 		break;
				case "var":	  symbol = Token.Symbol.VAR; 		break;
				case "if":    symbol = Token.Symbol.IF; 		break;
				case "then":  symbol = Token.Symbol.THEN; 		break;
				case "else":  symbol = Token.Symbol.ELSE; 		break;
				case "while": symbol = Token.Symbol.WHILE; 		break;
				case "do":    symbol = Token.Symbol.DO;			break;
				case "let":   symbol = Token.Symbol.LET;		break;
				case "in":    symbol = Token.Symbol.IN;			break;
				case "end":	  symbol = Token.Symbol.END;        break;
				default:      symbol = Token.Symbol.IDENTIFIER; break;
			}
			buffToken = new Token(new Report.Location(startLine, startColumn, startLine, startColumn + lexemeStr.length() - 1), symbol, lexemeStr);
			return;
		}

		//INTCONST
		if (Character.isDigit(buffChar)){
			startLine = buffCharLine;
			startColumn = buffCharColumn;
			lexeme = new StringBuilder();

			while (Character.isDigit(buffChar)){
				lexeme.append((char) buffChar);
				nextChar();
			}

			buffToken = new Token(new Report.Location(startLine, startColumn, buffCharLine, buffCharColumn), Token.Symbol.INTCONST, lexeme.toString());
			return;
		}

		//CHARCONST
		if (buffChar == '\'') {
			lexeme = new StringBuilder();
			int charStartLine = startLine;
			int charStartColumn = startColumn;
			lexeme.append((char) buffChar); // Add the opening single quote
			nextChar();

			if (buffChar == -1 || buffChar == '\n') {
				throw new Report.Error(new Report.Location(charStartLine, charStartColumn), "Unclosed CHARCONST.");
			}

			if (buffChar == '\\') { // Handle escape sequences
				lexeme.append((char) buffChar);
				nextChar();

				if ((buffChar >= '0' && buffChar <= '9') ||
						(buffChar >= 'a' && buffChar <= 'f') ||
						(buffChar >= 'A' && buffChar <= 'F')) {

					// prvi hex znak
					char first = (char) buffChar;
					lexeme.append(first);
					nextChar();

					if (!((buffChar >= '0' && buffChar <= '9') ||
							(buffChar >= 'a' && buffChar <= 'f') ||
							(buffChar >= 'A' && buffChar <= 'F'))) {

						throw new Report.Error(
								new Report.Location(charStartLine, charStartColumn),
								"Invalid hex escape in CHARCONST."
						);
					}

					// drugi hex znak
					char second = (char) buffChar;
					lexeme.append(second);

				} else {
					switch (buffChar) {
						case 'n':
							lexeme.append('n');
							break;
						case '\'':
							lexeme.append('\'');
							break;
						case '\\':
							lexeme.append('\\');
							break;
						default:
							throw new Report.Error(new Report.Location(charStartLine, charStartColumn), "Invalid escape sequence in CHARCONST.");
					}
				}
			} else { // Handle regular characters
				if (buffChar < 32 || buffChar > 126) {
					throw new Report.Error(new Report.Location(charStartLine, charStartColumn), "Invalid character in CHARCONST.");
				}
				lexeme.append((char) buffChar);
			}

			nextChar();

			if (buffChar != '\'') { // Ensure the CHARCONST ends with a single quote
				throw new Report.Error(new Report.Location(charStartLine, charStartColumn), "Unclosed CHARCONST.");
			}

			lexeme.append((char) buffChar); // Add the closing single quote
			int charEndColumn = buffCharColumn; // This is the column of the closing quote
			nextChar();

			buffToken = new Token(
					new Report.Location(charStartLine, charStartColumn, charStartLine, charEndColumn),
					Token.Symbol.CHARCONST,
					lexeme.toString()
			);
			return;
		}

		//STRINGCONST
		if (buffChar == '"'){
			startLine = buffCharLine;
			startColumn = buffCharColumn;
			lexeme = new StringBuilder();
			nextChar();

			while (buffChar != '"'){
				//Check for valid charecters
				if	(32 <= buffChar && buffChar <= 126){
					lexeme.append((char) buffChar);
					nextChar();
				}
				else {
					throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Invalid char.");
				}

			}

			nextChar();

			buffToken = new Token(new Report.Location(startLine, startColumn, buffCharLine, buffCharColumn), Token.Symbol.STRINGCONST, lexeme.toString());
			return;
		}



		// Symbols
		startLine = buffCharLine;
		startColumn = buffCharColumn;
		String lexemeStr = String.valueOf((char) buffChar);
		Token.Symbol symbol = null;
		switch (buffChar) {
			case '=':
				nextChar();
				if (buffChar == '=') {
					symbol = Token.Symbol.EQU;
					lexemeStr += "=";
					nextChar();
				} else {
					symbol = Token.Symbol.ASSIGN;
				}
				break;
			case '!':
				nextChar();
				if (buffChar == '=') {
					symbol = Token.Symbol.NEQ;
					lexemeStr += "=";
					nextChar();
				} else {
					symbol = Token.Symbol.NOT;
				}
				break;
			case '>':
				nextChar();
				if (buffChar == '=') {
					symbol = Token.Symbol.GEQ;
					lexemeStr += "=";
					nextChar();
				} else {
					symbol = Token.Symbol.GTH;
				}
				break;
			case '<':
				nextChar();
				if (buffChar == '=') {
					symbol = Token.Symbol.LEQ;
					lexemeStr += "=";
					nextChar();
				} else {
					symbol = Token.Symbol.LTH;
				}
				break;
			case '&':
				nextChar();
				if (buffChar == '&') {
					symbol = Token.Symbol.AND;
					lexemeStr += "&";
					nextChar();
				} else {
					throw new Report.Error(new Report.Location(startLine, startColumn), "Unknown symbol: &");
				}
				break;
			case '|':
				nextChar();
				if (buffChar == '|') {
					symbol = Token.Symbol.OR;
					lexemeStr += "|";
					nextChar();
				} else {
					throw new Report.Error(new Report.Location(startLine, startColumn), "Unknown symbol: |");
				}
				break;
			case '+': symbol = Token.Symbol.ADD; nextChar(); break;
			case '-': symbol = Token.Symbol.SUB; nextChar(); break;
			case '*': symbol = Token.Symbol.MUL; nextChar(); break;
			case '/': symbol = Token.Symbol.DIV; nextChar(); break;
			case '%': symbol = Token.Symbol.MOD; nextChar(); break;
			case '^': symbol = Token.Symbol.PTR; nextChar(); break;
			case '(': symbol = Token.Symbol.LPAREN; nextChar(); break;
			case ')': symbol = Token.Symbol.RPAREN; nextChar(); break;
			case ',': symbol = Token.Symbol.COMMA; nextChar(); break;
			case ';': symbol = Token.Symbol.SEMIC; nextChar(); break;
			default:
				throw new Report.Error(new Report.Location(startLine, startColumn), "Unknown symbol: " + (char) buffChar);
		}

		buffToken = new Token(new Report.Location(startLine, startColumn), symbol, lexeme.toString());
		return;

	}

	/**
	 * Vrne trenutni leksikalni simbol, ki ostane v lastnistvu leksikalnega
	 * analizatorja.
	 * 
	 * @return Leksikalni simbol.
	 */
	public Token peekToken() {
		if (buffToken == null)
			nextToken();
		return buffToken;
	}

	/**
	 * Vrne trenutni leksikalni simbol, ki preide v lastnistvo klicoce kode.
	 * 
	 * @return Leksikalni simbol.
	 */
	public Token takeToken() {
		if (buffToken == null)
			nextToken();
		final Token thisToken = buffToken;
		buffToken = null;
		return thisToken;
	}

	// --- ZAGON ---

	/**
	 * Zagon leksikalnega analizatorja kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'26 compiler (lexical analysis):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (LexAn lexAn = new LexAn(cmdLineArgs[0])) {
				while (lexAn.peekToken().symbol() != Token.Symbol.EOF)
					System.out.println(lexAn.takeToken());
				System.out.println(lexAn.takeToken());
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
