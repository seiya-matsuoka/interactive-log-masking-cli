package io.github.seiya_matsuoka.interactivelogmaskingcli.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.seiya_matsuoka.interactivelogmaskingcli.config.RuleFlag;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class RegexFlagUtilTest {

  // flags = null のとき 0 を返すことを確認
  @Test
  void toPatternFlags_returns_0_when_null() {
    assertEquals(0, RegexFlagUtil.toPatternFlags(null));
  }

  // CASE_INSENSITIVE が Pattern.CASE_INSENSITIVE に変換されることを確認
  @Test
  void toPatternFlags_converts_case_insensitive() {
    int flags = RegexFlagUtil.toPatternFlags(List.of(RuleFlag.CASE_INSENSITIVE));
    assertEquals(Pattern.CASE_INSENSITIVE, flags);
  }
}
