package com.thomasha.jlox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate an error in the exit code.
        if (hadError)
            System.exit(65);
        if (hadRuntimeError)
            System.exit(70);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null)
                break;
            run(line);

            // Resets error flag so user can continue running code.
            hadError = false;
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Stop if there was a syntax error.
        if (hadError)
            return;

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        // Stop if there was a resolution error.
        if (hadError)
            return;

        interpreter.interpret(statements);
    }

    /**
     * Reports, but does not throw, an error by printing the error information to
     * {@code System.err}.
     * 
     * @param line    the line where the error occurred
     * @param message the error message to be reported
     */
    static void error(int line, String message) {
        report(line, "", message);
    }

    /**
     * Prints an error message to {@code System.err} and marks {@link #hadError} as
     * {@code true}.
     * 
     * @param line    the line where the error occurred
     * @param where   the context of the error (such as the specific erroneous
     *                syntax or symbol)
     * @param message the error message
     */
    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    /**
     * Reports, but does not throw, an error by printing the error information to
     * {@code System.err}. Provides context using the given {@code Token}.
     * 
     * @param token   the {@code Token} where the error occurred
     * @param message the error message to be reported
     */
    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static void runtimeError(RuntimeError error) {
        System.err.println("RuntimeError [line " + error.token.line + "]: " + error.getMessage());
        hadRuntimeError = true;
    }
}
