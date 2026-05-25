package pins26.phase;

import java.util.*;

import pins26.common.*;

/**
 * Generiranje kode.
 */
public class CodeGen {

	@SuppressWarnings({ "doclint:missing" })
	public CodeGen() {
		throw new Report.InternalError();
	}

	/**
	 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
	 * predstavitve.
	 * 
	 * Atributi:
	 * <ol>
	 * <li>({@link Abstr}) lokacija kode, ki pripada posameznemu vozliscu;</li>
	 * <li>({@link SemAn}) definicija uporabljenega imena;</li>
	 * <li>({@link SemAn}) ali je dani izraz levi izraz;</li>
	 * <li>({@link Memory}) klicni zapis funkcije;</li>
	 * <li>({@link Memory}) dostop do parametra;</li>
	 * <li>({@link Memory}) dostop do spremenljivke;</li>
	 * <li>({@link CodeGen}) seznam ukazov, ki predstavljajo kodo programa;</li>
	 * <li>({@link CodeGen}) seznam ukazov, ki predstavljajo podatke programa.</li>
	 * </ol>
	 */
	public static class AttrAST extends Memory.AttrAST {

		/** Atribut: seznam ukazov, ki predstavljajo kodo programa. */
		public final Map<AST.Node, List<PDM.CodeInstr>> attrCode;

		/** Atribut: seznam ukazov, ki predstavljajo podatke programa. */
		public final Map<AST.Node, List<PDM.DataInstr>> attrData;

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi generiranja kode.
		 * 
		 * @param attrAST  Abstraktno sintaksno drevo z dodanimi atributi pomnilniske
		 *                 predstavitve.
		 * @param attrCode Attribut: seznam ukazov, ki predstavljajo kodo programa.
		 * @param attrData Attribut: seznam ukazov, ki predstavljajo podatke programa.
		 */
		public AttrAST(final Memory.AttrAST attrAST, final Map<AST.Node, List<PDM.CodeInstr>> attrCode,
				final Map<AST.Node, List<PDM.DataInstr>> attrData) {
			super(attrAST);
			this.attrCode = attrCode;
			this.attrData = attrData;
		}

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi generiranja kode.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi generiranja
		 *                kode.
		 */
		public AttrAST(final AttrAST attrAST) {
			super(attrAST);
			this.attrCode = attrAST.attrCode;
			this.attrData = attrAST.attrData;
		}

		@Override
		public String head(final AST.Node node, final boolean highlighted) {
			final StringBuffer head = new StringBuffer();
			head.append(super.head(node, false));
			return head.toString();
		}

		@Override
		public void desc(final int indent, final AST.Node node, final boolean highlighted) {
			super.desc(indent, node, false);
			System.out.print(highlighted ? "\033[31m" : "");
			if (attrCode.get(node) != null) {
				List<PDM.CodeInstr> instrs = attrCode.get(node);
				if (instrs != null) {
					if (indent > 0)
						System.out.printf("%" + indent + "c", ' ');
					System.out.printf("--- Code: ---\n");
					for (final PDM.CodeInstr instr : instrs) {
						if (indent > 0)
							System.out.printf("%" + indent + "c", ' ');
						System.out.println((instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
					}
				}
			}
			if (attrData.get(node) != null) {
				List<PDM.DataInstr> instrs = attrData.get(node);
				if (instrs != null) {
					if (indent > 0)
						System.out.printf("%" + indent + "c", ' ');
					System.out.printf("--- Data: ---\n");
					for (final PDM.DataInstr instr : instrs) {
						if (indent > 0)
							System.out.printf("%" + indent + "c", ' ');
						System.out.println((instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
					}
				}
			}
			System.out.print(highlighted ? "\033[30m" : "");
			return;
		}

	}

	/**
	 * Izracuna kodo programa
	 * 
	 * @param memoryAttrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
	 *                      pomnilniske predstavitve.
	 * @return Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
	 *         predstavitve.
	 */
	public static AttrAST generate(final Memory.AttrAST memoryAttrAST) {
		AttrAST attrAST = new AttrAST(memoryAttrAST, new HashMap<AST.Node, List<PDM.CodeInstr>>(),
				new HashMap<AST.Node, List<PDM.DataInstr>>());
		(new CodeGenerator(attrAST)).generate();
		return attrAST;
	}

	/**
	 * Generiranje kode v abstraktnem sintaksnem drevesu.
	 */
	private static class CodeGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Stevec anonimnih label. */
		private int labelCounter = 0;

		/**
		 * Ustvari nov generator kode v abstraktnem sintaksnem drevesu.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public CodeGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Sprozi generiranje kode v abstraktnem sintaksnem drevesu.
		 * 
		 * @return Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 *         predstavitve.
		 */
		public AttrAST generate() {
			attrAST.ast.accept(new Generator(), null);
			return new AttrAST(attrAST, Collections.unmodifiableMap(attrAST.attrCode),
					Collections.unmodifiableMap(attrAST.attrData));
		}

		/** Obiskovalec, ki generira kodo v abstraktnem sintaksnem drevesu. */
		private class Generator implements AST.FullVisitor<List<PDM.CodeInstr>, Mem.Frame> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {}

			@Override
			public List<PDM.CodeInstr> visit(AST.Nodes<? extends AST.Node> nodes, Mem.Frame frame) {
				List<PDM.CodeInstr> code = new Vector<>();
				for (AST.Node node : nodes) {
					List<PDM.CodeInstr> c = node.accept(this, frame);
					if (c != null) code.addAll(c);
				}
				attrAST.attrCode.put(nodes, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.FunDef funDef, Mem.Frame frame) {
				List<PDM.CodeInstr> code = new Vector<>();
				Mem.Frame myFrame = attrAST.attrFrame.get(funDef);

				// Label for function entry
				code.add(new PDM.LABEL(funDef.name, attrAST.attrLoc.get(funDef)));

				// Allocate space for locals (varsSize)
				if (myFrame.varsSize > 0) {
					code.add(new PDM.PUSH(-myFrame.varsSize,attrAST.attrLoc.get(funDef)));

					code.add(new PDM.POPN(attrAST.attrLoc.get(funDef)));
				}

				// Generate code for statements
				code.addAll(funDef.stmts.accept(this,myFrame));

				// default result
				code.add(new PDM.PUSH(0,attrAST.attrLoc.get(funDef)));

				// parsSize for RETN
				code.add(new PDM.PUSH(myFrame.parsSize,attrAST.attrLoc.get(funDef)));

				code.add(new PDM.RETN(myFrame,attrAST.attrLoc.get(funDef)));

				attrAST.attrCode.put(funDef, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.VarDef varDef, Mem.Frame frame) {
				List<PDM.CodeInstr> code = new Vector<>();
				Mem.Access access = attrAST.attrVarAccess.get(varDef);

				if (access instanceof Mem.AbsAccess abs) {
					List<PDM.DataInstr> data = new Vector<>();

					data.add(new PDM.LABEL(abs.name, attrAST.attrLoc.get(varDef)));

					if (abs.inits != null && !abs.inits.isEmpty()) {

						for (int value : abs.inits) {
							data.add(new PDM.DATA(value, attrAST.attrLoc.get(varDef)));
						}

						int words = abs.size / 4;
						for (int i = abs.inits == null ? 0 : abs.inits.size(); i < words; i++) {
							data.add(new PDM.DATA(0, attrAST.attrLoc.get(varDef)));
						}

					} else {
						data.add(new PDM.SIZE(abs.size,attrAST.attrLoc.get(varDef)));
					}

					attrAST.attrData.put(varDef, data);
				}
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.ExprStmt exprStmt, Mem.Frame frame) {
				List<PDM.CodeInstr> code = exprStmt.expr.accept(this, frame);
				// Remove result from stack
				code.add(new PDM.PUSH(4,attrAST.attrLoc.get(exprStmt)));
				code.add(new PDM.POPN(attrAST.attrLoc.get(exprStmt)));

				attrAST.attrCode.put(exprStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.AssignStmt assignStmt, Mem.Frame frame) {
				List<PDM.CodeInstr> code = new Vector<>();

				// PINS'26: najprej naslov
				code.addAll(genLValue(assignStmt.dstExpr, frame));

				// potem vrednost
				code.addAll(assignStmt.srcExpr.accept(this, frame));

				code.add(new PDM.SAVE(attrAST.attrLoc.get(assignStmt)));

				attrAST.attrCode.put(assignStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.IfStmt ifStmt, Mem.Frame frame) {
				List<PDM.CodeInstr> code = new Vector<>();
				String thenLabel = "__then" + (labelCounter++);
				String elseLabel = "__else" + (labelCounter++);
				String endLabel = "__endif" + (labelCounter++);

				// Condition
				code.addAll(ifStmt.cond.accept(this, frame));
				code.add(new PDM.NAME(thenLabel, attrAST.attrLoc.get(ifStmt)));
				code.add(new PDM.NAME(elseLabel, attrAST.attrLoc.get(ifStmt)));
				code.add(new PDM.CJMP(attrAST.attrLoc.get(ifStmt)));

				// Then branch
				code.add(new PDM.LABEL(thenLabel, attrAST.attrLoc.get(ifStmt)));
				code.addAll(ifStmt.thenStmts.accept(this, frame));
				code.add(new PDM.NAME(endLabel, attrAST.attrLoc.get(ifStmt)));
				code.add(new PDM.UJMP(attrAST.attrLoc.get(ifStmt)));

				// Else branch
				code.add(new PDM.LABEL(elseLabel, attrAST.attrLoc.get(ifStmt)));
				code.addAll(ifStmt.elseStmts.accept(this, frame));

				// End label
				code.add(new PDM.LABEL(endLabel, attrAST.attrLoc.get(ifStmt)));

				attrAST.attrCode.put(ifStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.WhileStmt whileStmt, Mem.Frame frame) {
				List<PDM.CodeInstr> code = new Vector<>();
				String condLabel = "__loop_cond" + (labelCounter++);
				String bodyLabel = "__loop_body" + (labelCounter++);
				String endLabel = "__loop_end" + (labelCounter++);

				// Condition at top
				code.add(new PDM.LABEL(condLabel, attrAST.attrLoc.get(whileStmt)));
				code.addAll(whileStmt.cond.accept(this, frame));
				code.add(new PDM.NAME(bodyLabel, attrAST.attrLoc.get(whileStmt)));
				code.add(new PDM.NAME(endLabel, attrAST.attrLoc.get(whileStmt)));
				code.add(new PDM.CJMP(attrAST.attrLoc.get(whileStmt)));

				// Body
				code.add(new PDM.LABEL(bodyLabel, attrAST.attrLoc.get(whileStmt)));
				code.addAll(whileStmt.stmts.accept(this, frame));
				code.add(new PDM.NAME(condLabel, attrAST.attrLoc.get(whileStmt)));
				code.add(new PDM.UJMP(attrAST.attrLoc.get(whileStmt)));

				// End
				code.add(new PDM.LABEL(endLabel, attrAST.attrLoc.get(whileStmt)));

				attrAST.attrCode.put(whileStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.LetStmt letStmt, Mem.Frame frame) {
				List<PDM.CodeInstr> code = new Vector<>();
				// Generate code for local definitions (variables)
				for (AST.MainDef def : letStmt.defs) {
					code.addAll(def.accept(this, frame));
				}
				// Generate code for statements
				code.addAll(letStmt.stmts.accept(this, frame));
				attrAST.attrCode.put(letStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.AtomExpr atomExpr, Mem.Frame frame) {
				List<PDM.CodeInstr> code = new Vector<>();
				int value = 0;
				switch (atomExpr.type) {
					case INTCONST -> {
						value = Memory.decodeIntConst(atomExpr, attrAST.attrLoc.get(atomExpr));
						code.add(new PDM.PUSH(value, attrAST.attrLoc.get(atomExpr)));
					}
					case CHRCONST -> {
						value = Memory.decodeChrConst(atomExpr, attrAST.attrLoc.get(atomExpr));
						code.add(new PDM.PUSH(value, attrAST.attrLoc.get(atomExpr)));
					}
					case STRCONST -> {
						String label = "__str" + (labelCounter++);
						List<PDM.DataInstr> data = new Vector<>();
						data.add(new PDM.LABEL(label, attrAST.attrLoc.get(atomExpr)));
						for (int v : Memory.decodeStrConst(atomExpr, attrAST.attrLoc.get(atomExpr)))
							data.add(new PDM.DATA(v, attrAST.attrLoc.get(atomExpr)));
						data.add(new PDM.DATA(0, attrAST.attrLoc.get(atomExpr)));
						attrAST.attrData.put(atomExpr, data);
						code.add(new PDM.NAME(label, attrAST.attrLoc.get(atomExpr)));
						attrAST.attrCode.put(atomExpr, code);
						return code;
					}
				}
				attrAST.attrCode.put(atomExpr, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.UnExpr unExpr, Mem.Frame frame) {
				List<PDM.CodeInstr> code = new Vector<>();
				switch (unExpr.oper) {
					case NOT -> {
						code.addAll(unExpr.expr.accept(this, frame));
						code.add(new PDM.OPER(PDM.OPER.Oper.NOT, attrAST.attrLoc.get(unExpr)));
					}
					case ADD -> {
						code.addAll(unExpr.expr.accept(this, frame));
					}
					case SUB -> {
						code.addAll(unExpr.expr.accept(this, frame));
						code.add(new PDM.OPER(PDM.OPER.Oper.NEG, attrAST.attrLoc.get(unExpr)));
					}
					case MEMADDR -> {
						code.addAll(genLValue(unExpr.expr, frame));
					}
					case VALUEAT -> {
						code.addAll(genLValue(unExpr.expr, frame));
						code.add(new PDM.LOAD(attrAST.attrLoc.get(unExpr)));
					}
				}
				attrAST.attrCode.put(unExpr, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.BinExpr binExpr, Mem.Frame frame) {
				List<PDM.CodeInstr> code = new Vector<>();
				code.addAll(binExpr.fstExpr.accept(this, frame));
				code.addAll(binExpr.sndExpr.accept(this, frame));
				PDM.OPER.Oper oper = switch (binExpr.oper) {
					case OR -> PDM.OPER.Oper.OR;
					case AND -> PDM.OPER.Oper.AND;
					case EQU -> PDM.OPER.Oper.EQU;
					case NEQ -> PDM.OPER.Oper.NEQ;
					case GTH -> PDM.OPER.Oper.GTH;
					case LTH -> PDM.OPER.Oper.LTH;
					case GEQ -> PDM.OPER.Oper.GEQ;
					case LEQ -> PDM.OPER.Oper.LEQ;
					case ADD -> PDM.OPER.Oper.ADD;
					case SUB -> PDM.OPER.Oper.SUB;
					case MUL -> PDM.OPER.Oper.MUL;
					case DIV -> PDM.OPER.Oper.DIV;
					case MOD -> PDM.OPER.Oper.MOD;
				};
				code.add(new PDM.OPER(oper, attrAST.attrLoc.get(binExpr)));
				attrAST.attrCode.put(binExpr, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.VarExpr varExpr, Mem.Frame frame) {
				List<PDM.CodeInstr> code = new Vector<>();
				Mem.Access access = attrAST.attrVarAccess.get(attrAST.attrDef.get(varExpr));
				if (access instanceof Mem.AbsAccess abs) {
					code.add(new PDM.NAME(abs.name, attrAST.attrLoc.get(varExpr)));
					code.add(new PDM.LOAD(attrAST.attrLoc.get(varExpr)));
				} else if (access instanceof Mem.RelAccess rel) {
					code.add(new PDM.REGN(PDM.REGN.Reg.FP,attrAST.attrLoc.get(varExpr)));
					code.add(new PDM.PUSH(rel.offset,attrAST.attrLoc.get(varExpr)));
					code.add(new PDM.OPER(PDM.OPER.Oper.ADD,attrAST.attrLoc.get(varExpr)));

					code.add(new PDM.LOAD(attrAST.attrLoc.get(varExpr)));
				}
				attrAST.attrCode.put(varExpr, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(AST.CallExpr callExpr, Mem.Frame frame) {
				List<PDM.CodeInstr> code = new Vector<>();
				AST.FunDef funDef = (AST.FunDef) attrAST.attrDef.get(callExpr);

				List<AST.Expr> args = callExpr.args.getAll();
				for (int i = 0; i < args.size(); i++) {
					code.addAll(args.get(i).accept(this, frame));
				}

				code.add(new PDM.PUSH(0, attrAST.attrLoc.get(callExpr)));

				// Push function address
				code.add(new PDM.NAME(funDef.name, attrAST.attrLoc.get(callExpr)));

				// Call
				code.add(new PDM.CALL(attrAST.attrFrame.get(funDef), attrAST.attrLoc.get(callExpr)));

				attrAST.attrCode.put(callExpr, code);
				return code;
			}

			private List<PDM.CodeInstr> genLValue(AST.Expr expr, Mem.Frame frame) {
				List<PDM.CodeInstr> code = new Vector<>();
				if (expr instanceof AST.VarExpr varExpr) {
					Mem.Access access = attrAST.attrVarAccess.get(attrAST.attrDef.get(varExpr));
					if (access instanceof Mem.AbsAccess abs) {
						code.add(new PDM.NAME(abs.name, attrAST.attrLoc.get(varExpr)));
					} else if (access instanceof Mem.RelAccess rel) {
						code.add(new PDM.REGN(PDM.REGN.Reg.FP,attrAST.attrLoc.get(varExpr)));
						code.add(new PDM.PUSH(rel.offset,attrAST.attrLoc.get(varExpr)));
						code.add(new PDM.OPER(PDM.OPER.Oper.ADD,attrAST.attrLoc.get(varExpr)));
					}
				} else if (expr instanceof AST.UnExpr unExpr && unExpr.oper == AST.UnExpr.Oper.VALUEAT) {
					code.addAll(unExpr.expr.accept(this, frame));
				} else {
					throw new Report.InternalError();
				}
				return code;
			}
		}

	}

	/**
	 * Generator seznama ukazov, ki predstavljajo kodo programa.
	 */
	public static class CodeSegmentGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Seznam ukazov za inicializacijo staticnih spremenljivk. */
		private final Vector<PDM.CodeInstr> codeInitSegment = new Vector<PDM.CodeInstr>();

		/** Seznam ukazov funkcij. */
		private final Vector<PDM.CodeInstr> codeFunsSegment = new Vector<PDM.CodeInstr>();

		/** Klicni zapis funkcije {@code main}. */
		private Mem.Frame main = null;

		/**
		 * Ustvari nov generator seznama ukazov, ki predstavljajo kodo programa.
		 *
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public CodeSegmentGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Izracuna seznam ukazov, ki predstavljajo kodo programa.
		 * 
		 * @return Seznam ukazov, ki predstavljajo kodo programa.
		 */
		public List<PDM.CodeInstr> codeSegment() {
			attrAST.ast.accept(new Generator(), null);
			codeInitSegment.addLast(new PDM.PUSH(0, null));
			codeInitSegment.addLast(new PDM.NAME("main", null));
			codeInitSegment.addLast(new PDM.CALL(main, null));
			codeInitSegment.addLast(new PDM.PUSH(0, null));
			codeInitSegment.addLast(new PDM.NAME("exit", null));
			codeInitSegment.addLast(new PDM.CALL(null, null));
			final Vector<PDM.CodeInstr> codeSegment = new Vector<PDM.CodeInstr>();
			codeSegment.addAll(codeInitSegment);
			codeSegment.addAll(codeFunsSegment);
			return Collections.unmodifiableList(codeSegment);
		}

		/**
		 * Obiskovalec, ki izracuna seznam ukazov, ki predstavljajo kodo programa.
		 */
		private class Generator implements AST.FullVisitor<Object, Object> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public Object visit(final AST.FunDef funDef, final Object arg) {
				if (funDef.stmts.size() == 0)
					return null;
				List<PDM.CodeInstr> code = attrAST.attrCode.get(funDef);
				codeFunsSegment.addAll(code);
				funDef.pars.accept(this, arg);
				funDef.stmts.accept(this, arg);
				switch (funDef.name) {
				case "main" -> main = attrAST.attrFrame.get(funDef);
				}
				return null;
			}

			@Override
			public Object visit(final AST.VarDef varDef, final Object arg) {
				switch (attrAST.attrVarAccess.get(varDef)) {
				case Mem.AbsAccess __: {
					List<PDM.CodeInstr> code = attrAST.attrCode.get(varDef);

					if (code != null && !code.isEmpty())
						codeInitSegment.addAll(code);

					break;
				}
				case Mem.RelAccess __: {
					break;
				}
				default:
					throw new Report.InternalError();
				}
				return null;
			}

		}

	}

	/**
	 * Generator seznama ukazov, ki predstavljajo podatke programa.
	 */
	public static class DataSegmentGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Seznam ukazov, ki predstavljajo podatke programa. */
		private final Vector<PDM.DataInstr> dataSegment = new Vector<PDM.DataInstr>();

		/**
		 * Ustvari nov generator seznama ukazov, ki predstavljajo podatke programa.
		 *
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public DataSegmentGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Izracuna seznam ukazov, ki predstavljajo podatke programa.
		 * 
		 * @return Seznam ukazov, ki predstavljajo podatke programa.
		 */
		public List<PDM.DataInstr> dataSegment() {
			attrAST.ast.accept(new Generator(), null);
			return Collections.unmodifiableList(dataSegment);
		}

		/**
		 * Obiskovalec, ki izracuna seznam ukazov, ki predstavljajo podatke programa.
		 */
		private class Generator implements AST.FullVisitor<Object, Object> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public Object visit(final AST.VarDef varDef, final Object arg) {
				List<PDM.DataInstr> data = attrAST.attrData.get(varDef);
				if (data != null)
					dataSegment.addAll(data);
				varDef.inits.accept(this, arg);
				return null;
			}

			@Override
			public Object visit(final AST.AtomExpr atomExpr, final Object arg) {
				List<PDM.DataInstr> data = attrAST.attrData.get(atomExpr);
				if (data != null)
					dataSegment.addAll(data);
				return null;
			}

		}

	}

	// --- ZAGON ---

	/**
	 * Zagon izracuna pomnilniske predstavitve kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'26 compiler (code generation):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
				// abstraktna sintaksa:
				final Abstr.AttrAST abstrAttrAST = Abstr.constructAST(synAn);
				// semanticna analiza:
				final SemAn.AttrAST semanAttrAST = SemAn.analyze(abstrAttrAST);
				// pomnilniska predstavitev:
				final Memory.AttrAST memoryAttrAST = Memory.organize(semanAttrAST);
				// generiranje kode:
				final CodeGen.AttrAST codegenAttrAST = CodeGen.generate(memoryAttrAST);

				(new AST.Logger(codegenAttrAST)).log();
				{
					int addr = 0;
					final List<PDM.CodeInstr> codeSegment = (new CodeSegmentGenerator(codegenAttrAST)).codeSegment();
					{
						System.out.println("\n\033[1mCODE SEGMENT:\033[0m");
						for (final PDM.CodeInstr instr : codeSegment) {
							System.out.printf("%8d [%s] %s\n", addr, instr.size(),
									(instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
							addr += instr.size();
						}
					}
					final List<PDM.DataInstr> dataSegment = (new DataSegmentGenerator(codegenAttrAST)).dataSegment();
					{
						System.out.println("\n\033[1mDATA SEGMENT:\033[0m");
						for (final PDM.DataInstr instr : dataSegment) {
							System.out.printf("%8d [%s] %s\n", addr, (instr instanceof PDM.SIZE) ? " " : instr.size(),
									(instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
							addr += instr.size();
						}
					}
					System.out.println();
				}
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
