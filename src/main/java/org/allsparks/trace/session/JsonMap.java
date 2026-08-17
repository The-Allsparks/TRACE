package org.allsparks.trace.session;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal JSON object reader for TRACE session metadata. Intentionally not a
 * general deserializer: unknown structures are ignored, and nested objects are
 * only accepted as string maps.
 */
final class JsonMap {
    private final Map<String, String> strings = new LinkedHashMap<>();
    private final Map<String, Boolean> bools = new LinkedHashMap<>();
    private final Map<String, Integer> ints = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> objects = new LinkedHashMap<>();

    static JsonMap parse(String json) {
        JsonMap map = new JsonMap();
        if (json == null || json.isEmpty()) {
            return map;
        }
        String body = json.trim();
        if (body.startsWith("{")) {
            body = body.substring(1);
        }
        if (body.endsWith("}")) {
            body = body.substring(0, body.length() - 1);
        }
        int index = 0;
        while (index < body.length()) {
            index = skipWs(body, index);
            if (index >= body.length()) {
                break;
            }
            if (body.charAt(index) != '"') {
                break;
            }
            ParseString key = readString(body, index);
            index = skipWs(body, key.end);
            if (index >= body.length() || body.charAt(index) != ':') {
                break;
            }
            index = skipWs(body, index + 1);
            if (index >= body.length()) {
                break;
            }
            char start = body.charAt(index);
            if (start == '"') {
                ParseString value = readString(body, index);
                map.strings.put(key.value, value.value);
                index = value.end;
            } else if (start == '{') {
                int end = matchingBrace(body, index);
                JsonMap nested = parse(body.substring(index, end));
                map.objects.put(key.value, nested.strings);
                index = end;
            } else if (body.startsWith("true", index)) {
                map.bools.put(key.value, true);
                index += 4;
            } else if (body.startsWith("false", index)) {
                map.bools.put(key.value, false);
                index += 5;
            } else {
                int end = index;
                while (end < body.length() && "-0123456789".indexOf(body.charAt(end)) >= 0) {
                    end++;
                }
                if (end > index) {
                    map.ints.put(key.value, Integer.parseInt(body.substring(index, end)));
                    index = end;
                } else {
                    break;
                }
            }
            index = skipWs(body, index);
            if (index < body.length() && body.charAt(index) == ',') {
                index++;
            }
        }
        return map;
    }

    String string(String key, String fallback) {
        return strings.getOrDefault(key, fallback);
    }

    boolean bool(String key, boolean fallback) {
        return bools.getOrDefault(key, fallback);
    }

    int integer(String key, int fallback) {
        return ints.getOrDefault(key, fallback);
    }

    Map<String, String> object(String key) {
        return objects.getOrDefault(key, Collections.emptyMap());
    }

    private static int skipWs(String body, int index) {
        while (index < body.length() && Character.isWhitespace(body.charAt(index))) {
            index++;
        }
        return index;
    }

    private static ParseString readString(String body, int index) {
        StringBuilder value = new StringBuilder();
        int cursor = index + 1;
        while (cursor < body.length()) {
            char ch = body.charAt(cursor);
            if (ch == '\\' && cursor + 1 < body.length()) {
                value.append(body.charAt(cursor + 1));
                cursor += 2;
                continue;
            }
            if (ch == '"') {
                return new ParseString(value.toString(), cursor + 1);
            }
            value.append(ch);
            cursor++;
        }
        return new ParseString(value.toString(), cursor);
    }

    private static int matchingBrace(String body, int start) {
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < body.length(); i++) {
            char ch = body.charAt(i);
            if (ch == '"' && (i == 0 || body.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (inString) {
                continue;
            }
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return i + 1;
                }
            }
        }
        return body.length();
    }

    private static final class ParseString {
        private final String value;
        private final int end;

        private ParseString(String value, int end) {
            this.value = value;
            this.end = end;
        }
    }
}
