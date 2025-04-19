package com.floweytf.fma.features.cz.data;

import net.minecraft.network.chat.Component;

import static com.floweytf.fma.util.FormatUtil.literal;

public enum ZenithAbility {
    FIREWORK_BLAST("Firework Blast", ZenithClass.STEELSAGE),
    FOCUSED_COMBOS("Focused Combos", ZenithClass.STEELSAGE),
    GRAVITY_BOMB("Gravity Bomb", ZenithClass.STEELSAGE),
    PRECISION_STRIKE("Precision Strike", ZenithClass.STEELSAGE),
    RAPID_FIRE("Rapid Fire", ZenithClass.STEELSAGE),
    SCRAPSHOT("Scrapshot", ZenithClass.STEELSAGE),
    SIDEARM("Sidearm", ZenithClass.STEELSAGE),
    STEEL_STALLION("Steel Stallion", ZenithClass.STEELSAGE),
    SHARPSHOOTER("Sharpshooter", ZenithClass.STEELSAGE),
    SPLIT_ARROW("Split Arrow", ZenithClass.STEELSAGE),
    VOLLEY("Volley", ZenithClass.STEELSAGE),
    AEROBLAST("Aeroblast", ZenithClass.WINDWALKER),
    AEROMANCY("Aeromancy", ZenithClass.WINDWALKER),
    DODGING("Dodging", ZenithClass.WINDWALKER),
    GUARDING_BOLT("Guarding Bolt", ZenithClass.WINDWALKER),
    THUNDERCLOUD_FORM("Thundercloud Form", ZenithClass.WINDWALKER),
    LAST_BREATH("Last Breath", ZenithClass.WINDWALKER),
    ONE_WITH_THE_WIND("One with the Wind", ZenithClass.WINDWALKER),
    RESTORING_DRAFT("Restoring Draft", ZenithClass.WINDWALKER),
    SKYHOOK("Skyhook", ZenithClass.WINDWALKER),
    WIND_WALK("Wind Walk", ZenithClass.WINDWALKER),
    WHIRLWIND("Whirlwind", ZenithClass.WINDWALKER),
    WINDSWEPT_COMBOS("Windswept Combos", ZenithClass.WINDWALKER),
    ADVANCING_SHADOWS("Advancing Shadows", ZenithClass.SHADOWDANCER),
    BLADE_FLURRY("Blade Flurry", ZenithClass.SHADOWDANCER),
    BRUTALIZE("Brutalize", ZenithClass.SHADOWDANCER),
    CHAOS_DAGGER("Chaos Dagger", ZenithClass.SHADOWDANCER),
    DARK_COMBOS("Dark Combos", ZenithClass.SHADOWDANCER),
    DEADLY_STRIKE("Deadly Strike", ZenithClass.SHADOWDANCER),
    DETHRONER("Dethroner", ZenithClass.SHADOWDANCER),
    DUMMY_DECOY("Dummy Decoy", ZenithClass.SHADOWDANCER),
    CLOAK_OF_SHADOWS("Cloak of Shadows", ZenithClass.SHADOWDANCER),
    SHADOW_SLAM("Shadow Slam", ZenithClass.SHADOWDANCER),
    ESCAPE_ARTIST("Escape Artist", ZenithClass.SHADOWDANCER),
    PHANTOM_FORCE("Phantom Force", ZenithClass.SHADOWDANCER),
    BOTTLED_SUNLIGHT("Bottled Sunlight", ZenithClass.DAWNBRINGER),
    ENLIGHTENMENT("Enlightenment", ZenithClass.DAWNBRINGER),
    LIGHTNING_BOTTLE("Lightning Bottle", ZenithClass.DAWNBRINGER),
    RADIANT_BLESSING("Radiant Blessing", ZenithClass.DAWNBRINGER),
    REJUVENATION("Rejuvenation", ZenithClass.DAWNBRINGER),
    SOOTHING_COMBOS("Soothing Combos", ZenithClass.DAWNBRINGER),
    SUNDROPS("Sundrops", ZenithClass.DAWNBRINGER),
    WARD_OF_LIGHT("Ward of Light", ZenithClass.DAWNBRINGER),
    DIVINE_BEAM("Divine Beam", ZenithClass.DAWNBRINGER),
    ETERNAL_SAVIOR("Eternal Savior", ZenithClass.DAWNBRINGER),
    SPARK_OF_INSPIRATION("Spark of Inspiration", ZenithClass.DAWNBRINGER),
    APOCALYPSE("Apocalypse", ZenithClass.FLAMECALLER),
    DETONATION("Detonation", ZenithClass.FLAMECALLER),
    FIREBALL("Fireball", ZenithClass.FLAMECALLER),
    FLAME_SPIRIT("Flame Spirit", ZenithClass.FLAMECALLER),
    FLAMESTRIKE("Flamestrike", ZenithClass.FLAMECALLER),
    PRIMORDIAL_MASTERY("Primordial Mastery", ZenithClass.FLAMECALLER),
    PYROBLAST("Pyroblast", ZenithClass.FLAMECALLER),
    PYROMANIA("Pyromania", ZenithClass.FLAMECALLER),
    VOLCANIC_COMBOS("Volcanic Combos", ZenithClass.FLAMECALLER),
    VOLCANIC_METEOR("Volcanic Meteor", ZenithClass.FLAMECALLER),
    IGNEOUS_RUNE("Igneous Rune", ZenithClass.FLAMECALLER),
    AVALANCHE("Avalanche", ZenithClass.FROSTBORN),
    CRYOBOX("Cryobox", ZenithClass.FROSTBORN),
    FRIGID_COMBOS("Frigid Combos", ZenithClass.FROSTBORN),
    SNOWSTORM("Snowstorm", ZenithClass.FROSTBORN),
    FROZEN_DOMAIN("Frozen Domain", ZenithClass.FROSTBORN),
    ICE_BARRIER("Ice Barrier", ZenithClass.FROSTBORN),
    ICEBREAKER("Icebreaker", ZenithClass.FROSTBORN),
    ICE_LANCE("Ice Lance", ZenithClass.FROSTBORN),
    PERMAFROST("Permafrost", ZenithClass.FROSTBORN),
    PIERCING_COLD("Piercing Cold", ZenithClass.FROSTBORN),
    BRAMBLE_SHELL("Bramble Shell", ZenithClass.EARTHBOUND),
    BULWARK("Bulwark", ZenithClass.EARTHBOUND),
    CRUSHING_EARTH("Crushing Earth", ZenithClass.EARTHBOUND),
    TOUGHNESS("Toughness", ZenithClass.EARTHBOUND),
    EARTHEN_COMBOS("Earthen Combos", ZenithClass.EARTHBOUND),
    EARTHEN_WRATH("Earthen Wrath", ZenithClass.EARTHBOUND),
    EARTHQUAKE("Earthquake", ZenithClass.EARTHBOUND),
    ENTRENCH("Entrench", ZenithClass.EARTHBOUND),
    IRON_GRIP("Iron Grip", ZenithClass.EARTHBOUND),
    TAUNT("Taunt", ZenithClass.EARTHBOUND);

    public final String displayName;
    public final ZenithClass zenithClass;
    public final Component coloredName;

    ZenithAbility(String displayName, ZenithClass zenithClass) {
        this.displayName = displayName;
        this.zenithClass = zenithClass;
        coloredName = literal(displayName, zenithClass.color);
    }
}
