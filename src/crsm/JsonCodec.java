package crsm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

/** Small JSON codec for the Supabase Auth and REST payloads used by this desktop client. */
final class JsonCodec {
    private JsonCodec() {}

    static Object parse(String input) {
        Parser parser = new Parser(input == null ? "" : input);
        Object value = parser.value(); parser.space();
        if (parser.index != parser.text.length()) throw new IllegalArgumentException("Unexpected JSON content.");
        return value;
    }

    static String stringify(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return quote(s);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            List<String> pairs = new ArrayList<>();
            map.forEach((k, v) -> pairs.add(quote(String.valueOf(k)) + ":" + stringify(v)));
            return "{" + String.join(",", pairs) + "}";
        }
        if (value instanceof Iterable<?> iterable) {
            List<String> items = new ArrayList<>(); iterable.forEach(v -> items.add(stringify(v)));
            return "[" + String.join(",", items) + "]";
        }
        return quote(value.toString());
    }

    private static String quote(String s) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\""); case '\\' -> out.append("\\\\"); case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r"); case '\t' -> out.append("\\t");
                default -> { if (c < 0x20) out.append(String.format("\\u%04x", (int)c)); else out.append(c); }
            }
        }
        return out.append('"').toString();
    }

    private static final class Parser {
        final String text; int index;
        Parser(String text) { this.text = text; }
        void space() { while (index < text.length() && Character.isWhitespace(text.charAt(index))) index++; }
        Object value() {
            space(); if (index >= text.length()) throw new IllegalArgumentException("Empty JSON response.");
            return switch (text.charAt(index)) {
                case '{' -> object(); case '[' -> array(); case '"' -> string(); case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE); case 'n' -> literal("null", null); default -> number();
            };
        }
        Map<String,Object> object() {
            index++; Map<String,Object> result = new LinkedHashMap<>(); space(); if (take('}')) return result;
            do { space(); String key = string(); space(); require(':'); result.put(key, value()); space(); if (take('}')) return result; require(','); } while (true);
        }
        List<Object> array() {
            index++; List<Object> result = new ArrayList<>(); space(); if (take(']')) return result;
            do { result.add(value()); space(); if (take(']')) return result; require(','); } while (true);
        }
        String string() {
            require('"'); StringBuilder out = new StringBuilder();
            while (index < text.length()) {
                char c = text.charAt(index++); if (c == '"') return out.toString();
                if (c == '\\') {
                    if (index >= text.length()) break; char e = text.charAt(index++);
                    switch (e) { case '"', '\\', '/' -> out.append(e); case 'n' -> out.append('\n'); case 'r' -> out.append('\r'); case 't' -> out.append('\t'); case 'b' -> out.append('\b'); case 'f' -> out.append('\f'); case 'u' -> { if (index + 4 > text.length()) throw new IllegalArgumentException("Invalid JSON escape."); out.append((char)Integer.parseInt(text.substring(index, index + 4), 16)); index += 4; } default -> throw new IllegalArgumentException("Invalid JSON escape."); }
                } else out.append(c);
            }
            throw new IllegalArgumentException("Unterminated JSON string.");
        }
        Object number() {
            int start = index; while (index < text.length() && "-+0123456789.eE".indexOf(text.charAt(index)) >= 0) index++;
            if (start == index) throw new IllegalArgumentException("Invalid JSON value.");
            String n = text.substring(start, index);
            try { return n.contains(".") || n.contains("e") || n.contains("E") ? new BigDecimal(n) : Long.valueOf(n); }
            catch (NumberFormatException ex) { throw new IllegalArgumentException("Invalid JSON number."); }
        }
        Object literal(String expected, Object value) { if (!text.startsWith(expected, index)) throw new IllegalArgumentException("Invalid JSON literal."); index += expected.length(); return value; }
        boolean take(char c) { if (index < text.length() && text.charAt(index) == c) { index++; return true; } return false; }
        void require(char c) { space(); if (!take(c)) throw new IllegalArgumentException("Malformed JSON response."); }
    }
}
