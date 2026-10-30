package com.family.kids;

import java.util.Calendar;

/**
 * The five worlds, one a day, each with its own colours, background clip and characters.
 * The clips and any character pictures live on the box (Movies/KidsCorner, Pictures/KidsCorner), not in this app.
 */
final class Worlds {
    static final class World {
        final String id, name, clip; final int[] sky; final int accent, body, bodyDark; final Character[] cast;
        World(String id, String name, String clip, int[] sky, int accent, int body, int bodyDark, Character... cast) {
            this.id = id; this.name = name; this.clip = clip; this.sky = sky;
            this.accent = accent; this.body = body; this.bodyDark = bodyDark; this.cast = cast;
        }
    }

    static final class Character {
        final String name, from, fact;
        Character(String name, String from, String fact) { this.name = name; this.from = from; this.fact = fact; }
    }

    static final World[] ALL = {
            new World("power", "Power Up World", "power-up",
                    new int[]{0xFFFF9A1F, 0xFFC2410C, 0xFF2A1240}, 0xFFFFC233, 0xFFFFB238, 0xFFE07B12,
                    new Character("Goku", "Dragon Ball", "He learned the Kamehameha on his very first try, just by watching Master Roshi do it once."),
                    new Character("Vegeta", "Dragon Ball", "He is the Prince of all Saiyans, and he never lets anyone forget it!"),
                    new Character("Gohan", "Dragon Ball", "Goku's son loves studying as much as fighting. He wants to be a scholar."),
                    new Character("Piccolo", "Dragon Ball", "Namekians only drink water, and Piccolo can grow back an arm in seconds."),
                    new Character("Krillin", "Dragon Ball", "Goku's best friend has no nose at all, and he is one of the bravest humans on Earth."),
                    new Character("Beerus", "Dragon Ball Super", "The God of Destruction loves tasty Earth food and naps that last for years."),
                    new Character("Whis", "Dragon Ball Super", "Beerus's teacher can rewind time by three minutes to fix a mistake.")),
            new World("mushroom", "Mushroom Hills", "mushroom-hills",
                    new int[]{0xFF7EC8FF, 0xFF3D9BE9, 0xFF2B6F2F}, 0xFFFFD84A, 0xFFE8463A, 0xFFB52A22,
                    new Character("Mario", "Super Mario", "His very first game was Donkey Kong in 1981, where he was called Jumpman."),
                    new Character("Luigi", "Super Mario", "Mario's younger brother is taller and can jump even higher."),
                    new Character("Yoshi", "Super Mario", "He can flutter his feet in the air to stay up longer, and eats almost anything with his long tongue."),
                    new Character("Princess Peach", "Super Mario", "She rules the Mushroom Kingdom and can float through the air with her dress."),
                    new Character("Toad", "Super Mario", "That's not a hat! The mushroom on Toad's head is part of him."),
                    new Character("Bowser", "Super Mario", "The King of the Koopas has a spiky shell and breathes fire.")),
            new World("speed", "Speed Loop", "speed-loop",
                    new int[]{0xFF4FB4FF, 0xFF1565D8, 0xFF0B2F6B}, 0xFFFFD21F, 0xFF2F7BFF, 0xFF1B4FC4,
                    new Character("Sonic", "Sonic the Hedgehog", "He can run faster than the speed of sound, and his favourite food is chili dogs."),
                    new Character("Tails", "Sonic the Hedgehog", "He spins his two tails like a helicopter to fly, and he builds his own planes."),
                    new Character("Knuckles", "Sonic the Hedgehog", "He guards the giant Master Emerald, and he can glide and climb walls."),
                    new Character("Amy Rose", "Sonic the Hedgehog", "She carries a huge Piko Piko Hammer and is Sonic's biggest fan."),
                    new Character("Shadow", "Sonic the Hedgehog", "He skates on hover shoes and can be just as fast as Sonic.")),
            new World("dream", "Dream Sky", "dream-sky",
                    new int[]{0xFFFFC7E6, 0xFFC9A6FF, 0xFF7AB8FF}, 0xFFFFE066, 0xFFFF8FC8, 0xFFE0559C,
                    new Character("Kirby", "Kirby", "He can swallow enemies and copy their powers, and he lives in Dream Land on Planet Popstar."),
                    new Character("King Dedede", "Kirby", "The self-declared king of Dream Land swings a giant hammer and loves to eat."),
                    new Character("Meta Knight", "Kirby", "He always wears his mask, and his cape can turn into bat-like wings."),
                    new Character("Waddle Dee", "Kirby", "These little helpers are everywhere in Dream Land. Most of them carry spears or parasols.")),
            new World("block", "Block World", "block-world",
                    new int[]{0xFF8FC9FF, 0xFF5AA0E6, 0xFF5B8C2A}, 0xFF7CFC6A, 0xFF5DBB46, 0xFF3C8A2E,
                    new Character("Steve", "Minecraft", "He can build almost anything one block at a time, from a dirt hut to a castle."),
                    new Character("Creeper", "Minecraft", "The Creeper was made by accident, when a pig model came out tall instead of long."),
                    new Character("Enderman", "Minecraft", "Endermen pick up blocks and carry them around. Don't look them in the eye!"),
                    new Character("Axolotl", "Minecraft", "Axolotls come in five colours. The blue one is super rare."),
                    new Character("Iron Golem", "Minecraft", "Iron Golems protect villages, and sometimes they hand a poppy to a villager child.")),
    };

    static int dayIndex() { return Calendar.getInstance(Rules.KUWAIT).get(Calendar.DAY_OF_YEAR); }

    /** The parent can pin one world; otherwise it changes every day. */
    static World today(int pinned) {
        if (pinned >= 0 && pinned < ALL.length) return ALL[pinned];
        return ALL[dayIndex() % ALL.length];
    }

    /** A new character each time the world comes round again (or every day when one world is pinned). */
    static Character characterOfTheDay(World w, boolean pinned) {
        return w.cast[(pinned ? dayIndex() : dayIndex() / ALL.length) % w.cast.length];
    }
}
