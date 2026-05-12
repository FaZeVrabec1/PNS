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
//		traceToken(token);

		if (token.symbol() != symbol)
			throw new Report.Error(token, "Unexpected symbol '" + token.lexeme() + "'.");
		return token;
	}

	/**
	 * Opravi sintaksno analizo.
	 */
//	SynAn version
//	public void parse() {
//		program();
//
//		if (lexAn.peekToken().symbol() != Token.Symbol.EOF)
//			Report.warning(lexAn.peekToken(),
//					"Unexpected text '" + lexAn.peekToken().lexeme() + "...' at the end of the program.");
//	}

//	Abstr version
	public AST.Node parse(HashMap<AST.Node, Report.Locatable> attrLoc) {
		this.attrLoc = attrLoc;
		final AST.Nodes<AST.MainDef> defs = parseProgram();
		if (lexAn.peekToken().symbol() != Token.Symbol.EOF)
			Report.warning(lexAn.peekToken(),
					"Unexpected text '" + lexAn.peekToken().lexeme() + "...' at the end of the program.");
		return defs;
	}

	private HashMap<AST.Node, Report.Locatable> attrLoc;

//	AST parser

	//PROGRAM

	private AST.Nodes<AST.MainDef> parseProgram() {
		Vector<AST.MainDef> defs = new Vector<>();

		while (lexAn.peekToken().symbol() == Token.Symbol.FUN ||
				lexAn.peekToken().symbol() == Token.Symbol.VAR) {

			AST.MainDef def = parseDefinition();
			defs.add(def);

			if (lexAn.peekToken().symbol() == Token.Symbol.SEMIC)
				check(Token.Symbol.SEMIC);
		}

		return new AST.Nodes<>(defs);
	}

	//DEFINITION
	private AST.MainDef parseDefinition() {

		if (lexAn.peekToken().symbol() == Token.Symbol.FUN) {

			Token funToken = check(Token.Symbol.FUN);
			Token name = check(Token.Symbol.IDENTIFIER);

			check(Token.Symbol.LPAREN);
			List<AST.ParDef> params = parseParameters();
			check(Token.Symbol.RPAREN);

			List<AST.Stmt> body = new Vector<>();

			if (lexAn.peekToken().symbol() == Token.Symbol.ASSIGN) {
				check(Token.Symbol.ASSIGN);
				body = parseStatements();
			}

			AST.MainDef node = new AST.FunDef(name.lexeme(), params, body);
			attrLoc.put(node, funToken);

			return node;
		}

		else if (lexAn.peekToken().symbol() == Token.Symbol.VAR) {

			Token varTok = check(Token.Symbol.VAR);
			Token name = check(Token.Symbol.IDENTIFIER);

			check(Token.Symbol.ASSIGN);
			List<AST.Init> inits = parseInitializers();

			AST.MainDef node2 = new AST.VarDef(name.lexeme(), inits);
			attrLoc.put(node2, varTok);

			return node2;
		}

		throw new Report.Error(lexAn.peekToken(), "Expected definition.");
	}

	private List<AST.ParDef> parseParameters() {

		List<AST.ParDef> params = new Vector<>();
		Token t = lexAn.peekToken();

		if (t.symbol() == Token.Symbol.IDENTIFIER) {

			Token id = check(Token.Symbol.IDENTIFIER);

			AST.ParDef param = new AST.ParDef(id.lexeme());
			attrLoc.put(param, id);
			params.add(param);


			while (lexAn.peekToken().symbol() == Token.Symbol.COMMA) {
				check(Token.Symbol.COMMA);
				id = check(Token.Symbol.IDENTIFIER);
				AST.ParDef param2 = new AST.ParDef(id.lexeme());
				attrLoc.put(param2, id);
				params.add(param2);
			}
		}

		return params;
	}

	//STATEMENTS
	private List<AST.Stmt> parseStatements() {

		List<AST.Stmt> stmts = new Vector<>();
		stmts.add(parseStatement());

		while (lexAn.peekToken().symbol() == Token.Symbol.SEMIC) {
			check(Token.Symbol.SEMIC);

			if (isStatementStart(lexAn.peekToken().symbol())) {
				stmts.add(parseStatement());
			} else {
				break;
			}
		}

		return stmts;
	}

	private AST.Stmt parseStatement() {

		Token.Symbol sym = lexAn.peekToken().symbol();

		// IF
		if (sym == Token.Symbol.IF) {

			Token ifTok = check(Token.Symbol.IF);

			AST.Expr cond = parseExpression();
			check(Token.Symbol.THEN);

			List<AST.Stmt> thenPart = parseStatements();
			List<AST.Stmt> elsePart = new Vector<>();

			if (lexAn.peekToken().symbol() == Token.Symbol.ELSE) {
				check(Token.Symbol.ELSE);
				elsePart = parseStatements();
			}

			check(Token.Symbol.END);

			AST.Stmt node = new AST.IfStmt(cond, thenPart, elsePart);
			attrLoc.put(node, ifTok);

			return node;
		}

		// WHILE
		else if (sym == Token.Symbol.WHILE) {

			Token whileTok = check(Token.Symbol.WHILE);

			AST.Expr cond = parseExpression();
			check(Token.Symbol.DO);

			List<AST.Stmt> body = parseStatements();
			check(Token.Symbol.END);

			AST.Stmt node = new AST.WhileStmt(cond, body);
			attrLoc.put(node, whileTok);

			return node;
		}

		// LET
		else if (sym == Token.Symbol.LET) {

			Token letTok = check(Token.Symbol.LET);

			List<AST.MainDef> defs = new Vector<>();

			// at least one definition is required by grammar
			defs.add(parseDefinition());

			// optional more defs (same line style LET block)
			while (lexAn.peekToken().symbol() == Token.Symbol.FUN ||
					lexAn.peekToken().symbol() == Token.Symbol.VAR) {
				defs.add(parseDefinition());
			}

			check(Token.Symbol.IN);

			List<AST.Stmt> body = parseStatements();
			check(Token.Symbol.END);

			AST.Stmt node = new AST.LetStmt(defs, body);
			attrLoc.put(node, letTok);

			return node;
		}

		// ASSIGNMENT OR EXPRESSION
		else {

			AST.Expr left = parseExpression();

			if (lexAn.peekToken().symbol() == Token.Symbol.ASSIGN) {

				if (!(left instanceof AST.VarExpr ||
						(left instanceof AST.UnExpr &&
								((AST.UnExpr) left).oper == AST.UnExpr.Oper.VALUEAT))) {

					throw new Report.Error(lexAn.peekToken(), "Invalid assignment target.");
				}

				Token assignTok = check(Token.Symbol.ASSIGN);
				AST.Expr right = parseExpression();

				AST.Stmt node = new AST.AssignStmt(left, right);
				attrLoc.put(node, assignTok);

				return node;
			}

			// expression statement
			AST.Stmt node = new AST.ExprStmt(left);
			attrLoc.put(node, lexAn.peekToken());

			return node;
		}
	}


	private AST.Expr parseExpression() {
		return parseDisjunction();
	}

	private AST.Expr parseDisjunction() {
		AST.Expr left = parseConjunction();

		while (lexAn.peekToken().symbol() == Token.Symbol.OR) {
			Token op = check(Token.Symbol.OR);
			AST.Expr right = parseConjunction();

			AST.Expr node = new AST.BinExpr(AST.BinExpr.Oper.OR, left, right);
			attrLoc.put(node, op);

			left = node;
		}

		return left;
	}

	private AST.Expr parseConjunction() {

		AST.Expr left = parseComparison();

		while (lexAn.peekToken().symbol() == Token.Symbol.AND) {
			Token op = check(Token.Symbol.AND);
			AST.Expr right = parseComparison();

			AST.Expr node = new AST.BinExpr(AST.BinExpr.Oper.AND, left, right);
			attrLoc.put(node, op);

			left = node;
		}

		return left;
	}

	private AST.Expr parseComparison() {

		AST.Expr left = parseAdditive();

		Token.Symbol sym = lexAn.peekToken().symbol();

		if (sym == Token.Symbol.EQU || sym == Token.Symbol.NEQ ||
				sym == Token.Symbol.LTH || sym == Token.Symbol.GTH ||
				sym == Token.Symbol.LEQ || sym == Token.Symbol.GEQ) {

			Token op = check(sym);
			AST.Expr right = parseAdditive();

			AST.BinExpr.Oper oper = switch (sym) {
				case EQU -> AST.BinExpr.Oper.EQU;
				case NEQ -> AST.BinExpr.Oper.NEQ;
				case LTH -> AST.BinExpr.Oper.LTH;
				case GTH -> AST.BinExpr.Oper.GTH;
				case LEQ -> AST.BinExpr.Oper.LEQ;
				case GEQ -> AST.BinExpr.Oper.GEQ;
				default -> throw new Report.InternalError();
			};

			AST.Expr node = new AST.BinExpr(oper, left, right);
			attrLoc.put(node, op);

			return node;
		}

		return left;
	}

	private AST.Expr parseAdditive() {

		AST.Expr left = parseMultiplicative();

		while (lexAn.peekToken().symbol() == Token.Symbol.ADD ||
				lexAn.peekToken().symbol() == Token.Symbol.SUB) {

			Token op = check(lexAn.peekToken().symbol());
			AST.Expr right = parseMultiplicative();

			AST.BinExpr.Oper oper =
					(op.symbol() == Token.Symbol.ADD)
							? AST.BinExpr.Oper.ADD
							: AST.BinExpr.Oper.SUB;

			AST.Expr node = new AST.BinExpr(oper, left, right);
			attrLoc.put(node, op);

			left = node;
		}

		return left;
	}

	private AST.Expr parseMultiplicative() {

		AST.Expr left = parsePrefix();

		while (lexAn.peekToken().symbol() == Token.Symbol.MUL ||
				lexAn.peekToken().symbol() == Token.Symbol.DIV ||
				lexAn.peekToken().symbol() == Token.Symbol.MOD) {

			Token op = check(lexAn.peekToken().symbol());
			AST.Expr right = parsePrefix();

			AST.BinExpr.Oper oper = switch (op.symbol()) {
				case MUL -> AST.BinExpr.Oper.MUL;
				case DIV -> AST.BinExpr.Oper.DIV;
				case MOD -> AST.BinExpr.Oper.MOD;
				default -> throw new Report.InternalError();
			};

			AST.Expr node = new AST.BinExpr(oper, left, right);
			attrLoc.put(node, op);

			left = node;
		}

		return left;
	}

	private AST.Expr parsePrefix() {


		// Support for prefix operators: ! + - ^
		if (lexAn.peekToken().symbol() == Token.Symbol.NOT ||
				lexAn.peekToken().symbol() == Token.Symbol.ADD ||
				lexAn.peekToken().symbol() == Token.Symbol.SUB ||
				lexAn.peekToken().symbol() == Token.Symbol.PTR) {
			Token.Symbol sym = lexAn.peekToken().symbol();
			if (sym == Token.Symbol.NOT || sym == Token.Symbol.ADD || sym == Token.Symbol.SUB || sym == Token.Symbol.PTR || sym.name().equals("POW") || sym.name().equals("CARET")) {
				Token op = check(sym);
				AST.Expr expr = parsePrefix();
				AST.UnExpr.Oper oper = switch (op.symbol()) {
					case NOT -> AST.UnExpr.Oper.NOT;
					case ADD -> AST.UnExpr.Oper.ADD;
					case SUB -> AST.UnExpr.Oper.SUB;
					case PTR -> AST.UnExpr.Oper.MEMADDR;
					default -> {
						throw new Report.InternalError();
					}
				};
				AST.Expr node = new AST.UnExpr(oper, expr);
				attrLoc.put(node, op);
				return node;
			}
		}

		return parsePostfix();
	}

	private AST.Expr parsePostfix() {
		AST.Expr expr = parsePrimaryExp();

		while (true) {
			Token.Symbol sym = lexAn.peekToken().symbol();
			if (sym == Token.Symbol.PTR) {
				Token op = check(Token.Symbol.PTR);
				AST.Expr node = new AST.UnExpr(AST.UnExpr.Oper.VALUEAT, expr);
				attrLoc.put(node, op);
				expr = node;
				continue;
			}
			break;
		}
		return expr;
	}

	//	PRIMARY
	private AST.Expr parsePrimaryExp() {

		Token.Symbol sym = lexAn.peekToken().symbol();

		if (sym == Token.Symbol.LPAREN) {
			check(Token.Symbol.LPAREN);
			AST.Expr expr = parseExpression();
			check(Token.Symbol.RPAREN);
			return expr;
		}

		else if (sym == Token.Symbol.INTCONST) {
			Token t = check(sym);
			AST.AtomExpr node =	new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, t.lexeme());
			attrLoc.put(node, t);

			return node;
		}

		else if (sym == Token.Symbol.CHARCONST) {
			Token t = check(sym);

			AST.AtomExpr node =	new AST.AtomExpr(AST.AtomExpr.Type.CHRCONST, t.lexeme());
			attrLoc.put(node, t);

			return node;
		}

		else if (sym == Token.Symbol.STRINGCONST) {
			Token t = check(sym);

			AST.AtomExpr node =	new AST.AtomExpr(AST.AtomExpr.Type.STRCONST, t.lexeme());
			attrLoc.put(node, t);

			return node;
		}

		else if (sym == Token.Symbol.IDENTIFIER) {
			Token id = check(sym);

			if (lexAn.peekToken().symbol() == Token.Symbol.LPAREN) {
				check(Token.Symbol.LPAREN);
				Vector<AST.Expr> args = parseArguments();
				check(Token.Symbol.RPAREN);

				AST.Expr node = new AST.CallExpr(id.lexeme(), args);
				attrLoc.put(node, id);

				return node;
			}

			AST.Expr node = new AST.VarExpr(id.lexeme());
			attrLoc.put(node, id);

			return node;
		}

		throw new Report.Error(lexAn.peekToken(), "Expected primary expression.");
	}

	private Vector<AST.Expr> parseArguments() {

		Vector<AST.Expr> args = new Vector<>();

		if (isExpressionStart(lexAn.peekToken().symbol())) {

			args.add(parseExpression());

			while (lexAn.peekToken().symbol() == Token.Symbol.COMMA) {
				check(Token.Symbol.COMMA);
				args.add(parseExpression());
			}
		}

		return args;
	}

	private List<AST.Init> parseInitializers() {

		List<AST.Init> list = new Vector<>();
		list.add(parseInitializer());

		while (lexAn.peekToken().symbol() == Token.Symbol.COMMA) {
			check(Token.Symbol.COMMA);
			list.add(parseInitializer());
		}

		return list;
	}

	private AST.Init parseInitializer() {
		Token t = lexAn.peekToken();
		AST.AtomExpr value = parseAtom();

		if (t.symbol() == Token.Symbol.MUL) {
			check(Token.Symbol.MUL);
			AST.AtomExpr num = parseAtom();
			attrLoc.put(num, t);

			AST.Init node = new AST.Init(num, value);
			attrLoc.put(node, t);

			return node;
		}

		AST.AtomExpr one = new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "1");
		attrLoc.put(one, t);
		AST.Init node = new AST.Init(one, value);
		attrLoc.put(node, t);

		return node;
	}

	private AST.AtomExpr parseAtom() {
		Token t = lexAn.peekToken();

		if (t.symbol() == Token.Symbol.INTCONST) {
			check(Token.Symbol.INTCONST);

			AST.AtomExpr node =	new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, t.lexeme());
			attrLoc.put(node, t);
			return node;
		}

		if (t.symbol() == Token.Symbol.CHARCONST) {
			check(Token.Symbol.CHARCONST);

			AST.AtomExpr node =	new AST.AtomExpr(AST.AtomExpr.Type.CHRCONST, t.lexeme());
			attrLoc.put(node, t);
			return node;
		}

		if (t.symbol() == Token.Symbol.STRINGCONST) {
			check(Token.Symbol.STRINGCONST);
			AST.AtomExpr node =	new AST.AtomExpr(AST.AtomExpr.Type.STRCONST, t.lexeme());
			attrLoc.put(node, t);
			return node;
		}

		throw new Report.Error(t, "Expected constant atom.");
	}

	/*
    //	------------------------ OLD CODE ----------------------------
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
        */
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


	// --- ZAGON ---

	/**
	 * Zagon sintaksnega analizatorja kot samostojnega programa.
	 *
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */

	public static void main(final String[] cmdLineArgs) {
		/*
		System.out.println("This is PINS'26 compiler (syntax analysis):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
//				Commented for the SynAn version
//				synAn.parse();
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
		 */
	}

}

