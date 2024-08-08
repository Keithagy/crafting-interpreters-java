package keithang.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * Script to help with generating AST.
 * Here is the grammar we currently have gone over:
 *
 * expression => literal | unary | binary | grouping ;
 *
 * literal => NUMBER | STRING | "true" | "false" | "nil" ;
 * grouping => "(" expression ")" ;
 * unary => ("-" | "!") expression ;
 * binary => expression operator expression ;
 * operator => "==" | "!=" | "<" | "<=" | ">" | ">=" | "+" | "-" | "*" | "/" ;
 * 
 * Expanding to account for statemnts:
 * program -> statement* EOF ;
 * statement -> exprStmt | printStmt ;
 * exprStmt -> expression ";" ;
 * printStmt -> "print" expression ";" ;
 * // Everything below as per before...
 *
 * Expanding further to account for variable declaration:
 * Right now, declarations only distinguish between variables and base
 * statements, but will later include functions and classes.
 * program => declaration* EOF ;
 * declaration => varDecl | statement ;
 * varDecl => "var" IDENTIFIER ("=" expression)? ";" ;
 * statement => exprStmt | printStmt ;
 * primary → "true" | "false" | "nil"
 * | NUMBER | STRING
 * | "(" expression ")"
 * | IDENTIFIER ;
 * expression => assignment ;
 * assignment => IDENTIFIER "=" assignemnt | equality;
 *
 * NOTE: This grammar remains ambiguous. For instance, what does it mean to
 * define the binary expression `"doggo" "+" "555"`?
 */
public class GenerateAst {
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: generate_ast <output directory>");
      System.exit(64);
    }
    String outputDir = args[0];
    defineAst(outputDir, "Expr", Arrays.asList(
        "Assign : Token name, Expr value",
        "Binary : Expr left, Token operator, Expr right",
        "Grouping : Expr expression",
        "Literal : Object value",
        "Logical : Expr left, Token operator, Expr right",
        "Unary : Token operator, Expr right",
        "Variable : Token name"));
    defineAst(outputDir, "Stmt", Arrays.asList(
        "Block : List<Stmt> statements",
        "Expression: Expr expression",
        "If: Expr condition, Stmt thenBranch, Stmt elseBranch",
        "Print: Expr expression",
        "Var: Token name, Expr initializer"));
  }

  /*
   * For brevity's sake, I jammed the descriptions of the expression types into
   * strings. Each is the name of the class followed by `:` and the list of
   * fields, separated by commas. Each field has a type and a name.
   */
  private static void defineAst(
      String outputDir, String baseName, List<String> types)
      throws IOException {
    String path = outputDir + "/" + baseName + ".java";
    PrintWriter writer = new PrintWriter(path, "UTF-8");

    // Write boilerplate
    writer.println("package keithang.craftinginterpreters.lox;");
    writer.println();
    writer.println("import java.util.List;");
    writer.println();

    writer.println("abstract class " + baseName + " {");

    defineVisitor(writer, baseName, types);

    // AST classes
    // Write data
    for (String type : types) {
      String className = type.split(":")[0].trim();
      String fields = type.split(":")[1].trim();
      defineType(writer, baseName, className, fields);
    }
    writer.println();
    writer.println("  abstract <R> R accept(Visitor<R> visitor);");

    // Close class definition
    writer.println("}");
    writer.close();
  }

  private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
    writer.println("  interface Visitor<R> {");
    for (String type : types) {
      String typeName = type.split(":")[0].trim();
      writer.println("    R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
    }
    writer.println("  }");
  }

  private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
    writer.println("  static class " + className + " extends " + baseName + " {");

    // Constructor.
    writer.println("    " + className + "(" + fieldList + ") {");

    // Store parameters in fields.
    String[] fields = fieldList.split(", ");
    for (String field : fields) {
      String name = field.split(" ")[1];
      writer.println("    this." + name + " = " + name + ";");
    }
    writer.println("    }");

    // Visitor pattern.
    writer.println();
    writer.println("    @Override");
    writer.println("    <R> R accept(Visitor<R> visitor) {");
    writer.println("      return visitor.visit" +
        className + baseName + "(this);");
    writer.println("    }");

    // Field definitions.
    writer.println();
    for (String field : fields) {
      writer.println("    final " + field + ";");
    }

    writer.println("}");
  }
}
