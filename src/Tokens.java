import java.util.ArrayList;
import java.util.List;

public class Tokens {
    private CharSequence s;
    public List<Json> tokens = new ArrayList<>();
    private int i = 0;

    private void addNext(char c) {
        if (current() != c) throw new RuntimeException("expecting " + c);
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
        return i < s.length() -3 && s.subSequence(i, i+4).equals("true");
    }
    private boolean isFalse() {
        return i < s.length() - 4 && s.subSequence(i, i+5).equals("false");
    }

    private boolean isDot() {
        return i < s.length() && s.charAt(i) == '.';
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
            System.err.println(s);
            throw new RuntimeException("expecting bool ");
        }
    }

    private void grabNum() {
        StringBuilder numRes = new StringBuilder();
        while (isNum() || isDot()) {
            numRes.append(s.charAt(i));
            i++;
        }
        tokens.add(new JsonNumber(Double.parseDouble(numRes.toString())));
    }

    private void grabString() {
        StringBuilder res = new StringBuilder();
        i++;
        while (true) {
            if (out()) {
                System.err.println(s);
                throw new RuntimeException("Missing closing JSON string");
            }


            if (current() == '\"' && prev() != '\\') {
                break;
            } else {
                res.append(current());
                i++;
            }
        }
        i++;
        tokens.add(new JsonString(res.toString()));
    }

    public Tokens(CharSequence raw) {
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