public record Transaction(String title, String user, String slug, String side, long timestamp, double value) {
    public String getPrettySide() {
        return switch (side.intern()) {
            case "BUY" -> "bought";
            case "SELL" -> "sold";
            default -> throw new IllegalStateException("Unexpected value: " + side);
        };
    }

    public String pretty() {
        return String.format("%s %s $%.2f of \"%s\"", user, getPrettySide(), value, title);
    }

    public boolean buySide() {
        return side.equals("BUY");
    }
}
