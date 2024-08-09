package keithang.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {

  private final Stmt.Function declaration;
  private final Environment closure;

  LoxFunction(Stmt.Function declaration, Environment closure) {
    this.closure = closure;
    this.declaration = declaration;
  }

  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme + ">";
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    Environment environment = new Environment(closure);
    for (int i = 0; i < declaration.params.size(); i++) {
      environment.define(declaration.params.get(i).lexeme, arguments.get(i));
    }
    try {
      // For the record, I'm not generally a fan of using exceptions for control flow.
      // But inside a heavily recursive tree-walk interpreter, it's the way to go.
      //
      // Since our own syntax tree evaluation is so heavily tied to the Java call
      // stack, we're pressed to do some heavyweight call stack manipulation
      // occasionally, and exceptions are a handy tool for that.
      //
      // If you didn't want to use a throw as control flow, though, i imagine you
      // would need some way to interrupt the execution of the statements in
      // `executeBlock`, perhaps by having all statements return some kind of signal
      // when visited.
      //
      // This need for throw-based return actually stems from the fact that we choose
      // to have our interpreter implement `Stmt.Visitor<Void>`, and not something
      // like `Stmt.Visitor<Optional<InterruptSignal>>.
      interpreter.executeBlock(declaration.body, environment);
    } catch (Return r) {
      return r.value;
    }
    return null;
  }
}
