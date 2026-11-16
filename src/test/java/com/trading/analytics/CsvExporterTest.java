package com.trading.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trading.model.Trade;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link CsvExporter}. Every file is written inside a JUnit temporary directory,
 * which JUnit deletes after each test.
 */
class CsvExporterTest {

  private static final String BOM = "﻿";
  private static final String CRLF = "\r\n";
  private static final String HEADER =
      "TradeID,Timestamp,Symbol,Action,Quantity,ExecutedPrice,RealizedPnL";
  private static final int BOM_BYTE_1 = 0xEF;
  private static final int BOM_BYTE_2 = 0xBB;
  private static final int BOM_BYTE_3 = 0xBF;
  private static final int BYTE_MASK = 0xFF;

  @TempDir
  Path tempDir;

  /** The two example rows from ANALYTICS.md: one BUY and one SELL. */
  private static List<Trade> sampleTrades() {
    return List.of(
        TradeFixtures.trade(
            "TR-001", "AAPL", true, 10, 182.50, 0.0, Instant.parse("2026-03-24T10:15:30Z")),
        TradeFixtures.trade(
            "TR-002", "AAPL", false, 10, 188.00, 55.0, Instant.parse("2026-03-24T14:20:10Z")));
  }

  /** Exports the trades to a fresh file and returns its text, including the BOM character. */
  private String export(List<Trade> trades) throws IOException {
    Path target = tempDir.resolve("trades.csv");
    CsvExporter.exportTrades(trades, target);
    return Files.readString(target, StandardCharsets.UTF_8);
  }

  /** Exports a single trade and returns only its data row, without the line ending. */
  private String exportSingleRow(Trade trade) throws IOException {
    String content = export(List.of(trade));
    String prefix = BOM + HEADER + CRLF;
    assertTrue(content.startsWith(prefix));
    assertTrue(content.endsWith(CRLF));
    return content.substring(prefix.length(), content.length() - CRLF.length());
  }

  @Test
  void fileStartsWithUtf8ByteOrderMark() throws IOException {
    Path target = tempDir.resolve("trades.csv");

    CsvExporter.exportTrades(sampleTrades(), target);

    byte[] bytes = Files.readAllBytes(target);
    assertEquals(BOM_BYTE_1, bytes[0] & BYTE_MASK);
    assertEquals(BOM_BYTE_2, bytes[1] & BYTE_MASK);
    assertEquals(BOM_BYTE_3, bytes[2] & BYTE_MASK);
  }

  @Test
  void firstLineIsTheHeaderAfterStrippingTheBom() throws IOException {
    String content = export(sampleTrades());

    assertTrue(content.startsWith(BOM));
    String firstLine = content.substring(BOM.length()).split(CRLF)[0];
    assertEquals(HEADER, firstLine);
  }

  @Test
  void lineCountIsNumberOfTradesPlusHeader() throws IOException {
    String content = export(sampleTrades());

    String[] lines = content.split(CRLF);
    assertEquals(sampleTrades().size() + 1, lines.length);
  }

  @Test
  void everyLineEndsWithCrlfAndThereAreNoBareLineBreaks() throws IOException {
    String content = export(sampleTrades());

    assertTrue(content.endsWith(CRLF));
    String withoutCrlf = content.replace(CRLF, "");
    assertFalse(withoutCrlf.contains("\n"));
    assertFalse(withoutCrlf.contains("\r"));
  }

  @Test
  void wholeFileMatchesTheSpecExample() throws IOException {
    String expected = BOM + HEADER + CRLF
        + "TR-001,2026-03-24T10:15:30Z,AAPL,BUY,10,182.50,0.00" + CRLF
        + "TR-002,2026-03-24T14:20:10Z,AAPL,SELL,10,188.00,55.00" + CRLF;

    assertEquals(expected, export(sampleTrades()));
  }

  @Test
  void fieldContainingCommaIsQuoted() throws IOException {
    Trade trade = TradeFixtures.trade(
        "T-1", "BRK,A", false, 1, 10.0, 1.0, TradeFixtures.BASE_TIME);

    assertEquals("T-1,2026-03-24T10:15:30Z,\"BRK,A\",SELL,1,10.00,1.00", exportSingleRow(trade));
  }

  @Test
  void quoteInsideFieldIsDoubledAndFieldIsQuoted() throws IOException {
    Trade trade = TradeFixtures.trade(
        "say \"hi\"", "AAPL", true, 1, 10.0, 0.0, TradeFixtures.BASE_TIME);

    assertEquals(
        "\"say \"\"hi\"\"\",2026-03-24T10:15:30Z,AAPL,BUY,1,10.00,0.00", exportSingleRow(trade));
  }

  @Test
  void lineBreaksInsideFieldAreKeptInsideQuotes() throws IOException {
    Trade withLineFeed = TradeFixtures.trade(
        "A\nB", "AAPL", true, 1, 10.0, 0.0, TradeFixtures.BASE_TIME);
    Trade withCarriageReturn = TradeFixtures.trade(
        "C\rD", "AAPL", true, 1, 10.0, 0.0, TradeFixtures.BASE_TIME);

    assertEquals(
        "\"A\nB\",2026-03-24T10:15:30Z,AAPL,BUY,1,10.00,0.00", exportSingleRow(withLineFeed));
    assertEquals(
        "\"C\rD\",2026-03-24T10:15:30Z,AAPL,BUY,1,10.00,0.00",
        exportSingleRow(withCarriageReturn));
  }

  @Test
  void unicodeTextSurvivesUnchanged() throws IOException {
    Trade trade = TradeFixtures.trade(
        "Lệnh-01 ✓ 株", "AAPL", true, 1, 10.0, 0.0, TradeFixtures.BASE_TIME);

    assertEquals(
        "Lệnh-01 ✓ 株,2026-03-24T10:15:30Z,AAPL,BUY,1,10.00,0.00", exportSingleRow(trade));
  }

  @Test
  void timestampIsIsoUtcTruncatedToSeconds() throws IOException {
    Trade trade = TradeFixtures.trade(
        "T-1", "AAPL", true, 1, 10.0, 0.0, Instant.parse("2026-03-24T10:15:30.987654321Z"));

    assertEquals("T-1,2026-03-24T10:15:30Z,AAPL,BUY,1,10.00,0.00", exportSingleRow(trade));
  }

  @Test
  void numbersUseDotDecimalSeparatorUnderVietnameseLocale() throws IOException {
    Locale original = Locale.getDefault();
    Locale.setDefault(Locale.forLanguageTag("vi-VN"));
    try {
      Trade trade = TradeFixtures.trade(
          "T-1", "AAPL", false, 10, 182.5, 1234.5, TradeFixtures.BASE_TIME);

      assertEquals(
          "T-1,2026-03-24T10:15:30Z,AAPL,SELL,10,182.50,1234.50", exportSingleRow(trade));
    } finally {
      Locale.setDefault(original);
    }
  }

  @Test
  void numbersAreRoundedToTwoDecimalsAndKeepTheirSign() throws IOException {
    Trade trade = TradeFixtures.trade(
        "T-1", "AAPL", false, 3, 99.999, -80.256, TradeFixtures.BASE_TIME);

    assertEquals("T-1,2026-03-24T10:15:30Z,AAPL,SELL,3,100.00,-80.26", exportSingleRow(trade));
  }

  @Test
  void nullOrEmptyTradeListWritesHeaderOnly() throws IOException {
    String expected = BOM + HEADER + CRLF;

    assertEquals(expected, export(null));
    assertEquals(expected, export(List.of()));
  }

  @Test
  void existingFileIsReplacedNotAppended() throws IOException {
    Path target = tempDir.resolve("trades.csv");
    Files.writeString(target, "old content that is much longer than the new file".repeat(10));

    CsvExporter.exportTrades(List.of(), target);

    assertEquals(BOM + HEADER + CRLF, Files.readString(target, StandardCharsets.UTF_8));
  }

  @Test
  void nullTargetPathThrowsNullPointerException() {
    assertThrows(
        NullPointerException.class, () -> CsvExporter.exportTrades(sampleTrades(), null));
  }

  @Test
  void nullElementIsRejectedBeforeAnyFileIsCreated() {
    Path target = tempDir.resolve("trades.csv");
    List<Trade> trades = Arrays.asList(TradeFixtures.buy("T-1"), null);

    assertThrows(
        IllegalArgumentException.class, () -> CsvExporter.exportTrades(trades, target));
    assertFalse(Files.exists(target));
  }

  @Test
  void targetThatIsADirectoryThrowsIoException() {
    assertThrows(IOException.class, () -> CsvExporter.exportTrades(sampleTrades(), tempDir));
  }

  @Test
  void missingParentDirectoryThrowsIoException() {
    Path target = tempDir.resolve("does-not-exist").resolve("trades.csv");

    assertThrows(IOException.class, () -> CsvExporter.exportTrades(sampleTrades(), target));
  }
}
