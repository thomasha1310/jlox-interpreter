package com.thomasha.jlox;

import static com.thomasha.jlox.TokenType.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
        keywords.put("break", BREAK);
    }

    /**
     * The source code as a {@code String}.
     */
    private final String source;

    /**
     * A list containing scanned tokens after {@link #scanTokens()} is run.
     */
    private final List<Token> tokens = new ArrayList<>();

    /**
     * The index of {@code source} corresponding to the first character of the
     * current lexeme such that {@code source.charAt(start)} is the first character
     * of the current lexeme.
     */
    private int start = 0;

    /**
     * The index of {@code source} corresponding to the character that is currently
     * being considered such that {@code source.charAt(start)} is the character in
     * the lexeme currently being evaluated.
     */
    private int current = 0;

    /**
     * The line of {@code source} that {@code current} is on. Used to create tokens
     * that know their location for error reporting.
     */
    private int line = 1;

    Scanner(String source) {
        this.source = source;
    }

    /**
     * Scans {@link #source} to create a {@code List} of parseable tokens.
     * 
     * @return a list of parseable tokens
     */
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

    /**
     * Scans a single token and adds it to {@link #tokens}.
     */
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
                switch (peek()) {
                    case '-':
                        addToken(MINUS_MINUS);
                        advance();
                        break;
                    case '=':
                        addToken(MINUS_EQUAL);
                        advance();
                        break;
                    default:
                        addToken(MINUS);
                        break;
                }
                break;
            case '+':
                switch (peek()) {
                    case '+':
                        addToken(PLUS_PLUS);
                        advance();
                        break;
                    case '=':
                        addToken(PLUS_EQUAL);
                        advance();
                        break;
                    default:
                        addToken(PLUS);
                        break;
                }
                break;
            case '*':
                addToken(match('=') ? STAR_EQUAL : STAR);
                break;
            case '%':
                addToken(match('=') ? PERCENT_EQUAL : PERCENT);
                break;

            case ';':
                addToken(SEMICOLON);
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
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;

            case '/':
                if (match('/')) {
                    // Comment found; advance past all characters until end of the line.
                    while (peek() != '\n' && !isAtEnd())
                        advance();
                } else if (match('=')) {
                    addToken(SLASH_EQUAL);
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

            case '"':
                string();
                break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character.");
                }
                break;
        }
    }

    /**
     * Handles scanning an alphanumeric token (which can be either a keyword or an
     * identifier). Advances until the next character of {@link #source} is no
     * longer alphanumeric, then extracts the value of the token and determines the
     * type of the token as either a keyword or an identifier. Adds the token to
     * {@link #tokens}.
     */
    private void identifier() {
        while (isAlphaNumeric(peek()))
            advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);

        if (type == null)
            // If the text does not match a keyword, it is an identifier.
            type = IDENTIFIER;

        addToken(type);
    }

    /**
     * Handles scanning a number by advancing until the next character of
     * {@link #source} is no longer a digit, then extracting the literal value using
     * the {@code substring} and {@code parseDouble} methods.
     * 
     * @see #isDigit(char)
     */
    private void number() {
        // Advance until a non-digit (0-9) character is found.
        while (isDigit(peek()))
            advance();

        // Check if there is a fractional part.
        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            // Continue advancing until the end of the fractional part is reached;
            while (isDigit(peek()))
                advance();
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    /**
     * Handles scanning a string once a quotation mark {@code "} has been found by
     * advancing until a closing quotation mark is found, then extracting the
     * literal value using the {@code substring} method. Calls {@code Lox.error} if
     * the string is unterminated, but does not throw a {@link RuntimeError} as an
     * unterminated string is a compile-time error.
     * 
     * @see Lox#run(String)
     * @see com.thomasha.jlox.Lox#error(Token, String)
     */
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n')
                line++;
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        advance();

        // Trim surrounding quotation marks.
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    /**
     * Checks if the {@link #current} character matches the {@code expected}
     * character. Increments {@code current} by 1 and returns {@code true} if and
     * only if the characters match. Otherwise, returns {@code false} (including if
     * {@link #isAtEnd()} is true) without incrementing {@code current}.
     * 
     * @param expected the character to match
     * @return {@code false} if {@link #isAtEnd()} or the characters do not match,
     *         and {@code true} if the characters match
     */
    private boolean match(char expected) {
        if (isAtEnd())
            return false;
        if (source.charAt(current) != expected)
            return false;
        current++;
        return true;
    }

    /**
     * Returns the value of {@link #source} at index {@code current}, but does not
     * change the value of {@link #current}. If {@code current} exceeds the length
     * of {@code source}, returns the null character {@code \0}.
     * 
     * @return the value of {@link #source} at index {@code current} or the null
     *         character {@code \0} if {@code current} exceeds {@code source}
     *         length
     */
    private char peek() {
        if (isAtEnd())
            return '\0';
        return source.charAt(current);
    }

    /**
     * Returns the value of {@link #source} at index {@code current + 1}, but does
     * not change the value of {@link #current}. If {@code current + 1} exceeds the
     * length of {@code source}, returns the null character {@code \0}.
     * 
     * @return the value of {@link #source} at index {@code current + 1} or the null
     *         character {@code \0} if {@code current + 1} exceeds {@code source}
     *         length
     */
    private char peekNext() {
        if (current + 1 >= source.length())
            return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    /**
     * Reads and returns the {@link #current} character, then increments
     * {@code current} by 1.
     * 
     * @return the character that was advanced past
     */
    private char advance() {
        return source.charAt(current++);
    }

    /**
     * Adds a {@link Token} to {@link #tokens} with {@link TokenType} of
     * {@code type} and a {@code null} {@code literal} value. Equivalent to calling
     * {@code addToken(type, null)}.
     * 
     * @param type type of token to add, usually not {@code IDENTIFIER},
     *             {@code STRING}, or {@code NUMBER}
     */
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    /**
     * Adds a {@link Token} to {@link #tokens} with {@link TokenType} of
     * {@code type}, a {@code lexeme} of {@code source.substring(start, current)}, a
     * {@code literal} value of {@code literal}, and a {@code line} number of
     * {@code line}.
     * 
     * @param type    type of token to add
     * @param literal value of an {@code IDENTIFIER}, {@code STRING}, or
     *                {@code NUMBER} type token, or {@code null} otherwise
     */
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}
