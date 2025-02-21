package com.thomasha.jlox;

import java.util.ArrayList;
import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    final Environment globals = new Environment();
    private Environment environment = globals;

    public Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<native fn>";
            }
        });
    }

    public void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    // <<============================ EXPRESSIONS =============================>> //

    /**
     * Evaluates an {@code Expr} object by having it accept this {@code Interpreter}
     * object. The expression then calls the corresponding {@code visit} method in
     * this {@code Interpreter} object.
     * 
     * @param expr the expression to be evaluated
     * @return the resulting evaluation as a Java Object
     * @see Expr
     */
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            // OR operator.

            // Return the evaluation of the left expression if it is truthy.
            // Essentially, returns "true" by returning a truthy value.
            // Right side is not evaluated due to short-circuiting.
            if (isTruthy(left))
                return left;
        } else {
            // AND operator.

            // Return the evaluation of the left expression if it is falsey.
            // Essentially, returns "false" by returning a falsey value.
            // Right side is not evaluated due to short-circuiting.
            if (!isTruthy(left)) {
                return left;
            }
        }

        // The logical expression is either (false or right) or (true and right);
        // therefore, the truthiness of the right expression is equivalent to the
        // truthiness of the logical expression.
        return evaluate(expr.right);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        // Evaluates the inside part of the grouping, essentially removing the left and
        // right parentheses.
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;
            case BANG:
                return !isTruthy(right);
            // Unreachable.
            default:
                return null;
        }
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case PLUS:
                if (left instanceof String || right instanceof String) {
                    return stringify(left) + stringify(right);
                }
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or include a string.");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if ((double) right != 0) {
                    return (double) left / (double) right;
                } else {
                    throw new RuntimeError(expr.operator,
                            "Cannot divide by zero.");
                }
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            case PERCENT:
                checkNumberOperands(expr.operator, left, right);
                if ((double) right != 0) {
                    return (double) left % (double) right;
                } else {
                    throw new RuntimeError(expr.operator,
                            "Cannot divide by zero.");
                }
            default:
                // Not a binary operator.
                throw new RuntimeError(expr.operator,
                        "The interpreter encountered a critical issue: non-binary operator evaluated as a binary operator.");
        }
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        LoxCallable function = (LoxCallable) callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren,
                    "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    // <<============================= STATEMENTS =============================>> //

    /**
     * Executes a {@code Stmt} object by having it accept this {@code Interpreter}
     * object. The statement then calls the corresponding {@code visit} method in
     * this {@code Interpreter} object.
     * 
     * @param stmt the statement to be executed
     * @see Stmt
     */
    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    public void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) {
            value = evaluate(stmt.value);
        }

        throw new Return(value);
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new Break();
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }

        return null;
    }

    /**
     * Evaluates the initializer (instance of {@code Expr}) of the
     * {@code Stmt.Var} object and sets the variable to the evaluated value. If
     * no initializer is provided, sets the variable to {@code nil}.
     * 
     * @param stmt the variable statement to be evaluated
     * @return a null instance of {@code java.lang.Void}
     */
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    /**
     * Executes a {@code while} loop.
     * 
     * @param stmt an instance of {@code Stmt.While} with a condition and body
     * @return a null instance of {@code java.lang.Void}
     */
    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        try {
            while (isTruthy(stmt.condition)) {
                execute(stmt.body);
            }
        } catch (Break breakException) {
        }
        return null;
    }

    // <<========================== HELPER FUNCTIONS ==========================>> //

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double)
            return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double)
            return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    /**
     * Returns false if and only if the object is {@code nil} or is an instance of
     * {@code Boolean} that evaluates to {@code false}. Otherwise, returns true,
     * including if the object is an integer equal to 0.
     * 
     * @param object the object to be evaluated
     * @return a boolean representing the truthiness of the specified object
     */
    private boolean isTruthy(Object object) {
        if (object == null)
            return false;
        if (object instanceof Boolean)
            return (boolean) object;
        return true;
    }

    /**
     * Returns false if one object is {@code nil} and the other is not. Returns true
     * if both objects are {@code nil}. Otherwise, defers to the
     * {@code java.lang.Object.equals} method.
     * 
     * @param a the first object to be compared
     * @param b the second object to be compared
     * @return a boolean representing whether the objects are equal
     * @see java.lang.Object#equals(Object)
     */
    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null)
            return true;
        if (a == null)
            return false;

        return a.equals(b);
    }

    /**
     * Converts a {@code java.lang.Object} representation of a Lox value into a
     * string following Lox conventions.
     * 
     * @param object a representation of a Lox value
     * @return a string conversion of the original object
     */
    private String stringify(Object object) {
        if (object == null)
            return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            // Handles numbers with integer values.
            if (text.endsWith(".0"))
                text = text.substring(0, text.length() - 2);
            return text;
        }

        return object.toString();
    }
}
