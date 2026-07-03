package eu.pb4.warps.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum Privacy implements StringRepresentable {
    PUBLIC("public"),
    PRIVATE("private");

    public static final Codec<Privacy> CODEC = StringRepresentable.fromEnum(Privacy::values);

    private final String name;

    Privacy(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
