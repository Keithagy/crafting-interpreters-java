package keithang.craftinginterpreters;

import java.util.ArrayList;
import java.util.List;

/**
 * The `start` and `current` fields are offsets that index into the string. The
 * `start` field points to the first character in the lexeme being scanned, and
 * `current` points at the character currently being considered.
 * The `line` field tracks what source line `current` is on so we can produce
 * token that know their location.
 */
class Scanner {
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
    }
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
