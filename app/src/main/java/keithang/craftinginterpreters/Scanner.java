package keithang.craftinginterpreters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The `start` and `current` fields are offsets that index into the string. The
 * `start` field points to the first character in the lexeme being scanned, and
 * `current` points at the character currently being considered.
 * The `line` field tracks what source line `current` is on so we can produce
 * token that know their location.
 */
class Scanner {
  private static final Map<String, TokenType> keywords;
  static {
    keywords = new HashMap<>();
    keywords.put("and", TokenType.AND);
    keywords.put("class", TokenType.CLASS);
    keywords.put("else", TokenType.ELSE);
    keywords.put("false", TokenType.FALSE);
    keywords.put("for", TokenType.FOR);
    keywords.put("fun", TokenType.FUN);
    keywords.put("if", TokenType.IF);
    keywords.put("nil", TokenType.NIL);
    keywords.put("or", TokenType.OR);
    keywords.put("print", TokenType.PRINT);
    keywords.put("return", TokenType.RETURN);
    keywords.put("super", TokenType.SUPER);
    keywords.put("this", TokenType.THIS);
    keywords.put("true", TokenType.TRUE);
    keywords.put("var", TokenType.VAR);
    keywords.put("while", TokenType.WHILE);
  }
  private final String source;
  private final List<Token> tokens = new ArrayList<>();

  private int start = 0;
  private int current = 0;
  private int line = 1;

  Scanner(String source) {
    this.source = source;
  }

  List<Token> scanTokens() {
    while (!isAtEnd()) {
      // We are at the beginning of the next lexeme.
      start = current;
      scanToken();
    }

    tokens.add(new Token(TokenType.EOF, "", null, line));
    return tokens;
  }

  private boolean isAtEnd() {
    return current >= source.length();
  }

  private void scanToken() {
    char c = advance();
    switch (c) {
      case '(':
        addToken(TokenType.LEFT_PAREN);
        break;
      case ')':
        addToken(TokenType.RIGHT_PAREN);
        break;
      case '{':
        addToken(TokenType.LEFT_BRACE);
        break;
      case '}':
        addToken(TokenType.RIGHT_BRACE);
        break;
      case ',':
        addToken(TokenType.COMMA);
        break;
      case '.':
        addToken(TokenType.DOT);
        break;
      case '-':
        addToken(TokenType.MINUS);
        break;
      case '+':
        addToken(TokenType.PLUS);
        break;
      case ';':
        addToken(TokenType.SEMICOLON);
        break;
      case '*':
        addToken(TokenType.STAR);
        break;
      case '!':
        addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
        break;
      case '=':
        addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
        break;
      case '<':
        addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
        break;
      case '>':
        addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
        break;
      case '/':
        if (match('/')) {
          // A comment goes until the end of the line.
          // advance past those characters.
          while (peek() != '\n' && !isAtEnd()) {
            advance();
          }
        } else {
          addToken(TokenType.SLASH);
        }
        break;
      case ' ':
      case '\r':
      case '\t':
        break; // ignore whitespace.
      case '\n':
        line++;
        break;
      case '"':
        string();
        break;
      default:
        if (isDigit(c)) {
          number();
        } else if (isAlpha(c)) {
          /*
           * Here, our scanner is almost done.
           * The only remaining pieces of the lexical grammar to implement are identifiers
           * and their close cousins, the reserved words.
           *
           * You might think we could match keywords like `or` in the same way we handle
           * multiple-character operators like `<=`.
           * ```java
           * case 'o':
           * if (match('r')) {
           * addToken(OR);
           * }
           * break;
           * ```
           * but consider what would happen if a user named a variable `orchid`!
           *
           * This gets us to an important principle called `maximal munch`:
           * When two lexical grammar rules can both match a chunk of code that the
           * scanner is looking at, whichever one would consume the most characters wins.
           *
           * In this context, `orchid` always gets parsed over `or`, since `orchid`
           * consumes the most characters.
           */
          identifier();
        } else {
          // Note that the erroneous character is still consmed by the earlier call to
          // `advance`.
          // That's important so that we don't get stuck in an infinite loop.
          Lox.error(line, "Unexpected character.");
          break;
        }
    }
  }

  private void identifier() {
    while (isAlphaNumeric(peek())) {
      advance();
    }
    String text = source.substring(start, current);
    TokenType type = keywords.get(text);
    if (type == null) {
      type = TokenType.IDENTIFIER;
    }
    addToken(type);
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') ||
        (c >= 'A' && c <= 'Z') ||
        c == '_';
  }

  private boolean isDigit(char c) {
    return c >= '0' && c < '9';
  }

  private void number() {
    while (isDigit(peek())) {
      advance();
    }

    // Look for a fractional part.
    if (peek() == '.' && isDigit(peekNext())) {
      // Consume the "."
      advance();
      while (isDigit(peek())) {
        advance();
      }
    }

    addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
  }

  private char peekNext() {
    if (current + 1 >= source.length()) {
      return '\0';
    }
    return source.charAt(current + 1);
  }

  private void string() {
    while (peek() != '"' && !isAtEnd()) {
      // For no particular reason, Lox supports multi-line strings. There are pros and
      // cons to that, but prohibiting them was a little more complex than allowing
      // them, so I left them in.
      if (peek() == '\n') {
        line++;
      }
      advance();
    }
    if (isAtEnd()) {
      Lox.error(line, "Unterminated string.");
      return;
    }
    // The closing ".
    advance();
    // Trim the surrounding quotes
    String value = source.substring(start + 1, current - 1);
    addToken(TokenType.STRING, value);
  }

  private char peek() {
    return isAtEnd() ? '\0' : source.charAt(current);
  }

  private boolean match(char expected) {
    if (isAtEnd()) {
      return false;
    }
    if (source.charAt(current) != expected) {
      return false;
    }
    current++; // `match` consumes the character, IF it matches
    return true;
  }

  /**
   * The `advance` method consumes the next character in the source file and
   * returns it.
   */
  private char advance() {
    return source.charAt(current++);
  }

  /**
   * Where `advance` is for input, `addToken` is for output. It grabs the text of
   * the current lexeme and creates a new token for it.
   * We'll use the other overload to handle tokens with literal values soon.
   */
  private void addToken(TokenType type) {
    addToken(type, null);
  }

  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }
}
