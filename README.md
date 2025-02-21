# jLox Interpreter

This is a work-in-progress interpreter built in Java for the Lox language following Bob Nystrom's book [_Crafting Interpreters_](https://craftinginterpreters.com/), a guide to building an interpreter for Lox in both Java and C.

## Features

[The third chapter of _Crafting Interpreters_](https://craftinginterpreters.com/the-lox-language.html) provides a general overview of the Lox language. The complete grammar (both syntactic and lexical) can be found in [Appendix I](https://craftinginterpreters.com/appendix-i.html).

Baseline Lox features:

- dynamic type system, supporting:
  - Booleans
  - strings
  - numbers
  - null (`nil`)
- automatic memory management
- control flow
- functions
- classes and inheritance
- comments
- extendable standard library

Additional features (work-in-progress):

- modulo operator (`%`)
- `break` statement in loops
- compound assignment operators (e.g. `a += 5`)
- improved standard library

## Installation

Please ensure that [JDK-23 or later](https://www.oracle.com/java/technologies/downloads/) and [Maven 3.9 or later](https://maven.apache.org/download.cgi) are installed.

Clone the repository:

```
git clone https://github.com/thomasha1310/jlox-interpreter.git
```

Navigate to the new directory:

```
cd jlox-interpreter
```

Compile the project using Maven:

```
mvn package
```

The compiled JAR will be located at `jlox-interpreter\target\jlox-interpreter-1.0.jar`.

## Usage

To run the interpreter in REPL mode:

```
java -jar jlox-interpreter-1.0.jar
```

To run the interpreter on a file with Lox source code:

```
java -jar jlox-interpreter-1.0.jar lox-source-code.txt
```

## License

This project (like Bob Nystrom's [original code](https://github.com/munificent/craftinginterpreters)) is licensed under the [MIT License](LICENSE).
