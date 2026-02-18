import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class Parser {
    private final Tokens t;

    static Json parseJson(CharSequence raw) {
        Parser p = new Parser(raw);
        return p.parse();
    }

    private Parser(CharSequence raw) {
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
            case JsonLong jl -> parseLong();
            case JsonDouble jd -> parseDouble();
            case JsonNull jn -> parseNull();
            default -> throw new IllegalStateException("Unexpected value: " + t.peek());
        };
    }

    private <T extends Json> T parseJsonObj(Class<T> clazz) {
        Json next = t.pop();
        if (next.getClass() != clazz) throw new RuntimeException("obj parse expecting " + clazz.getName() + " got " + next.getClass());
        return clazz.cast(next);
    }

    public JsonBoolean parseBoolean() {
        return parseJsonObj(JsonBoolean.class);
    }

    public JsonNull parseNull() {
        return parseJsonObj(JsonNull.class);
    }

    public JsonDouble parseDouble() {
        return parseJsonObj(JsonDouble.class);
    }

    public JsonLong parseLong() {
        return parseJsonObj(JsonLong.class);
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
record JsonDouble(double inner) implements Json {
    @Override
    public String toString() {
        return "" + inner;
    }
}

record JsonLong(long inner) implements Json {
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

record JsonNull() implements Json {
    @Override
    public String toString() {
        return "null";
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
        Json obj = get(rawKey);
        if (obj instanceof JsonNull) return null;
        return (JsonObject) obj;
    }

    public JsonArray getArray(String rawKey) {
        Json obj = get(rawKey);
        if (obj instanceof JsonNull) return null;
        return (JsonArray) obj;
    }

    public String getString(String rawKey) {
        Json obj = get(rawKey);
        if (obj == null || obj instanceof JsonNull) return null;
        if (obj instanceof JsonString js) return js.inner();
        if (obj instanceof JsonLong jl) return String.valueOf(jl.inner());
        if (obj instanceof JsonDouble jd) return String.valueOf(jd.inner());
        return obj.toString();
    }

    public double getDouble(String rawKey) {
        Json obj = get(rawKey);
        return switch (obj) {
            case null -> 0.0;
            case JsonNull jsonNull -> 0.0;
            case JsonLong(long inner) -> inner;
            case JsonString(String inner) -> Double.parseDouble(inner);
            default -> ((JsonDouble) obj).inner();
        };
    }

    public long getLong(String rawKey) {
        Json obj = get(rawKey);
        if (obj == null || obj instanceof JsonNull) return 0L;
        return ((JsonLong) obj).inner();
    }
}

record JsonArray(List<Json> elements) implements Json {
    @Override
    public String toString() {
        return "[" + elements.stream().map(Json::toString).collect(Collectors.joining(", ")) + "]";
    }
}
