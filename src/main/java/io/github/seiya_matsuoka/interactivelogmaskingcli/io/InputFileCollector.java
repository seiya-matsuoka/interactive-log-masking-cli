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
      return List.of(new InputFileRef(inputPath, inputPath.getFileName()));
    }

    if (!Files.isDirectory(inputPath)) {
      throw new IllegalArgumentException("inputPath はファイルまたはディレクトリである必要があります: " + inputPath);
    }

    try (var stream = Files.walk(inputPath)) {
      return stream
          .filter(Files::isRegularFile)
          .map(p -> new InputFileRef(p, inputPath.relativize(p)))
          // 出力順を安定させる（テストしやすいため）
          .sorted(Comparator.comparing(ref -> ref.relative().toString()))
          .collect(Collectors.toList());
    }
  }
}
