package net.meowsers.Peach.Structures;

public class Color {

    public float r, g, b, a;

    public Color() {
        this(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public Color(float val) {
        this(val, val, val, 1.f);
    }

    public Color(float r, float g, float b) {
        this(r, g, b, 1.f);
    }

    public Color(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public Color(Color other) {
        if (other != null) {
            this.r = other.r;
            this.g = other.g;
            this.b = other.b;
            this.a = other.a;
        } else {
            this.r = 1.0f;
            this.g = 1.0f;
            this.b = 1.0f;
            this.a = 1.0f;
        }
    }

    public Color copy() {
        return new Color(this.r, this.g, this.b, this.a);
    }

    public Color set(Color other) {
        if (other != null) {
            this.r = other.r;
            this.g = other.g;
            this.b = other.b;
            this.a = other.a;
        }
        return this;
    }

    public Color set(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        return this;
    }

    public Color set(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Color color = (Color) o;
        return Float.compare(color.r, r) == 0 &&
               Float.compare(color.g, g) == 0 &&
               Float.compare(color.b, b) == 0 &&
               Float.compare(color.a, a) == 0;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(r, g, b, a);
    }

    @Override
    public String toString() {
        return "Color{" + "r=" + r + ", g=" + g + ", b=" + b + ", a=" + a + '}';
    }



    // Grayscale
    public static final Color Black       = new Color(0.000f);
    public static final Color White       = new Color(1.000f);
    public static final Color Gray        = new Color(0.502f);
    public static final Color Grey        = new Color(0.502f);
    public static final Color LightGray   = new Color(0.827f);
    public static final Color LightGrey   = new Color(0.827f);
    public static final Color DarkGray    = new Color(0.251f);
    public static final Color DarkGrey    = new Color(0.251f);

    // Primary colors
    public static final Color Red         = new Color(1.000f, 0.000f, 0.000f);
    public static final Color Green       = new Color(0.000f, 0.502f, 0.000f);
    public static final Color Blue        = new Color(0.000f, 0.000f, 1.000f);

    // Secondary colors
    public static final Color Yellow      = new Color(1.000f, 1.000f, 0.000f);
    public static final Color Cyan        = new Color(0.000f, 1.000f, 1.000f);
    public static final Color Magenta     = new Color(1.000f, 0.000f, 1.000f);

    // Common colors
    public static final Color Orange      = new Color(1.000f, 0.647f, 0.000f);
    public static final Color Purple      = new Color(0.502f, 0.000f, 0.502f);
    public static final Color Violet      = new Color(0.933f, 0.510f, 0.933f);
    public static final Color Pink        = new Color(1.000f, 0.753f, 0.796f);
    public static final Color Brown       = new Color(0.647f, 0.165f, 0.165f);
    public static final Color Lime        = new Color(0.000f, 1.000f, 0.000f);
    public static final Color Olive       = new Color(0.502f, 0.502f, 0.000f);
    public static final Color Navy        = new Color(0.000f, 0.000f, 0.502f);
    public static final Color Teal        = new Color(0.000f, 0.502f, 0.502f);
    public static final Color Aqua        = new Color(0.000f, 1.000f, 1.000f);
    public static final Color Maroon      = new Color(0.502f, 0.000f, 0.000f);
    public static final Color Silver      = new Color(0.753f, 0.753f, 0.753f);
    public static final Color Gold        = new Color(1.000f, 0.843f, 0.000f);

    // Reds
    public static final Color Crimson     = new Color(0.863f, 0.078f, 0.235f);
    public static final Color DarkRed     = new Color(0.545f, 0.000f, 0.000f);
    public static final Color Firebrick   = new Color(0.698f, 0.133f, 0.133f);
    public static final Color Tomato      = new Color(1.000f, 0.388f, 0.278f);
    public static final Color Coral       = new Color(1.000f, 0.498f, 0.314f);
    public static final Color Salmon      = new Color(0.980f, 0.502f, 0.447f);
    public static final Color DarkSalmon  = new Color(0.914f, 0.588f, 0.478f);
    public static final Color LightCoral  = new Color(0.941f, 0.502f, 0.502f);

    // Oranges / Yellows
    public static final Color DarkOrange  = new Color(1.000f, 0.549f, 0.000f);
    public static final Color LightSalmon = new Color(1.000f, 0.627f, 0.478f);
    public static final Color Khaki       = new Color(0.941f, 0.902f, 0.549f);
    public static final Color DarkKhaki   = new Color(0.741f, 0.718f, 0.420f);
    public static final Color LemonChiffon = new Color(1.000f, 0.980f, 0.804f);
    public static final Color GoldYellow  = new Color(1.000f, 0.843f, 0.000f);

    // Greens
    public static final Color DarkGreen   = new Color(0.000f, 0.392f, 0.000f);
    public static final Color ForestGreen = new Color(0.133f, 0.545f, 0.133f);
    public static final Color LimeGreen   = new Color(0.196f, 0.804f, 0.196f);
    public static final Color SpringGreen = new Color(0.000f, 1.000f, 0.498f);
    public static final Color SeaGreen    = new Color(0.180f, 0.545f, 0.341f);
    public static final Color MediumSeaGreen = new Color(0.235f, 0.702f, 0.443f);
    public static final Color Mint        = new Color(0.596f, 1.000f, 0.596f);
    public static final Color MintCream   = new Color(0.961f, 1.000f, 0.980f);
    public static final Color PaleGreen   = new Color(0.596f, 0.984f, 0.596f);
    public static final Color DarkOliveGreen = new Color(0.333f, 0.420f, 0.184f);

    // Blues
    public static final Color LightBlue   = new Color(0.678f, 0.847f, 0.902f);
    public static final Color SkyBlue     = new Color(0.529f, 0.808f, 0.922f);
    public static final Color DeepSkyBlue = new Color(0.000f, 0.749f, 1.000f);
    public static final Color SteelBlue   = new Color(0.275f, 0.510f, 0.706f);
    public static final Color RoyalBlue   = new Color(0.255f, 0.412f, 0.882f);
    public static final Color CornflowerBlue = new Color(0.392f, 0.584f, 0.929f);
    public static final Color DodgerBlue  = new Color(0.118f, 0.565f, 1.000f);
    public static final Color MidnightBlue = new Color(0.098f, 0.098f, 0.439f);
    public static final Color DarkBlue    = new Color(0.000f, 0.000f, 0.545f);
    public static final Color LightSkyBlue = new Color(0.529f, 0.808f, 0.980f);

    // Cyans
    public static final Color DarkCyan    = new Color(0.000f, 0.545f, 0.545f);
    public static final Color LightCyan   = new Color(0.878f, 1.000f, 1.000f);
    public static final Color Turquoise   = new Color(0.251f, 0.878f, 0.816f);
    public static final Color Aquamarine  = new Color(0.498f, 1.000f, 0.831f);
    public static final Color MediumTurquoise = new Color(0.282f, 0.820f, 0.800f);

    // Purples
    public static final Color DarkPurple  = new Color(0.294f, 0.000f, 0.510f);
    public static final Color Indigo      = new Color(0.294f, 0.000f, 0.510f);
    public static final Color DarkViolet  = new Color(0.580f, 0.000f, 0.827f);
    public static final Color BlueViolet  = new Color(0.541f, 0.169f, 0.886f);
    public static final Color MediumPurple = new Color(0.576f, 0.439f, 0.859f);
    public static final Color Plum        = new Color(0.867f, 0.627f, 0.867f);
    public static final Color Orchid      = new Color(0.855f, 0.439f, 0.839f);
    public static final Color Lavender    = new Color(0.902f, 0.902f, 0.980f);

    // Pinks
    public static final Color HotPink     = new Color(1.000f, 0.412f, 0.706f);
    public static final Color DeepPink    = new Color(1.000f, 0.078f, 0.576f);
    public static final Color LightPink   = new Color(1.000f, 0.714f, 0.757f);
    public static final Color PaleVioletRed = new Color(0.859f, 0.439f, 0.576f);
    public static final Color MediumVioletRed = new Color(0.780f, 0.082f, 0.522f);

    // Browns
    public static final Color SaddleBrown = new Color(0.545f, 0.271f, 0.075f);
    public static final Color Sienna      = new Color(0.627f, 0.322f, 0.176f);
    public static final Color Chocolate   = new Color(0.824f, 0.412f, 0.118f);
    public static final Color Peru        = new Color(0.804f, 0.522f, 0.247f);
    public static final Color Tan         = new Color(0.824f, 0.706f, 0.549f);
    public static final Color Wheat       = new Color(0.961f, 0.871f, 0.702f);
    public static final Color Beige       = new Color(0.961f, 0.961f, 0.863f);

    // Miscellaneous
    public static final Color Transparent = new Color(0.000f, 0.000f, 0.000f, 0.000f);
    public static final Color Clear       = Transparent;
    public static final Color Ivory       = new Color(1.000f, 1.000f, 0.941f);
    public static final Color Snow        = new Color(1.000f, 0.980f, 0.980f);
    public static final Color WhiteSmoke  = new Color(0.961f, 0.961f, 0.961f);
    public static final Color GhostWhite  = new Color(0.973f, 0.973f, 1.000f);
    public static final Color AliceBlue   = new Color(0.941f, 0.973f, 1.000f);
    public static final Color FloralWhite = new Color(1.000f, 0.980f, 0.941f);
    public static final Color Azure       = new Color(0.941f, 1.000f, 1.000f);
    public static final Color OldLace     = new Color(0.992f, 0.961f, 0.902f);
}