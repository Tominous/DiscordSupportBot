package github.scarsz.discordsupportbot.support;

public enum Status {

    GATHERING_INFO("gathering more information"),
    AWAITING_RESPONSE("awaiting response"),
    RESPONDED("responded"),
    SOLVED("solved"),
    ABANDONED("abandoned");

    private final String name;

    Status(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

}
