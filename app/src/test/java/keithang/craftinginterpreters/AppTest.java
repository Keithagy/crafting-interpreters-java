package keithang.craftinginterpreters;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AppTest {
  @Test
  void appHasAGreeting() {
    Lox classUnderTest = new Lox();
    assertNotNull(classUnderTest.getGreeting(), "app should have a greeting");
  }
}
