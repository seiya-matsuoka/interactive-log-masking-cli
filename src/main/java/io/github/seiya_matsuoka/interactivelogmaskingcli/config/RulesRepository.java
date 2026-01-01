package io.github.seiya_matsuoka.interactivelogmaskingcli.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** ルール設定（mask-rules.json）の読み込み/保存を担うリポジトリ。 */
public class RulesRepository {

  private final ObjectMapper objectMapper;

  /** デフォルト設定の ObjectMapper を利用する。 */
  public RulesRepository() {
    this.objectMapper = createDefaultMapper();
  }

  /** テストなどで ObjectMapper を差し替えたい場合に使用する。 */
  public RulesRepository(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * 指定パスからルールJSONを読み込む。
   *
   * @param path JSONファイルパス
   * @return 読み込んだ設定
   * @throws IOException 読み込みに失敗した場合
   */
  public MaskRulesConfig load(Path path) throws IOException {
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      return objectMapper.readValue(reader, MaskRulesConfig.class);
    }
  }

  /**
   * 指定パスへルールJSONを保存する（pretty print）。
   *
   * @param path 出力先パス
   * @param config 保存する設定
   * @throws IOException 書き込みに失敗した場合
   */
  public void save(Path path, MaskRulesConfig config) throws IOException {
    if (path.getParent() != null) {
      Files.createDirectories(path.getParent());
    }
    try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, config);
    }
  }

  private static ObjectMapper createDefaultMapper() {
    ObjectMapper mapper = new ObjectMapper();

    // 未知フィールドは無視（安全対策）
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    // JSONのenum不一致がある場合に例外ではなく null として扱い Validator で検出しやすくする
    mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

    return mapper;
  }
}
