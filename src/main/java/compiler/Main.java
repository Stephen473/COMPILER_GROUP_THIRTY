package compiler;

import compiler.ast.AstPrinter;
import compiler.ast.Program;
import compiler.ir.IrGenerator;
import compiler.ir.IrInterpreter;
import compiler.ir.IrPrinter;
import compiler.ir.IrProgram;
import compiler.parser.ParseError;
import compiler.parser.Parser;
import compiler.scanner.Lexer;
import compiler.scanner.LexicalError;
import compiler.scanner.Token;
import compiler.semantic.Analyzer;
import compiler.semantic.SemanticError;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Compiler entry point for the Eyra language.
 *
 * Usage: java -jar compiler-group-30-1.0-SNAPSHOT.jar [--tokens | --ast | --run] path/to/program.ey
 *
 * Phases run in order and the compiler stops after the first phase that finds errors:
 * scanner, parser, semantic analyzer, intermediate code. What is printed:
 *   (no flag)   the intermediate code
 *   --tokens    the token list (scanner only)
 *   --ast       the syntax tree (scanner and parser)
 *   --run       the output of the program, run by the IR interpreter
 *
 * Exit codes: 0 success, 64 wrong usage, 65 the program has errors, 66 file not readable,
 * 70 the program failed while running.
 */
public class Main {

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /** The whole command-line program. It returns the exit code instead of exiting, so tests can call it. */
    static int run(String[] args, PrintStream out, PrintStream err) {
        String mode = "--ir";
        if (args.length == 2 && List.of("--tokens", "--ast", "--run").contains(args[0])) {
            mode = args[0];
        } else if (args.length != 1 || args[0].startsWith("--")) {
            err.println("Usage: java -jar compiler-group-30-1.0-SNAPSHOT.jar [--tokens | --ast | --run] <source-file.ey>");
            return 64;
        }
        String path = args[args.length - 1];

        String source;
        try {
            source = Files.readString(Path.of(path));
        } catch (IOException e) {
            err.println("Cannot read file '" + path + "': " + e.getMessage());
            return 66;
        }

        // Phase 1: scan
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        if (lexer.hasErrors()) {
            for (LexicalError error : lexer.getErrors()) {
                err.println(error);
            }
            return failed(err, lexer.getErrors().size(), "lexical");
        }
        if (mode.equals("--tokens")) {
            tokens.forEach(out::println);
            return 0;
        }

        // Phase 2: parse
        Parser parser = new Parser(tokens);
        Program program = parser.parse();
        if (parser.hasErrors()) {
            for (ParseError error : parser.getErrors()) {
                err.println(error);
            }
            return failed(err, parser.getErrors().size(), "syntax");
        }
        if (mode.equals("--ast")) {
            out.print(AstPrinter.print(program));
            return 0;
        }

        // Phase 3: check the meaning, then generate intermediate code
        Analyzer analyzer = new Analyzer();
        analyzer.analyze(program);
        if (analyzer.hasErrors()) {
            for (SemanticError error : analyzer.getErrors()) {
                err.println(error);
            }
            return failed(err, analyzer.getErrors().size(), "semantic");
        }
        IrProgram ir = new IrGenerator(program, analyzer).generate();
        if (mode.equals("--run")) {
            try {
                new IrInterpreter(ir).run(out);
            } catch (IrInterpreter.RuntimeError e) {
                err.println("Runtime error: " + e.getMessage());
                return 70;
            }
            return 0;
        }
        out.print(IrPrinter.print(ir));
        return 0;
    }

    private static int failed(PrintStream err, int count, String kind) {
        err.println(count + " " + kind + " error" + (count == 1 ? "" : "s") + " found. Compilation stopped.");
        return 65;
    }
}
