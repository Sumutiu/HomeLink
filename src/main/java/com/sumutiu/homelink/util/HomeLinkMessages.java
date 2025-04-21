package com.sumutiu.homelink.util;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class HomeLinkMessages {

    public static Text prefix(String message) {
        return Text.literal("[HomeLink]: ")
                .styled(style -> style.withColor(Formatting.GREEN))
                .append(Text.literal(message).styled(s -> s.withColor(Formatting.WHITE)));
    }
}