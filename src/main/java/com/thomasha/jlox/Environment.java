package com.thomasha.jlox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    // Environment initializer for global scope environment.
    Environment() {
        enclosing = null;
    }

    // Environment initializer for local scope environments.
    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    // Provided the name of a variable, returns the value associated with that
    // variable. If the variable has not been defined, throws a RuntimeError.
    Object get(Token name) {
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

    // Sets the value of the specified variable. If the specified variable has
    // already been defined, simply override the old value.
    void define(String name, Object value) {
        values.put(name, value);
    }

    // Sets the value of the specified variable if and only if the variable has
    // already been defined. If the variable has not been defined, throws a
    // RuntimeError, similar to get().
    void assign(Token name, Object value) {
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
}
