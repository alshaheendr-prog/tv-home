package com.family.adhan;

/** Built-in texts for the widget card. Hadith are from Sahih al-Bukhari, Sahih Muslim and Jami' at-Tirmidhi. */
final class Content {
    private Content() { }

    /** {Arabic, English, reference} */
    static final String[][] HADITH = {
            {"إنما الأعمال بالنيات", "Actions are judged by intentions.", "Bukhari & Muslim"},
            {"الكلمة الطيبة صدقة", "A good word is charity.", "Bukhari & Muslim"},
            {"من كان يؤمن بالله واليوم الآخر فليقل خيرا أو ليصمت", "Whoever believes in Allah and the Last Day, let him speak good or stay silent.", "Bukhari & Muslim"},
            {"تبسمك في وجه أخيك لك صدقة", "Your smile in the face of your brother is charity.", "Tirmidhi"},
            {"لا يؤمن أحدكم حتى يحب لأخيه ما يحب لنفسه", "None of you truly believes until he loves for his brother what he loves for himself.", "Bukhari & Muslim"},
            {"خيركم من تعلم القرآن وعلمه", "The best of you are those who learn the Qur'an and teach it.", "Bukhari"},
            {"الطهور شطر الإيمان", "Purity is half of faith.", "Muslim"},
            {"يسروا ولا تعسروا وبشروا ولا تنفروا", "Make things easy and do not make them hard. Give good news and do not drive people away.", "Bukhari & Muslim"},
            {"المسلم من سلم المسلمون من لسانه ويده", "A Muslim is one from whose tongue and hand other Muslims are safe.", "Bukhari & Muslim"},
            {"أحب الأعمال إلى الله أدومها وإن قل", "The deeds most loved by Allah are those done regularly, even if small.", "Bukhari & Muslim"},
    };

    /** Offline verses, used when the box can't reach the Qur'an service. {Arabic, English (Sahih International), reference} */
    static final String[][] AYAH = {
            {"إن مع العسر يسرا", "Indeed, with hardship comes ease.", "Ash-Sharh 94:6"},
            {"ألا بذكر الله تطمئن القلوب", "Unquestionably, by the remembrance of Allah hearts are assured.", "Ar-Ra'd 13:28"},
            {"فاذكروني أذكركم", "So remember Me; I will remember you.", "Al-Baqarah 2:152"},
            {"وقل رب زدني علما", "And say, \"My Lord, increase me in knowledge.\"", "Ta-Ha 20:114"},
            {"لا يكلف الله نفسا إلا وسعها", "Allah does not burden a soul beyond what it can bear.", "Al-Baqarah 2:286"},
    };

    /** {franchise, character, Arabic fact, English fact} */
    static final String[][] CHARACTERS = {
            {"Dragon Ball Z", "Goku", "اسم غوكو الحقيقي كساياجين هو كاكاروت.", "Goku's Saiyan birth name is Kakarot."},
            {"Dragon Ball Z", "Vegeta", "فيجيتا هو أمير شعب الساياجين.", "Vegeta is the prince of all Saiyans."},
            {"Dragon Ball Z", "Gohan", "غوهان هو الابن الأول لغوكو، وسُمّي على اسم جده الذي ربّى غوكو.", "Gohan is Goku's first son, named after the grandpa who raised Goku."},
            {"Dragon Ball Z", "Piccolo", "بيكولو من كوكب ناميك، ويستطيع إعادة إنبات ذراعه إذا فقدها.", "Piccolo is a Namekian and can regrow a lost arm."},
            {"Dragon Ball Z", "Frieza", "فريزا يستطيع التحوّل إلى أشكال متعددة، كل شكل أقوى من الذي قبله.", "Frieza has several forms, each stronger than the last."},
            {"Dragon Ball Z", "Future Trunks", "ترانكس جاء من المستقبل بآلة زمن ليحذّر الأبطال.", "Future Trunks travelled back in a time machine to warn the heroes."},
            {"Dragon Ball Z", "Cell", "سيل صُنع من خلايا أقوى المقاتلين، ومنهم غوكو وفيجيتا وبيكولو.", "Cell was built from the cells of great fighters, including Goku, Vegeta and Piccolo."},
            {"Dragon Ball Z", "Majin Buu", "ماجين بو يحب الحلويات، ويستطيع تحويل أعدائه إلى حلوى.", "Majin Buu loves sweets and can turn enemies into candy."},
            {"Dragon Ball Super", "Beerus", "بيروس هو إله الدمار في الكون السابع.", "Beerus is the God of Destruction of Universe 7."},
            {"Dragon Ball Super", "Whis", "ويس هو مرافق بيروس ومعلّمه، ويدرّب غوكو وفيجيتا أيضًا.", "Whis is Beerus's attendant and teacher, and he trains Goku and Vegeta too."},
            {"Dragon Ball Super", "Jiren", "جيرين من الكون الحادي عشر، وكان من أقوى المقاتلين في بطولة القوة.", "Jiren is from Universe 11 and was one of the strongest in the Tournament of Power."},
            {"Dragon Ball Super", "Broly", "برولي ساياجين قوته هائلة، ويزداد قوة كلما غضب.", "Broly is a Saiyan with huge power that grows when he gets angry."},
            {"Dragon Ball Super", "Goku (Ultra Instinct)", "في الغريزة الفائقة يتحرّك جسد غوكو ويتفادى الضربات من تلقاء نفسه.", "In Ultra Instinct, Goku's body dodges and moves on its own."},
            {"Super Mario", "Mario", "ظهر ماريو أول مرة في لعبة دونكي كونغ عام 1981 باسم جمب مان.", "Mario first appeared in Donkey Kong (1981), where he was called Jumpman."},
            {"Super Mario", "Luigi", "لويجي هو الأخ التوأم الأصغر لماريو، وأطول منه.", "Luigi is Mario's younger twin brother, and he's taller."},
            {"Super Mario", "Princess Peach", "الأميرة بيتش تحكم مملكة الفطر.", "Princess Peach rules the Mushroom Kingdom."},
            {"Super Mario", "Bowser", "باوزر هو ملك الكوبا، ودائمًا يحاول خطف الأميرة بيتش.", "Bowser is King of the Koopas and keeps trying to kidnap Princess Peach."},
            {"Super Mario", "Yoshi", "ظهر يوشي أول مرة في لعبة سوبر ماريو وورلد عام 1990.", "Yoshi first appeared in Super Mario World in 1990."},
            {"Super Mario", "Toad", "تود من سكان مملكة الفطر ويخدم الأميرة بيتش.", "Toad lives in the Mushroom Kingdom and serves Princess Peach."},
            {"Kirby", "Kirby", "كيربي يبتلع أعداءه ليأخذ قدراتهم.", "Kirby inhales enemies to copy their powers."},
            {"Kirby", "Meta Knight", "ميتا نايت فارس غامض يرتدي قناعًا ولا يُرى وجهه إلا نادرًا.", "Meta Knight is a masked swordsman whose face is rarely seen."},
            {"Kirby", "King Dedede", "الملك ديديدي نصّب نفسه ملكًا على دريم لاند، ويحمل مطرقة ضخمة.", "King Dedede calls himself the king of Dream Land and swings a giant hammer."},
            {"Kirby", "Waddle Dee", "وادل دي من أكثر الشخصيات ظهورًا في عالم كيربي.", "Waddle Dees are among the most common characters in Kirby's world."},
            {"Sonic", "Sonic", "ظهر سونيك أول مرة عام 1991 على جهاز سيغا ميغا درايف.", "Sonic first appeared in 1991 on the Sega Mega Drive."},
            {"Sonic", "Tails", "اسم تيلز الحقيقي مايلز براور، ويطير بتدوير ذيليه.", "Tails's real name is Miles Prower, and he flies by spinning his two tails."},
            {"Sonic", "Knuckles", "ناكلز هو حارس الزمرّدة الكبرى.", "Knuckles guards the Master Emerald."},
            {"Sonic", "Shadow", "صنع شادو العالِم جيرالد روبوتنيك، جد الدكتور إغمان.", "Shadow was created by Professor Gerald Robotnik, Dr. Eggman's grandfather."},
            {"Sonic", "Amy Rose", "إيمي روز تحمل مطرقة بيكو بيكو الكبيرة.", "Amy Rose carries her big Piko Piko Hammer."},
            {"Sonic", "Dr. Eggman", "الدكتور إغمان عالِم عبقري يبني جيوشًا من الروبوتات.", "Dr. Eggman is a genius scientist who builds robot armies."},
            {"Minecraft", "Steve", "ستيف هو الشخصية الأصلية للاعب في ماين كرافت.", "Steve is Minecraft's original player character."},
            {"Minecraft", "Alex", "أليكس هي الشخصية الثانية للاعب، وأُضيفت عام 2014.", "Alex is the second player character, added in 2014."},
            {"Minecraft", "Creeper", "الكريبر وُلد من خطأ برمجي أثناء صنع الخنزير.", "The Creeper came from a coding mistake while the pig was being made."},
            {"Minecraft", "Enderman", "الإندرمان يغضب إذا نظرت مباشرة إلى عينيه.", "An Enderman gets angry if you look it in the eyes."},
            {"Minecraft", "Ender Dragon", "تنين الإندر هو الزعيم في بُعد النهاية.", "The Ender Dragon is the boss of the End dimension."},
    };
}
