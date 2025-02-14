package com.thomasha.jlox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<String, Object> values = new HashMap<>();

    // Provided the name of a variable, returns the value associated with that
    // variable. If the variable has not been defined, throws a RuntimeError.
    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
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
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
}
