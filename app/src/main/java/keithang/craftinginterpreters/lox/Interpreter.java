package keithang.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lox type Java representation
 * Any Lox value Object
 * nil null
 * Boolean Boolean
 * number Double
 * string String
 */
class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

  // `environment` tracks the interpreter changes as we enter and exit local
  // scopes. It tracks the *current* environment. `globals` holds a fixed
  // reference to the outermost global environment.
  final Environment globals = new Environment();
  private Environment environment = globals;
  private final Map<Expr, Integer> locals = new HashMap<>();

  Interpreter() {
    // interesting... a "trait object"...
    // interpreter startup gets clock defined. I imagine other ways exist to do
    // this, such as through static block, but it's ok
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

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  private void execute(Stmt statement) {
    statement.accept(this);
  }

  public void resolve(Expr expr, int depth) {
    locals.put(expr, depth);
  }

  private String stringify(Object object) {
    if (object == null)
      return "nil";
    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }
    return object.toString();
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
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
        checkNotDividingByZero(expr.operator, left, right);
        return (double) left / (double) right;
      case STAR:
        checkNumberOperands(expr.operator, left, right);
        return (double) left * (double) right;
      case PLUS:
        // Overload the `+` operator to also support string concatenation.
        if (left instanceof Double && right instanceof Double) {
          return (double) left + (double) right;
        }
        if (left instanceof String && right instanceof String) {
          return (String) left + (String) right;
        }
        throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
      default:
        throw new RuntimeError(expr.operator, "Invalid binary operator.");
    }
  }

  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null)
      return true;
    if (a == null)
      return false;

    return a.equals(b);
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  private Object evaluate(Expr expression) {
    return expression.accept(this);
  }

  /**
   * Literals are almost values already, but the distinction is important. A
   * literal is a `bit of syntax` that produces a value. Lots of values are
   * produced by computation and don't exist anywhere in the code itself. jthose
   * aren't literals. A literal comes directly from the parser. Values are an
   * interpreter concept, part of the runtime's world.
   */
  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);
    switch (expr.operator.type) {
      case BANG:
        return !isTruthy(right);
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double) right;
      default:
        throw new RuntimeError(expr.operator, "Operator to unary expression must be \"!\" or \"-\".");
    }
  }

  private void checkNotDividingByZero(Token operator,
      Object left, Object right) {
    checkNumberOperands(operator, left, right);
    if ((double) right == 0) {
      throw new RuntimeError(operator, "Cannot divide by zero.");
    }
  }

  private void checkNumberOperands(Token operator,
      Object left, Object right) {
    if (left instanceof Double && right instanceof Double)
      return;
    throw new RuntimeError(operator, "Operands must be numbers.");
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double)
      return;
    throw new RuntimeError(operator, "Operand to unary expression must be a number.");
  }

  /**
   * Lox follows Ruby's simple rule: `false` and `nil` are falsey, and everything
   * else is truthy.
   */
  private boolean isTruthy(Object object) {
    if (object == null)
      return false;
    if (object instanceof Boolean)
      return (boolean) object;
    return true;
  }

  // Unlike expressions, statements produce no values, so the return type of the
  // visit methods is Void, not Object.
  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }
    environment.define(stmt.name.lexeme, value);
    return null;
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return lookUpVariable(expr.name, expr);
  }

  private Object lookUpVariable(Token name, Expr expr) {
    Integer distance = locals.get(expr);
    if (distance != null) {
      return environment.getAt(distance, name.lexeme);
    } else {
      return globals.get(name);
    }
  }

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);

    Integer distance = locals.get(expr);
    if (distance != null) {
      environment.assignAt(distance, expr.name, value);
    } else {
      globals.assign(expr.name, value);
    }
    return value;
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  void executeBlock(List<Stmt> statements, Environment env) {
    Environment previous = this.environment;
    try {
      this.environment = env;
      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }

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

  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = evaluate(expr.left);
    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left)) {
        return left;
      }
    } else {
      if (!isTruthy(left)) {
        return left;
      }
    }
    return evaluate(expr.right);
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.body);
    }
    return null;
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
  public Void visitFunctionStmt(Stmt.Function stmt) {
    LoxFunction function = new LoxFunction(stmt, environment, false);
    environment.define(stmt.name.lexeme, function);
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null)
      value = evaluate(stmt.value);
    throw new Return(value);
  }

  @Override
  public Object visitFunctionExpr(Expr.Function expr) {
    return new LoxFunction(expr.fn, environment, false);
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    Object superclass = null;
    if (stmt.superclass != null) {
      superclass = evaluate(stmt.superclass);
      if (!(superclass instanceof LoxClass)) {
        throw new RuntimeError(stmt.superclass.name, "Superclass must be a class");
      }
    }
    environment.define(stmt.name.lexeme, null);

    // When we execute a subclass definition, we create a new environment. Inside
    // that environment, we store a reference to the superclass -- the actual
    // LoxClass object for the superclass which we have now that we are in the
    // runtime. Then we create the LoxFunctions for each method. Those will capture
    // the current environment -- the one where we just bound "super" -- as their
    // closure, holding on to the superclass like we need.
    if (stmt.superclass != null) {
      environment = new Environment(environment);
      environment.define("super", superclass);
    }
    Map<String, LoxFunction> methods = new HashMap<>();
    for (Stmt.Function method : stmt.methods) {
      LoxFunction function = new LoxFunction(method, environment, method.name.lexeme.equals("init"));
      methods.put(method.name.lexeme, function);
    }
    Map<String, LoxFunction> staticMethods = new HashMap<>();
    for (Stmt.Function staticMethod : stmt.staticMethods) {
      LoxFunction function = new LoxFunction(staticMethod, new Environment(), false);
      staticMethods.put(staticMethod.name.lexeme, function);
    }
    LoxClass klass = new LoxClass(stmt.name.lexeme, (LoxClass) superclass, methods, staticMethods);

    // Once that's done, we pop the environment.
    if (superclass != null) {
      environment = environment.enclosing;
    }
    // This two-stage variable binding process allows references to the class inside
    // its own methods
    environment.assign(stmt.name, klass);
    return null;
  }

  @Override
  public Object visitGetExpr(Expr.Get expr) {
    Object object = evaluate(expr.object);
    if (object instanceof LoxInstance) {
      return ((LoxInstance) object).get(expr.name);
    }
    if (object instanceof LoxClass) {
      return ((LoxClass) object).findStaticMethod(expr.name.lexeme);
    }
    throw new RuntimeError(expr.name, "Only instances have properties.");
  }

  @Override
  public Object visitSetExpr(Expr.Set expr) {
    Object object = evaluate(expr.object);
    if (!(object instanceof LoxInstance)) {
      throw new RuntimeError(expr.name, "Only instances have fields.");
    }
    Object value = evaluate(expr.value);
    ((LoxInstance) object).set(expr.name, value);
    return value;
  }

  @Override
  public Object visitThisExpr(Expr.This expr) {
    return lookUpVariable(expr.keyword, expr);
  }

  @Override
  public Object visitSuperExpr(Expr.Super expr) {
    // TODO: Learning gaps
    // Need to revise how the resolver passes locals to interpreter
    // Need to recap how method binding works
    // Need to recap how environment nestedness lets you access the right object by
    // offsetting distance of super by 1
    // How would we handle super calls with ( inherited ) static methods?
    // Maybe we just say that static methods are not inherited?
    int distance = locals.get(expr);
    LoxClass superclass = (LoxClass) environment.getAt(distance, "super");

    // Unfortunately, inside the super expression, we don't have a convenient note
    // for the resolver to hang the number of hops to `this` on. Fortunately, we do
    // control the layout of the environment chains. The environment where `this` is
    // bound is always right inside the environment where we store `super`.
    // Offfsetting the distance by one looks up `this` in that inner environment.
    LoxInstance object = (LoxInstance) environment.getAt(distance - 1, "this");
    LoxFunction method = superclass.findMethod(expr.method.lexeme);
    if (method == null) {
      throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'.");
    }
    return method.bind(object);
  }
}
