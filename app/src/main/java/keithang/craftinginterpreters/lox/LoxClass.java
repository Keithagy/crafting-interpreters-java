package keithang.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

class LoxClass implements LoxCallable {
  final String name;
  private final Map<String, LoxFunction> methods;
  private final Map<String, LoxFunction> staticMethods;

  LoxClass(String name, Map<String, LoxFunction> methods, Map<String, LoxFunction> staticMethods) {
    this.name = name;
    this.methods = methods;
    this.staticMethods = staticMethods;
  }

  LoxFunction findMethod(String name) {
    if (methods.containsKey(name)) {
      return methods.get(name);
    }
    return null;
  }

  LoxFunction findStaticMethod(String name) {
    if (staticMethods.containsKey(name)) {
      return staticMethods.get(name);
    }
    return null;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int arity() {
    LoxFunction initializer = findMethod("init");
    if (initializer == null) {
      return 0;
    }
    return initializer.arity();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    LoxInstance instance = new LoxInstance(this);
    LoxFunction initializer = findMethod("init");
    if (initializer != null) {
      initializer.bind(instance).call(interpreter, arguments); // >> That's why it's called a receiver
    }
    return instance;
  }
}
