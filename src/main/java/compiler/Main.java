package compiler;

import compiler.scanner.Lexer;
import compiler.scanner.LexicalError;
import compiler.scanner.Token;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Compiler entry point for the Eyra language.
 * Phase 1 only runs the scanner and prints the tokens (or every lexical error).
 * Later phases will extend this to parse, analyze, generate code and optimize.
 *
 * Usage: java -jar compiler-group-30-1.0-SNAPSHOT.jar path/to/program.ey
 */
public class Main {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java -jar compiler-group-30-1.0-SNAPSHOT.jar <source-file.ey>");
            System.exit(64);
        }

        String source;
        try {
            source = Files.readString(Path.of(args[0]));
        } catch (IOException e) {
            System.err.println("Cannot read file '" + args[0] + "': " + e.getMessage());
            System.exit(66);
            return;
        }

        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();

        if (lexer.hasErrors()) {
            for (LexicalError error : lexer.getErrors()) {
                System.err.println(error);
            }
            int n = lexer.getErrors().size();
            System.err.println(n + " lexical error" + (n == 1 ? "" : "s") + " found. Compilation stopped.");
            System.exit(65);
        }

        tokens.forEach(System.out::println);
    }
}
