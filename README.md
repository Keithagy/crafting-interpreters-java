# crafting-interpreters-java

Following through Robert Nystrom's [Crafting Interpreters](https://craftinginterpreters.com/introduction.html) in java.
This implementation focuses on the semantics of the interpreter!
A realer implementation in C (for starters...) will come.

## Running this interpreter

You likely want the [following configs on Gradle](https://stackoverflow.com/a/37737186):

### REPL mode

```shell
gradle run -q --console=plain
```

### Script mode

```shell
gradle run -q --console=plain --args="hello.lox"
```

The above command assumes the presence of a file called `hello.lox` in the `app` directory (full path: `<PROJECT_ROOT>/app/hello.lox`)
