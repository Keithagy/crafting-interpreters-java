package keithang.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
  private final Map<String, Object> values = new HashMap<>();

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

    // What if we are trying to access a varialbe that hasn't yet been defined?
    // We have a few choices:
    // - Syntax error (we want this to have a helpful compiler)
    // - Runtime error
    // - Just return nil (haha)
    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }
}
