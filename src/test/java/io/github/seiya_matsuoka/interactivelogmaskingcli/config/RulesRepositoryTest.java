package io.github.seiya_matsuoka.interactivelogmaskingcli.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RulesRepositoryTest {

  @TempDir Path tempDir;

  @Test
  void load_valid_rules_json() throws Exception {

    Path jsonPath = copyResourceToTemp("fixtures/rules/valid-mask-rules.json");

    RulesRepository repo = new RulesRepository();
    MaskRulesConfig config = repo.load(jsonPath);

    assertNotNull(config);
    assertEquals(1, config.getVersion());
    assertEquals(1, config.getRules().size());
    assertEquals("email", config.getRules().get(0).getId());
  }

  @Test
  void save_and_load_roundtrip() throws Exception {

    MaskRulesConfig config =
        new MaskRulesConfig()
            .setVersion(1)
            .setRules(
                List.of(
                    new MaskRuleConfig()
                        .setId("id1")
                        .setName("name1")
                        .setEnabled(true)
                        .setPattern("abc")
                        .setReplacement("[X]")
                        .setFlags(List.of(RuleFlag.CASE_INSENSITIVE))));

    Path out = tempDir.resolve("saved.json");

    RulesRepository repo = new RulesRepository();
    repo.save(out, config);

    MaskRulesConfig loaded = repo.load(out);

    assertEquals(1, loaded.getVersion());
    assertEquals(1, loaded.getRules().size());
    assertEquals("id1", loaded.getRules().get(0).getId());
  }

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
