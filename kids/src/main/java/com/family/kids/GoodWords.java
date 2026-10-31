package com.family.kids;

/**
 * One good word a day for the children: on even days a beautiful name of Allah,
 * on odd days a short hadith. Each has a simple picture (an emoji) and a kid-sized meaning.
 */
final class GoodWords {
    static final class Name {
        final String pic, arabic, name, meaning, forKids;
        Name(String pic, String arabic, String name, String meaning, String forKids) {
            this.pic = pic; this.arabic = arabic; this.name = name; this.meaning = meaning; this.forKids = forKids;
        }
    }

    static final class Hadith {
        final String pic, arabic, english, source;
        Hadith(String pic, String arabic, String english, String source) {
            this.pic = pic; this.arabic = arabic; this.english = english; this.source = source;
        }
    }

    static final Name[] NAMES = {
            new Name("🌈", "الرَّحْمَٰنُ", "Ar-Rahman", "The Most Kind", "Allah is kind to everyone and everything: people, animals, even tiny ants."),
            new Name("💗", "الرَّحِيمُ", "Ar-Raheem", "The Most Merciful", "Allah loves to forgive us when we say sorry."),
            new Name("👑", "الْمَلِكُ", "Al-Malik", "The King", "Allah is the King of everything. Every king on Earth is small next to Him."),
            new Name("🕊️", "السَّلَامُ", "As-Salam", "The Source of Peace", "Peace comes from Allah. That's why we say Assalamu alaikum."),
            new Name("🌍", "الْخَالِقُ", "Al-Khaliq", "The Creator", "Allah made the sun, the sea, the stars, and you!"),
            new Name("🎨", "الْمُصَوِّرُ", "Al-Musawwir", "The Shaper", "Allah gave every flower, fish and face its own beautiful shape."),
            new Name("🍎", "الرَّزَّاقُ", "Ar-Razzaq", "The Provider", "Every bite of food, from a bird's seed to your dinner, comes from Allah."),
            new Name("📚", "الْعَلِيمُ", "Al-Aleem", "The All-Knowing", "Allah knows everything, even what we are thinking."),
            new Name("👂", "السَّمِيعُ", "As-Samee'", "The All-Hearing", "Allah hears every dua, even a tiny whisper."),
            new Name("👀", "الْبَصِيرُ", "Al-Baseer", "The All-Seeing", "Allah sees everything, even a black ant on a black rock at night."),
            new Name("🌸", "اللَّطِيفُ", "Al-Lateef", "The Gentle", "Allah takes care of us in gentle ways we don't even notice."),
            new Name("🤲", "الْغَفُورُ", "Al-Ghafoor", "The Forgiving", "When we make a mistake and say sorry to Allah, He forgives us."),
            new Name("🌟", "الشَّكُورُ", "Ash-Shakoor", "The Appreciative", "Allah rewards every good deed, even the smallest one."),
            new Name("🎁", "الْكَرِيمُ", "Al-Kareem", "The Generous", "Allah gives us so much, more than we could ever count."),
            new Name("🤗", "الْوَدُودُ", "Al-Wadood", "The Loving", "Allah loves people who are kind and do good."),
            new Name("🛡️", "الْحَفِيظُ", "Al-Hafeedh", "The Protector", "Allah keeps us safe. Say Bismillah before you start anything."),
            new Name("💡", "النُّورُ", "An-Noor", "The Light", "Allah is the Light of the heavens and the Earth."),
            new Name("💪", "الْقَوِيُّ", "Al-Qawiyy", "The Strong", "Allah is stronger than anything. Nothing is too hard for Him."),
            new Name("🦉", "الْحَكِيمُ", "Al-Hakeem", "The Wise", "Everything Allah does has a wise reason, even when we don't understand it yet."),
            new Name("💬", "الْمُجِيبُ", "Al-Mujeeb", "The One Who Answers", "When we make dua, Allah listens and answers in the best way."),
            new Name("🐢", "الصَّبُورُ", "As-Saboor", "The Patient", "Allah is patient with us, so we try to be patient too."),
            new Name("☝️", "الْوَاحِدُ", "Al-Wahid", "The One", "There is only one Allah, and no one is like Him."),
    };

    static final Hadith[] HADITHS = {
            new Hadith("😊", "تَبَسُّمُكَ فِي وَجْهِ أَخِيكَ لَكَ صَدَقَةٌ", "Smiling at your brother is a charity.", "Tirmidhi"),
            new Hadith("💬", "الْكَلِمَةُ الطَّيِّبَةُ صَدَقَةٌ", "A kind word is a charity.", "Bukhari & Muslim"),
            new Hadith("🧊", "لَا تَغْضَبْ", "Don't get angry! (He ﷺ said it three times to a man who asked for advice.)", "Bukhari"),
            new Hadith("💪", "لَيْسَ الشَّدِيدُ بِالصُّرَعَةِ", "The strong one is not the best wrestler, but the one who stays calm when angry.", "Bukhari & Muslim"),
            new Hadith("🫧", "الطُّهُورُ شَطْرُ الْإِيمَانِ", "Being clean is half of faith.", "Muslim"),
            new Hadith("🎁", "تَهَادَوْا تَحَابُّوا", "Give each other gifts, and you will love each other.", "Al-Adab al-Mufrad"),
            new Hadith("🌈", "يَسِّرُوا وَلَا تُعَسِّرُوا", "Make things easy for people, not hard.", "Bukhari & Muslim"),
            new Hadith("🌸", "إِنَّ اللَّهَ رَفِيقٌ يُحِبُّ الرِّفْقَ", "Allah is gentle and loves gentleness.", "Muslim"),
            new Hadith("🤝", "لَا يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لِأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ", "Love for your brother and sister what you love for yourself.", "Bukhari & Muslim"),
            new Hadith("🏠", "خَيْرُكُمْ خَيْرُكُمْ لِأَهْلِهِ", "The best of you are the best to their family.", "Tirmidhi"),
            new Hadith("🙏", "مَنْ لَا يَشْكُرُ النَّاسَ لَا يَشْكُرُ اللَّهَ", "Whoever doesn't thank people doesn't thank Allah.", "Abu Dawud & Tirmidhi"),
            new Hadith("🍽️", "سَمِّ اللَّهَ، وَكُلْ بِيَمِينِكَ، وَكُلْ مِمَّا يَلِيكَ", "Say Bismillah, eat with your right hand, and eat from what is in front of you.", "Bukhari & Muslim"),
            new Hadith("🐢", "أَحَبُّ الْأَعْمَالِ إِلَى اللَّهِ أَدْوَمُهَا وَإِنْ قَلَّ", "The good deeds Allah loves most are the ones we do every day, even small ones.", "Bukhari & Muslim"),
            new Hadith("💗", "مَنْ لَا يَرْحَمُ لَا يُرْحَمُ", "Whoever is not kind and merciful will not be shown mercy.", "Bukhari & Muslim"),
            new Hadith("👋", "أَفْشُوا السَّلَامَ بَيْنَكُمْ", "Say salam to each other a lot.", "Muslim"),
            new Hadith("🕊️", "الْمُسْلِمُ مَنْ سَلِمَ الْمُسْلِمُونَ مِنْ لِسَانِهِ وَيَدِهِ", "A Muslim is someone whose words and hands never hurt others.", "Bukhari & Muslim"),
    };

    static boolean nameDay() { return Worlds.dayIndex() % 2 == 0; }
    static Name name() { return NAMES[(Worlds.dayIndex() / 2) % NAMES.length]; }
    static Hadith hadith() { return HADITHS[(Worlds.dayIndex() / 2) % HADITHS.length]; }
}
