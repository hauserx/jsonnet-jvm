package com.databricks.jsonnetjvm.stdlib;

import com.databricks.jsonnetjvm.runtime.JArray;
import com.databricks.jsonnetjvm.runtime.JNull;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.runtime.Val;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Python-compatible printf-style string formatting for Jsonnet's std.format / % operator. Ported
 * from sjsonnet's Format.scala + DecimalFormat.scala.
 *
 * <p>Parses the format string into literal strings separated by {@link FormatSpec} interpolations,
 * then formats the provided Jsonnet values accordingly.
 *
 * @see <a href= "https://docs.python.org/2/library/stdtypes.html#string-formatting">Python 2 String
 *     Formatting</a>
 */
public class JsonnetFormat {

  /** Parsed format specifier — one per % interpolation in the format string. */
  static class FormatSpec {
    String label; // null if no (name) key
    boolean alternate;
    boolean zeroPadded;
    boolean leftAdjusted;
    boolean blankBeforePositive;
    boolean signCharacter;
    int width; // -1 if unspecified
    boolean widthStar;
    int precision; // -1 if unspecified
    boolean precisionStar;
    char conversion;
  }

  // ---- Format string parser ----

  /** Parse result: leading literal, then alternating (spec, literal) pairs. */
  private static class ParseResult {
    final String leading;
    final List<FormatSpec> specs = new ArrayList<>();
    final List<String> literals = new ArrayList<>(); // one per spec

    ParseResult(String leading) {
      this.leading = leading;
    }
  }

  /** Hand-written parser matching sjsonnet's fastparse grammar. */
  private static ParseResult parse(String fmt) {
    int i = 0;
    // Read leading plain text
    int start = i;
    while (i < fmt.length() && fmt.charAt(i) != '%') i++;
    ParseResult result = new ParseResult(fmt.substring(start, i));

    while (i < fmt.length()) {
      if (fmt.charAt(i) != '%')
        throw new JsonnetException("std.format: internal parse error at " + i);
      i++; // consume '%'
      if (i >= fmt.length()) throw new JsonnetException("std.format: truncated format code");

      FormatSpec spec = new FormatSpec();

      // Optional (name)
      if (fmt.charAt(i) == '(') {
        int end = fmt.indexOf(')', i);
        if (end < 0) throw new JsonnetException("std.format: unclosed (name)");
        spec.label = fmt.substring(i + 1, end);
        i = end + 1;
      }

      // Flags: # 0 - (space) +
      while (i < fmt.length()) {
        char f = fmt.charAt(i);
        if (f == '#') spec.alternate = true;
        else if (f == '0') spec.zeroPadded = true;
        else if (f == '-') spec.leftAdjusted = true;
        else if (f == ' ') spec.blankBeforePositive = true;
        else if (f == '+') spec.signCharacter = true;
        else break;
        i++;
      }

      // Width: integer or *
      spec.width = -1;
      if (i < fmt.length() && fmt.charAt(i) == '*') {
        spec.widthStar = true;
        i++;
      } else {
        int w = 0;
        boolean hasWidth = false;
        while (i < fmt.length() && Character.isDigit(fmt.charAt(i))) {
          hasWidth = true;
          w = w * 10 + (fmt.charAt(i) - '0');
          i++;
        }
        if (hasWidth) spec.width = w;
      }

      // Precision: . followed by integer or *
      spec.precision = -1;
      if (i < fmt.length() && fmt.charAt(i) == '.') {
        i++;
        if (i < fmt.length() && fmt.charAt(i) == '*') {
          spec.precisionStar = true;
          i++;
        } else {
          int p = 0;
          while (i < fmt.length() && Character.isDigit(fmt.charAt(i))) {
            p = p * 10 + (fmt.charAt(i) - '0');
            i++;
          }
          spec.precision = p;
        }
      }

      // Skip optional length modifier (h, l, L)
      if (i < fmt.length() && "hlL".indexOf(fmt.charAt(i)) >= 0) i++;

      // Conversion character
      if (i >= fmt.length()) throw new JsonnetException("std.format: truncated format code");
      spec.conversion = fmt.charAt(i);
      i++;

      result.specs.add(spec);

      // Read trailing plain text until next %
      start = i;
      while (i < fmt.length() && fmt.charAt(i) != '%') i++;
      result.literals.add(fmt.substring(start, i));
    }

    return result;
  }

  // ---- Main entry point ----

  public static String apply(String fmt, Object values) {
    ParseResult parsed = parse(fmt);

    boolean isObject = values instanceof JObject;
    JObject objValues = isObject ? (JObject) values : null;
    List<Object> valueList;
    if (values instanceof JArray arr) {
      valueList = new ArrayList<>(arr.size());
      for (int j = 0; j < arr.size(); j++) valueList.add(arr.get(j));
    } else if (isObject) {
      valueList = new ArrayList<>();
    } else {
      valueList = new ArrayList<>();
      valueList.add(values);
    }

    StringBuilder output = new StringBuilder();
    output.append(parsed.leading);
    int argIdx = 0;

    for (int idx = 0; idx < parsed.specs.size(); idx++) {
      FormatSpec spec = parsed.specs.get(idx);

      if (spec.conversion == '%') {
        output.append(widenRaw(spec, "%"));
        output.append(parsed.literals.get(idx));
        continue;
      }

      // Resolve star width/precision from args
      if (spec.widthStar && !spec.precisionStar) {
        if (argIdx >= valueList.size()) throw new JsonnetException("std.format: not enough values");
        spec.width = (int) toDouble(valueList.get(argIdx++));
      } else if (!spec.widthStar && spec.precisionStar) {
        if (argIdx >= valueList.size()) throw new JsonnetException("std.format: not enough values");
        spec.precision = (int) toDouble(valueList.get(argIdx++));
      } else if (spec.widthStar && spec.precisionStar) {
        if (argIdx + 1 >= valueList.size())
          throw new JsonnetException("std.format: not enough values");
        spec.width = (int) toDouble(valueList.get(argIdx++));
        spec.precision = (int) toDouble(valueList.get(argIdx++));
      }

      // Get argument
      if (!isObject && argIdx >= valueList.size())
        throw new JsonnetException(
            "Too few values to format: "
                + valueList.size()
                + ", expected at least "
                + (argIdx + 1));

      Object raw;
      if (spec.label != null) {
        if (objValues != null) {
          raw = objValues.getField(spec.label);
          if (raw == null)
            throw new JsonnetException("std.format: missing key '" + spec.label + "'");
        } else if (values instanceof JArray) {
          raw = valueList.get(argIdx);
        } else {
          throw new JsonnetException("std.format: invalid format values");
        }
      } else {
        raw = valueList.get(argIdx);
      }

      String formatted = formatValue(spec, raw);
      argIdx++;

      output.append(formatted);
      output.append(parsed.literals.get(idx));
    }

    if (!isObject && argIdx < valueList.size()) {
      throw new JsonnetException(
          "Too many values to format: " + valueList.size() + ", expected " + argIdx);
    }

    return output.toString();
  }

  // ---- Per-value formatting dispatch ----

  private static String formatValue(FormatSpec spec, Object raw) {
    if (raw instanceof String s) {
      return switch (spec.conversion) {
        case 's' -> widenRaw(spec, s);
        case 'c' -> {
          if (s.isEmpty()) throw new JsonnetException("std.format: %c requires non-empty string");
          yield widenRaw(spec, String.valueOf(s.charAt(0)));
        }
        default -> throw new JsonnetException("Format required a number at position, got string");
      };
    }
    if (raw instanceof Double d) {
      return switch (spec.conversion) {
        case 'd', 'i', 'u' -> formatInteger(spec, d);
        case 'o' -> formatOctal(spec, d);
        case 'x' -> formatHexadecimal(spec, d);
        case 'X' -> formatHexadecimal(spec, d).toUpperCase();
        case 'e' -> formatExponent(spec, d).toLowerCase();
        case 'E' -> formatExponent(spec, d);
        case 'f', 'F' -> formatFloat(spec, d);
        case 'g' -> formatGeneric(spec, d).toLowerCase();
        case 'G' -> formatGeneric(spec, d);
        case 'c' -> widenRaw(spec, Character.toString((int) d.doubleValue()));
        case 's' -> {
          long l = d.longValue();
          if (l == d) yield widenRaw(spec, Long.toString(l));
          else yield widenRaw(spec, Double.toString(d));
        }
        default ->
            throw new JsonnetException("std.format: unknown conversion type %" + spec.conversion);
      };
    }
    if (raw instanceof Boolean b) {
      int bv = b ? 1 : 0;
      return switch (spec.conversion) {
        case 'd', 'i', 'u' -> formatInteger(spec, bv);
        case 'o' -> formatOctal(spec, bv);
        case 'x' -> formatHexadecimal(spec, bv);
        case 'X' -> formatHexadecimal(spec, bv).toUpperCase();
        case 'e' -> formatExponent(spec, bv).toLowerCase();
        case 'E' -> formatExponent(spec, bv);
        case 'f', 'F' -> formatFloat(spec, bv);
        case 'g' -> formatGeneric(spec, bv).toLowerCase();
        case 'G' -> formatGeneric(spec, bv);
        case 'c' -> widenRaw(spec, Character.forDigit(bv, 10) + "");
        case 's' -> widenRaw(spec, b.toString());
        default ->
            throw new JsonnetException("std.format: unknown conversion type %" + spec.conversion);
      };
    }
    if (raw instanceof JNull) {
      if (spec.conversion == 's') return widenRaw(spec, "null");
      throw new JsonnetException("std.format: expected number, got null");
    }
    if (raw instanceof Val v) {
      if (spec.conversion == 's') return widenRaw(spec, v.toJson());
      throw new JsonnetException("std.format: cannot format " + typeName(raw));
    }
    return widenRaw(spec, String.valueOf(raw));
  }

  // ---- Integer formats ----

  private static String formatInteger(FormatSpec spec, double s) {
    BigInteger bi = new BigDecimal(s).toBigInteger();
    boolean negative = bi.signum() < 0;
    String lhs = negative ? "-" : "";
    String rhs = bi.abs().toString();
    rhs = precisionPad(rhs, spec.precision);
    return widen(spec, lhs, "", rhs, true, !negative);
  }

  private static String formatOctal(FormatSpec spec, double s) {
    BigInteger bi = new BigDecimal(s).toBigInteger();
    boolean negative = bi.signum() < 0;
    String lhs = negative ? "-" : "";
    String rhs = bi.abs().toString(8);
    rhs = precisionPad(rhs, spec.precision);
    String mhs = (!spec.alternate || rhs.charAt(0) == '0') ? "" : "0";
    return widen(spec, lhs, mhs, rhs, true, !negative);
  }

  private static String formatHexadecimal(FormatSpec spec, double s) {
    BigInteger bi = new BigDecimal(s).toBigInteger();
    boolean negative = bi.signum() < 0;
    String lhs = negative ? "-" : "";
    String rhs = bi.abs().toString(16);
    rhs = precisionPad(rhs, spec.precision);
    String mhs = spec.alternate ? "0x" : "";
    return widen(spec, lhs, mhs, rhs, true, !negative);
  }

  private static String precisionPad(String rhs, int precision) {
    if (precision < 0) return rhs;
    int shortage = precision - rhs.length();
    if (shortage > 0) return "0".repeat(shortage) + rhs;
    return rhs;
  }

  // ---- Float/Exponent/Generic formats (using custom DecimalFormat) ----

  private static String formatFloat(FormatSpec spec, double s) {
    int prec = spec.precision >= 0 ? spec.precision : 6;
    String lhs = s < 0 ? "-" : "";
    String body = decimalFormat(prec, 0, spec.alternate, -1, Math.abs(s)).replace("E", "E+");
    return widen(spec, lhs, "", body, true, s > 0);
  }

  private static String formatExponent(FormatSpec spec, double s) {
    int prec = spec.precision >= 0 ? spec.precision : 6;
    String lhs = s < 0 ? "-" : "";
    String body = decimalFormat(prec, 0, spec.alternate, 2, Math.abs(s)).replace("E", "E+");
    return widen(spec, lhs, "", body, true, s > 0);
  }

  private static String formatGeneric(FormatSpec spec, double s) {
    int precision = spec.precision >= 0 ? spec.precision : 6;
    if (precision == 0) precision = 1;
    double absS = Math.abs(s);
    int exponent = absS != 0 ? (int) Math.floor(Math.log10(absS)) : 0;
    String lhs = s < 0 ? "-" : "";
    String body;
    if (exponent < -4 || exponent >= precision) {
      // Exponential form
      body =
          decimalFormat(
                  spec.alternate ? precision - 1 : 0,
                  spec.alternate ? 0 : precision - 1,
                  spec.alternate,
                  2,
                  absS)
              .replace("E", "E+");
    } else {
      // Fixed-point form
      int digitsBeforePoint = Math.max(1, exponent + 1);
      body =
          decimalFormat(
                  spec.alternate ? precision - digitsBeforePoint : 0,
                  spec.alternate ? 0 : precision - digitsBeforePoint,
                  spec.alternate,
                  -1,
                  absS)
              .replace("E", "E+");
    }
    return widen(spec, lhs, "", body, true, s > 0);
  }

  // ---- DecimalFormat (ported from sjsonnet DecimalFormat.scala) ----

  /**
   * Custom decimal formatting.
   *
   * @param zeroes minimum fractional digits (zero-padded)
   * @param hashes additional optional fractional digits (trailing zeros stripped)
   * @param alternate if true, always show decimal point when precision is 0
   * @param expLength exponent digit count (e.g. 2), or -1 for no exponent
   * @param number the non-negative number to format
   */
  static String decimalFormat(
      int zeroes, int hashes, boolean alternate, int expLength, double number) {
    if (expLength >= 0) {
      // Exponential mode
      long roundLog10 = number == 0.0 ? 1L : (long) Math.ceil(Math.log10(Math.abs(number)));
      long expNum = roundLog10 - 1;
      double scaled = number / Math.pow(10, expNum);
      String prefix = Long.toString((long) scaled);
      String expFrag = leftPadNum(expNum, expLength);
      int precision = zeroes + hashes;

      if (precision == 0 && !alternate) {
        return prefix + "E" + expFrag;
      } else if (precision == 0) {
        return prefix + ".E" + expFrag;
      } else {
        double divided = number / Math.pow(10, expNum - precision);
        double scaledFrac = divided % Math.pow(10, precision);
        String frac = rightPadNum(Math.abs(Math.round(scaledFrac)), zeroes, precision);
        if (frac.isEmpty()) {
          return prefix + "E" + expFrag;
        } else {
          return prefix + "." + frac + "E" + expFrag;
        }
      }
    } else {
      // Fixed-point mode
      int precision = zeroes + hashes;
      double denominator = Math.pow(10, precision);
      double numerator = number * denominator + 0.5;
      long whole = (long) Math.floor(numerator / denominator);
      long fracNum = (long) Math.floor(numerator) % (long) denominator;
      String prefix = Long.toString(whole);

      String frac;
      if (fracNum == 0 && zeroes == 0) {
        frac = "";
      } else {
        int n = 0;
        while (n < hashes && fracNum % 10 == 0 && fracNum != 0) {
          fracNum /= 10;
          n++;
        }
        frac = leftPadNum(fracNum, precision - n);
      }

      if (precision == 0 && !alternate) {
        return prefix;
      } else if (precision == 0) {
        return prefix + ".";
      } else {
        if (frac.isEmpty()) return prefix;
        return prefix + "." + frac;
      }
    }
  }

  /** Left-pad a number to targetWidth digits. */
  private static String leftPadNum(long n, int targetWidth) {
    String sign = n < 0 ? "-" : "";
    long absN = Math.abs(n);
    int nWidth = absN == 0 ? 1 : (int) Math.log10(absN) + 1;
    int pad = targetWidth - nWidth;
    if (pad <= 0) return sign + absN;
    return sign + "0".repeat(pad) + absN;
  }

  /** Right-pad (trim trailing zeros) a fractional part. */
  private static String rightPadNum(long n0, int minWidth, int maxWidth) {
    if (n0 == 0 && minWidth == 0) return "";
    // Strip trailing zeros
    long n = n0;
    int trailingZeros = 0;
    while (n > 0 && n % 10 == 0) {
      n /= 10;
      trailingZeros++;
    }
    n = (long) (n0 / Math.pow(10, trailingZeros));
    int nWidth = n == 0 ? 1 : (int) Math.log10(n) + 1;
    String s = "" + n;
    int padNeeded = minWidth - nWidth;
    if (padNeeded > 0) s = s + "0".repeat(padNeeded);
    if (s.length() > maxWidth) s = s.substring(0, maxWidth);
    return s;
  }

  // ---- widen / widenRaw (ported from sjsonnet Format.scala) ----

  /** Widen raw (no sign handling) — for %s, %c, %%. */
  private static String widenRaw(FormatSpec spec, String txt) {
    return widen(spec, "", "", txt, false, false);
  }

  /**
   * Widen/pad a formatted value. Ported from sjsonnet Format.widen.
   *
   * @param lhs sign string ("-" or "")
   * @param mhs prefix string (e.g. "0x")
   * @param rhs body digits
   * @param numeric whether this is a numeric conversion
   * @param signedConversion true if value is positive AND conversion supports sign
   */
  private static String widen(
      FormatSpec spec,
      String lhs,
      String mhs,
      String rhs,
      boolean numeric,
      boolean signedConversion) {
    String lhs2;
    if (signedConversion && spec.blankBeforePositive) {
      lhs2 = " " + lhs;
    } else if (signedConversion && spec.signCharacter) {
      lhs2 = "+" + lhs;
    } else {
      lhs2 = lhs;
    }

    int totalLen = lhs2.length() + mhs.length() + rhs.length();
    int missingWidth = (spec.width >= 0 ? spec.width : 0) - totalLen;

    if (missingWidth <= 0) {
      return lhs2 + mhs + rhs;
    } else if (spec.zeroPadded) {
      if (numeric) {
        return lhs2 + mhs + "0".repeat(missingWidth) + rhs;
      } else {
        if (spec.leftAdjusted) return lhs2 + mhs + rhs + " ".repeat(missingWidth);
        else return " ".repeat(missingWidth) + lhs2 + mhs + rhs;
      }
    } else if (spec.leftAdjusted) {
      return lhs2 + mhs + rhs + " ".repeat(missingWidth);
    } else {
      return " ".repeat(missingWidth) + lhs2 + mhs + rhs;
    }
  }

  // ---- Utilities ----

  private static double toDouble(Object v) {
    if (v instanceof Double d) return d;
    if (v instanceof Boolean b) return b ? 1.0 : 0.0;
    throw new JsonnetException("std.format: expected number, got " + typeName(v));
  }

  private static String typeName(Object x) {
    if (x instanceof JNull) return "null";
    if (x instanceof Boolean) return "boolean";
    if (x instanceof Double) return "number";
    if (x instanceof String) return "string";
    if (x instanceof JArray) return "array";
    if (x instanceof JObject) return "object";
    return "unknown";
  }
}
