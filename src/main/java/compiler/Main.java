package compiler;

import compiler.ast.AstPrinter;
import compiler.ast.Program;
import compiler.parser.ParseError;
import compiler.parser.Parser;
import compiler.scanner.Lexer;
import compiler.scanner.LexicalError;
import compiler.scanner.Token;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Compiler entry point for the Eyra language.
 *
 * Phase 1 added the scanner and Phase 2 added the parser. For now the compiler
 * scans the file, parses it, and prints the syntax tree (or every error it found).
 * Later phases will extend this to analyze, generate code and optimize.
 *
 * Usage: java -jar compiler-group-30-1.0-SNAPSHOT.jar [--tokens] path/to/program.ey
 *
 * Exit codes: 0 success, 64 wrong usage, 65 the program has errors, 66 file not readable.
 */
public class Main {

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /** The whole command-line program. It returns the exit code instead of exiting, so tests can call it. */
    static int run(String[] args, PrintStream out, PrintStream err) {
        boolean tokensOnly = args.length == 2 && args[0].equals("--tokens");
        if (args.length != 1 && !tokensOnly) {
            err.println("Usage: java -jar compiler-group-30-1.0-SNAPSHOT.jar [--tokens] <source-file.ey>");
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
            int n = lexer.getErrors().size();
            err.println(n + " lexical error" + (n == 1 ? "" : "s") + " found. Compilation stopped.");
            return 65;
        }
        if (tokensOnly) {
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
            int n = parser.getErrors().size();
            err.println(n + " syntax error" + (n == 1 ? "" : "s") + " found. Compilation stopped.");
            return 65;
        }
        out.print(AstPrinter.print(program));
        return 0;
    }
}
