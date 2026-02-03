import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Tokens {

    private String s;
    public List<Json> tokens = new ArrayList<>();
    private int i = 0;

    private void addNext(char c) {
        if (current() != c) throw new RuntimeException("expacting " + c);
        tokens.add(new TokenChar(current()));
        i++;
    }

    public Json peek() {
        return tokens.getFirst();
    }
    public Json pop() {
        return tokens.removeFirst();
    }

    public Json pop(Json expected) {
        Json popped = tokens.removeFirst();
        if (!popped.equals(expected)) throw new RuntimeException("expecting " + expected);
        return popped;
    }

    private char current() {
        return s.charAt(i);
    }
    private char prev() {
        return s.charAt(i-1);
    }

    private boolean out() {
        return i >= s.length();
    }

    private boolean isNum() {
        return i < s.length() && current() >= 48 && current()-48 <= 9;
    }

    private boolean isTrue() {
        return i < s.length() -3 && s.substring(i, i+4).intern() == "true";
    }
    private boolean isFalse() {
        return i < s.length() - 4 && s.substring(i, i+5).intern() == "false";
    }

    private boolean isBool() {
        return isTrue() || isFalse();
    }

    private void grabBool() {
        if (isTrue()) {
            tokens.add(new JsonBoolean(true));
            i+= 4;
        } else if (isFalse()) {
            tokens.add(new JsonBoolean(false));
            i+= 5;
        } else {
            throw new RuntimeException("expecting bool ");
        }
    }

    private void grabNum() {
        StringBuilder numRes = new StringBuilder();
        while (isNum()) {
            numRes.append(s.charAt(i));
            i++;
        }
        tokens.add(new JsonNumber(Long.parseLong(numRes.toString())));
    }

    private void grabString() {
        StringBuilder res = new StringBuilder();
        res.append(s.charAt(i++));
        while (true) {
            if (out()) {
                throw new RuntimeException("Missing closing JSON string");
            }

            res.append(current());
            if (current() == '\"' && prev() != '\\') {
                break;
            } else {
                i++;
            }
        }
        i++;
        tokens.add(new JsonString(res.toString()));
    }

    public Tokens(String raw) {
        s = raw;


        while (i < raw.length()) {

            if (isNum()) {
                grabNum();
                continue;
            } else if (isBool()) {
                grabBool();
                continue;
            }

            switch (current()) {
                case '\"' -> grabString();
                case ' ' -> i++;
                case ':' -> addNext(':');
                case '{' -> addNext('{');
                case '}' -> addNext('}');
                case ',' -> addNext(',');
                case '[' -> addNext('[');
                case ']' -> addNext(']');
                default -> throw new RuntimeException("strange token " + current());
            }

        }
    }
}