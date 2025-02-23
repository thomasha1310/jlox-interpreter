package com.thomasha.jlox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    /**
     * Initializes an Environment object for the global scope environment.
     */
    Environment() {
        enclosing = null;
    }

    /**
     * Initializes an Environment object for a local scope environment.
     * 
     * @param enclosing the {@code environment} that encloses the new
     *                  {@code environment}
     */
    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    /**
     * Returns the value associated with the given variable name. Throws a
     * {@code RuntimeError} if the variable has not been defined.
     * 
     * @param name the variable to retrieve the value of
     * @return the value associated with the variable
     * @throws RuntimeError if the specified variable has not been defined
     */
    public Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        // If the variable is not found in the current scope, check the enclosing scope.
        // The process continues until the variable is found or the global scope is
        // reached (at which point a RuntimeError is thrown).
        if (enclosing != null) {
            return enclosing.get(name);
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    /**
     * Sets the value of the specified variable, overriding the previous value if
     * one existed.
     * 
     * @param name  the name of the variable
     * @param value the value to be associated with the variable
     */
    public void define(String name, Object value) {
        values.put(name, value);
    }

    public Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    private Environment ancestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment.enclosing;
        }

        return environment;
    }

    /**
     * Sets the value of the specified variable if and only if the variable has
     * already been defined. If the variable has not been defined, throws a
     * {@code RuntimeError}, similar to {@link #get}.
     * 
     * @param name  the variable to assign a value to
     * @param value the value to assign to the specified variable
     * @throws RuntimeError if the specified variable has not been defined
     */
    public void assign(Token name, Object value) {
        // If the variable is in the current scope, assign it and exit.
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        // If the variable is not found in the current scope, check the enclosing scope.
        // The process continues until the variable is found or the global scope is
        // reached (at which point a RuntimeError is thrown).
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    public void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }

    private Environment ancestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment.enclosing;
        }

        return environment;
    }
}
