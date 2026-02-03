import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class Parser {
    private final Tokens t;

    static Json parseJson(String raw) {
        Parser p = new Parser(raw);
        return p.parse();
    }

    private Parser(String raw) {
        t = new Tokens(raw);
    }

    public void expectChar(char expected) {
        Json popped = t.pop();
        if (!(popped instanceof TokenChar(char c) && c == expected)) throw new RuntimeException("needed: " + expected + " got: " + popped);
    }

    public JsonString parseString() {
        Json next = t.pop();
        if (!(next instanceof JsonString js)) throw new RuntimeException("Expecting json string, got " + next);
        return js;
    }

    private Json parse() {
        return switch (t.peek()) {
            case JsonString js -> parseString();
            case TokenChar tk -> switch (tk.c()) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                default -> throw new IllegalStateException("Unexpected value in tokenchar: " + tk.c());
            };
            case JsonBoolean jb -> parseBoolean();
            case JsonNumber jn -> parseNumber();
            default -> throw new IllegalStateException("Unexpected value: " + t.peek());
        };
    }

    public JsonBoolean parseBoolean() {
        Json next = t.pop();
        if (!(next instanceof JsonBoolean jb)) throw new RuntimeException("Expecting json boolean, got " + next);
        return jb;
    }

    public JsonNumber parseNumber() {
        Json next = t.pop();
        if (!(next instanceof JsonNumber jn)) throw new RuntimeException("Expecting json number, got " + next);
        return jn;
    }

    public JsonObject parseObject() {
        expectChar('{');
        List<JsonPair> pairs = new ArrayList<>();


        while (!(t.peek() instanceof TokenChar(char c) && c == '}')) {
            if (!pairs.isEmpty()) expectChar(',');
            pairs.add(parsePair());

        }
        t.pop(new TokenChar('}'));
        return new JsonObject(pairs);
    }

    public JsonPair parsePair() { // todo key must be string
        JsonString key = parseString();
        expectChar(':');
        Json val = parse();
        return new JsonPair(key, val);
    }

    public JsonArray parseArray() {
        expectChar('[');
        List<Json> elems = new ArrayList<>();

        while (!(t.peek() instanceof TokenChar(char c) && c == ']')) {
            if (!elems.isEmpty()) expectChar(',');
            elems.add(parse());

        }
        t.pop(new TokenChar(']'));
        return new JsonArray(elems);
    }



}

public interface Json {
    static Json parse(String s) {
        return Parser.parseJson(s.trim());
    }

    static JsonObject parseObject(String s) {
        return (JsonObject) parse(s);
    }
}

record TokenChar(char c) implements Json {
    @Override
    public String toString() {
        return c + "";
    }
}

record JsonString(String inner) implements Json {
    @Override
    public String toString() {
        return inner;
    }
}
record JsonNumber(double inner) implements Json {
    @Override
    public String toString() {
        return "" + inner;
    }
}
record JsonBoolean(boolean inner) implements Json {
    @Override
    public String toString() {
        return "" + inner;
    }
}

record JsonPair(JsonString left, Json right) implements Json {
    @Override
    public String toString() {
        return left + ": " + right;
    }
}

record JsonObject(List<JsonPair> fields) implements Json { // json spec "allows" duplicates
    @Override
    public String toString() {
        return "{" + fields.stream().map(Json::toString).collect(Collectors.joining(", ")) + "}";
    }

    public Json get(String rawKey) {
        return fields.stream().filter(pair -> pair.left().inner().equals(rawKey)).findFirst().map(JsonPair::right).orElse(null);
    }

    public JsonObject getObj(String rawKey) {
        return (JsonObject) get(rawKey);
    }

    public String getString(String rawKey) {
        return ((JsonString) get(rawKey)).inner();
    }

    public double getDouble(String rawKey) {
        return ((JsonNumber) get(rawKey)).inner();
    }
}

record JsonArray(List<Json> elements) implements Json {
    @Override
    public String toString() {
        return "[" + elements.stream().map(Json::toString).collect(Collectors.joining(", ")) + "]";
    }
}
