package keithang.craftinginterpreters.lox;

import java.util.Arrays;
import java.util.List;

import keithang.craftinginterpreters.lox.Expr.Assign;
import keithang.craftinginterpreters.lox.Expr.Binary;
import keithang.craftinginterpreters.lox.Expr.Call;
import keithang.craftinginterpreters.lox.Expr.Function;
import keithang.craftinginterpreters.lox.Expr.Get;
import keithang.craftinginterpreters.lox.Expr.Grouping;
import keithang.craftinginterpreters.lox.Expr.Literal;
import keithang.craftinginterpreters.lox.Expr.Logical;
import keithang.craftinginterpreters.lox.Expr.Set;
import keithang.craftinginterpreters.lox.Expr.Super;
import keithang.craftinginterpreters.lox.Expr.This;
import keithang.craftinginterpreters.lox.Expr.Unary;
import keithang.craftinginterpreters.lox.Expr.Variable;

class AstPrinter implements Expr.Visitor<String> {
  String print(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public String visitBinaryExpr(Binary expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  @Override
  public String visitGroupingExpr(Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Literal expr) {
    if (expr.value == null) {
      return "nil";
    }

    String text = expr.value.toString();
    if (text.endsWith(".0")) {
      text = text.substring(0, text.length() - 2);
    }
    return text;
  }

  @Override
  public String visitUnaryExpr(Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  private String parenthesize(String name, Expr... exprs) {
    StringBuilder builder = new StringBuilder();

    builder.append("(").append(name);
    for (Expr expr : exprs) {
      builder.append(" ");
      // NOTE: nested sub-expressions call `accept()` on `this` same visitor, which is
      // what gives you the recursive printing of an entire tree
      builder.append(expr.accept(this));
    }
    builder.append(")");
    return builder.toString();
  }

  // NOTE: this should really be a test...
  public static void main(String[] args) {
    Expr expression = new Binary(
        new Unary(
            new Token(TokenType.MINUS, "-", null, 1),
            new Literal(123)),
        new Token(TokenType.STAR, "*", null, 1),
        new Grouping(new Literal(45.67)));
    System.out.println(new AstPrinter().print(expression));
  }

  @Override
  public String visitVariableExpr(Variable expr) {
    return "Variable" + expr.name.lexeme;
  }

  @Override
  public String visitAssignExpr(Assign expr) {
    return "Assign value " + print(expr.value) + " to var " + expr.name.lexeme;
  }

  @Override
  public String visitLogicalExpr(Logical expr) {
    return print(expr.left) + " " + (expr.operator.type == TokenType.OR ? "OR" : "AND") + " " + print(expr.right);
  }

  @Override
  public String visitCallExpr(Call expr) {
    List<Expr> exprs = Arrays.asList(expr.callee);
    exprs.addAll(expr.arguments);
    return parenthesize("Call", exprs.toArray(new Expr[exprs.size()]));
  }

  @Override
  public String visitFunctionExpr(Function expr) {
    return parenthesize("Fn Expr", expr);
  }

  @Override
  public String visitGetExpr(Get expr) {
    return parenthesize("Get", expr);
  }

  @Override
  public String visitSetExpr(Set expr) {
    return parenthesize("Set", expr);
  }

  @Override
  public String visitThisExpr(This expr) {
    return parenthesize("This", expr);
  }

  @Override
  public String visitSuperExpr(Super expr) {
    return parenthesize("Super", expr);
  }
}
