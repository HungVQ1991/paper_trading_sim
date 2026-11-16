package com.trading.analytics;

import com.trading.model.Trade;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Writes trade history to an RFC 4180-compatible CSV file.
 *
 * <p>This is a utility class: every method is static, it holds no state, and it cannot be
 * instantiated.
 *
 * <p>File format:
 * <ul>
 *   <li>UTF-8 with a byte order mark, so Microsoft Excel detects the encoding.</li>
 *   <li>Every record, including the last, ends with CRLF.</li>
 *   <li>A field containing a comma, a double quote, CR or LF is wrapped in double quotes, and
 *       each double quote inside it is doubled.</li>
 *   <li>Numbers always use a dot as decimal separator, whatever the system locale is.</li>
 *   <li>Timestamps are ISO-8601 in UTC, truncated to whole seconds.</li>
 * </ul>
 */
public final class CsvExporter {

  private static final String BYTE_ORDER_MARK = "﻿";
  private static final String LINE_END = "\r\n";
  private static final String SEPARATOR = ",";
  private static final String HEADER =
      "TradeID,Timestamp,Symbol,Action,Quantity,ExecutedPrice,RealizedPnL";
  private static final String ACTION_BUY = "BUY";
  private static final String ACTION_SELL = "SELL";
  private static final String MONEY_FORMAT = "%.2f";
  private static final String QUOTE = "\"";
  private static final String ESCAPED_QUOTE = "\"\"";

  private CsvExporter() {
    // Utility class: prevents instantiation.
  }

  /**
   * Exports the given trades to a UTF-8 CSV file, replacing the file if it exists.
   *
   * <p>The input is validated before the file is opened, so an invalid list never leaves a
   * half-written file behind.
   *
   * @param trades the executed trades; null is treated as an empty list (header only)
   * @param targetPath the file to write; must not be null
   * @throws IOException if the file cannot be created or written
   * @throws NullPointerException if {@code targetPath} is null
   * @throws IllegalArgumentException if the list contains a null element
   */
  public static void exportTrades(List<Trade> trades, Path targetPath) throws IOException {
    Objects.requireNonNull(targetPath, "targetPath must not be null.");
    List<Trade> snapshot = trades == null ? new ArrayList<>() : new ArrayList<>(trades);
    for (Trade trade : snapshot) {
      if (trade == null) {
        throw new IllegalArgumentException("Trade list must not contain null elements.");
      }
    }
    // try-with-resources closes the writer even when a write fails; the IOException still
    // propagates to the caller.
    try (BufferedWriter writer = Files.newBufferedWriter(targetPath, StandardCharsets.UTF_8)) {
      writer.write(BYTE_ORDER_MARK);
      writer.write(HEADER);
      writer.write(LINE_END);
      for (Trade trade : snapshot) {
        writer.write(toRecord(trade));
        writer.write(LINE_END);
      }
    }
  }

  /** Builds one CSV record (without the line ending) for a trade. */
  private static String toRecord(Trade trade) {
    return String.join(
        SEPARATOR,
        escape(trade.getTradeId()),
        trade.getTimestamp().truncatedTo(ChronoUnit.SECONDS).toString(),
        escape(trade.getSymbol()),
        trade.isBuy() ? ACTION_BUY : ACTION_SELL,
        Integer.toString(trade.getQuantity()),
        formatMoney(trade.getExecutedPrice()),
        formatMoney(trade.getRealizedPnL()));
  }

  /** Formats a monetary value with two decimals and a dot as decimal separator. */
  private static String formatMoney(double value) {
    return String.format(Locale.ROOT, MONEY_FORMAT, value);
  }

  /** Applies RFC 4180 quoting to a text field when it contains a special character. */
  private static String escape(String field) {
    boolean needsQuoting = field.contains(SEPARATOR)
        || field.contains(QUOTE)
        || field.contains("\r")
        || field.contains("\n");
    if (!needsQuoting) {
      return field;
    }
    return QUOTE + field.replace(QUOTE, ESCAPED_QUOTE) + QUOTE;
  }
}
