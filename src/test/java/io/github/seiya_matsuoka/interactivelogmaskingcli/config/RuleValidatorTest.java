package io.github.seiya_matsuoka.interactivelogmaskingcli.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RuleValidatorTest {

  @TempDir Path tempDir;

  // 正常な設定ではエラー一覧が空になることを確認する
  @Test
  void validate_ok_for_valid_config() throws Exception {

    Path jsonPath = copyResourceToTemp("fixtures/rules/valid-mask-rules.json");

    RulesRepository repo = new RulesRepository();
    MaskRulesConfig config = repo.load(jsonPath);

    RuleValidator validator = new RuleValidator();
    List<String> errors = validator.validate(config);

    assertTrue(errors.isEmpty(), "errors should be empty but was: " + errors);
  }

  // 不正な正規表現を含む設定では、pattern に関するエラーが返ることを確認する
  @Test
  void validate_ng_for_invalid_pattern() throws Exception {

    Path jsonPath = copyResourceToTemp("fixtures/rules/invalid-pattern-mask-rules.json");

    RulesRepository repo = new RulesRepository();
    MaskRulesConfig config = repo.load(jsonPath);

    RuleValidator validator = new RuleValidator();
    List<String> errors = validator.validate(config);

    assertFalse(errors.isEmpty());
    assertTrue(
        errors.stream().anyMatch(s -> s.contains("pattern")),
        "pattern error should exist: " + errors);
  }

  /**
   * src/test/resources 配下のファイル（fixture）を、テスト用の一時ディレクトリへコピーして Path を返す。
   *
   * <p>Validator テストでも「JSON → Config」の流れを使うため、クラスパスリソースを一旦ファイルへコピーする。
   */
  private Path copyResourceToTemp(String resourcePath) throws Exception {

    Path dest = tempDir.resolve("rules.json");

    try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
      if (in == null) {
        throw new IllegalStateException("resource not found: " + resourcePath);
      }
      Files.copy(in, dest);
    }
    return dest;
  }
}
