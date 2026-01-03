package io.github.seiya_matsuoka.interactivelogmaskingcli.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** 入力パスから、処理対象ファイルを収集する。 */
public class InputFileCollector {

  /**
   * 入力パスがファイルなら1件、ディレクトリなら再帰で全ファイルを収集する。
   *
   * @param inputPath 入力（ファイル or ディレクトリ）
   * @return 入力ファイル参照一覧（relative は inputBase からの相対パス）
   */
  public List<InputFileRef> collect(Path inputPath) throws IOException {

    if (Files.isRegularFile(inputPath)) {
      // 単一ファイル入力の場合、relative はファイル名のみとする（outBase直下に出すため）
      return List.of(new InputFileRef(inputPath, inputPath.getFileName()));
    }

    if (!Files.isDirectory(inputPath)) {
      throw new IllegalArgumentException("inputPath はファイルまたはディレクトリである必要があります: " + inputPath);
    }

    // Files.walk でディレクトリ配下を再帰的に探索する（Streamなので try-with-resources で確実にcloseする）
    try (var stream = Files.walk(inputPath)) {
      return stream
          // ディレクトリ配下の通常ファイルだけを処理対象にする
          .filter(Files::isRegularFile)
          // inputBase からの相対パスを保持しておく（out配下の構造維持に使う）
          .map(p -> new InputFileRef(p, inputPath.relativize(p)))
          // 出力順を安定させる（テストしやすいため）
          .sorted(Comparator.comparing(ref -> ref.relative().toString()))
          .collect(Collectors.toList());
    }
  }
}
