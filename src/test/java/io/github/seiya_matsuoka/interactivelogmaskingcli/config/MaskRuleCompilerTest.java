package io.github.seiya_matsuoka.interactivelogmaskingcli.config;

import static org.junit.jupiter.api.Assertions.*;

import io.github.seiya_matsuoka.interactivelogmaskingcli.core.MaskRule;
import java.util.List;
import org.junit.jupiter.api.Test;

class MaskRuleCompilerTest {

  // ルール設定からコンパイル済みのマスキングルールが生成され、flags と enabled（省略時true）が反映されることを確認
  @Test
  void compile_creates_compiled_rule_with_flags_and_enabled_default() {

    MaskRulesConfig config =
        new MaskRulesConfig()
            .setVersion(1)
            .setRules(
                List.of(
                    new MaskRuleConfig()
                        .setId("email")
                        .setName("Email")
                        // enabled を省略（null） => 有効扱い
                        .setPattern("abc")
                        .setReplacement("[X]")
                        .setFlags(List.of(RuleFlag.CASE_INSENSITIVE))));

    MaskRuleCompiler compiler = new MaskRuleCompiler();
    List<MaskRule> rules = compiler.compile(config);

    assertEquals(1, rules.size());

    MaskRule r = rules.get(0);

    assertEquals("email", r.id());
    assertTrue(r.enabled());
    assertNotNull(r.pattern());
    assertEquals("[X]", r.replacement());
  }
}
