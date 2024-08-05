package keithang.craftinginterpreters.lox;

import keithang.craftinginterpreters.lox.Expr.Binary;
import keithang.craftinginterpreters.lox.Expr.Grouping;
import keithang.craftinginterpreters.lox.Expr.Literal;
import keithang.craftinginterpreters.lox.Expr.Unary;

/**
 * Lox type Java representation
 * Any Lox value Object
 * nil null
 * Boolean Boolean
 * number Double
 * string String
 */
class Interpreter implements Expr.Visitor<Object> {
  void interpret(Expr expression) {
    try {
      Object value = evaluate(expression);
      System.out.println(stringify(value));
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
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
  public Object visitBinaryExpr(Binary expr) {
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
  public Object visitGroupingExpr(Grouping expr) {
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
  public Object visitLiteralExpr(Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitUnaryExpr(Unary expr) {
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

}
