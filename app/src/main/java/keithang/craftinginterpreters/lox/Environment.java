package keithang.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
  final Environment enclosing;
  private final Map<String, Object> values = new HashMap<>();

  Environment() {
    enclosing = null;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  /**
   * Notice that when we add the key to the map, we don't check to see if it's
   * already present. This allows our variables to be freely redeclared.
   */
  void define(String name, Object value) {
    values.put(name, value);
  }

  Object get(Token name) {
    if (values.containsKey(name.lexeme)) {
      return values.get(name.lexeme);
    }
    if (enclosing != null) {
      return enclosing.get(name);
    }

    // What if we are trying to access a varialbe that hasn't yet been defined?
    // We have a few choices:
    // - Syntax error (we want this to have a helpful compiler)
    // - Runtime error
    // - Just return nil (haha)
    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }

  public void assign(Token name, Object value) {
    if (values.containsKey(name.lexeme)) {
      values.put(name.lexeme, value);
      return;
    }
    if (enclosing != null) {
      enclosing.assign(name, value);
      return;
    }
    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }
}
