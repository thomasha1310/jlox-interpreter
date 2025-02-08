package com.thomasha;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thomasha.TokenType.*;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    // First character in the lexeme being scanned; index is absolute with respect
    // to `source`. That is, if `start` is 5, then the first character of the lexeme
    // is at source.charAt(5).
    private int start = 0;

    // Character in the lexeme that is currently being considered; index is absolute
    // with respect to `source`.
    private int current = 0;

    // Traces which source line `current` is on to create tokens that know their
    // location (for error reporting).
    private int line = 1;

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }

        // Appends an EOF token.
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(':
                addToken(LEFT_PAREN);
                break;
            case ')':
                addToken(RIGHT_PAREN);
                break;
            case '{':
                addToken(LEFT_BRACE);
                break;
            case '}':
                addToken(RIGHT_BRACE);
                break;
            case ',':
                addToken(COMMA);
                break;
            case '.':
                addToken(DOT);
                break;
            case '-':
                addToken(MINUS);
                break;
            case '+':
                addToken(PLUS);
                break;
            case ';':
                addToken(SEMICOLON);
                break;
            case '*':
                addToken(STAR);
                break;

            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '?':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;

            case '/':
                if (match('/')) {
                    // Comment found; advance past all characters until end of the line.
                    while (peek() != '\n' && !isAtEnd())
                        advance();
                } else {
                    addToken(SLASH);
                }
                break;

            // Ignore whitespace.
            case ' ':
            case '\r':
            case '\t':
                break;

            case '\n':
                line++;
                break;

            default:
                Lox.error(line, "Unexpected character.");
                break;
        }
    }

    private boolean match(char expected) {
        if (isAtEnd())
            return false;
        if (source.charAt(current) != expected)
            return false;
        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd())
            return '\0';
        return source.charAt(current);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    // Reads and returns the current character, then moves `current` forward.
    private char advance() {
        return source.charAt(current++);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}
