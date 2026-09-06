package net.meowsers.Peach.Structures;

public enum LightType {
    POINT(0),
    DIRECTIONAL(1),
    SPOT(2);

    private final int id;

    LightType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
