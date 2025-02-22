package com.thomasha.jlox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    /* <<========================= GENERAL FUNCTIONS ==========================>> */

    /**
     * Resolves a {@code Stmt}.
     * 
     * @param stmt the statement to be resolved
     */
    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    /**
     * Resolves an {@code Expr}.
     * 
     * @param expr the expression to be resolved
     */
    private void resolve(Expr expr) {
        expr.accept(this);
    }

    /**
     * Resolves a {@code List} of statements.
     * 
     * @param statements the {@code List} of statements to be resolved
     */
    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    /**
     * Creates a new scope by pushing a {@code Map} representing a block scope onto
     * the {@code Stack} of scopes.
     */
    private void beginScope() {
        scopes.push(new HashMap<String, Boolean>());
    }

    /**
     * Removes the top scope from {@link #scopes};
     */
    private void endScope() {
        scopes.pop();
    }

    /**
     * Adds the provided variable {@code name} to the innermost scope and marks its
     * value in the scope as {@code false}, indicating that its initializer has not
     * been resolved yet.
     * 
     * @param name the variable to declare
     */
    private void declare(Token name) {
        if (scopes.isEmpty())
            return;

        Map<String, Boolean> scope = scopes.peek();
        scope.put(name.lexeme, false);
    }

    /**
     * Sets the value of the provided variable {@code name} in the innermost scope
     * as {@code true}, indicating that its initializer has been resolved.
     * 
     * @param name the variable to mark as initialized
     */
    private void define(Token name) {
        if (scopes.isEmpty())
            return;
        scopes.peek().put(name.lexeme, true);
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    /* <<========================= STATEMENT VISITORS =========================>> */

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    /* <<======================== EXPRESSION VISITORS =========================>> */

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            Lox.error(expr.name, "Can't read local variable in its own initializer.");
        }

        resolveLocal(expr, expr.name);
        return null;
    }
}
