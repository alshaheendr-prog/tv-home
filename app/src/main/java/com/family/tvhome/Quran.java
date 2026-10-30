package com.family.tvhome;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/** Surah names and the reciters the Quran player picks from (mp3quran.net, Hafs murattal, never Mishary Alafasy). */
final class Quran {
    private Quran() { }

    static final String[] AR = {"الفاتحة", "البقرة", "آل عمران", "النساء", "المائدة", "الأنعام", "الأعراف", "الأنفال", "التوبة", "يونس",
            "هود", "يوسف", "الرعد", "إبراهيم", "الحجر", "النحل", "الإسراء", "الكهف", "مريم", "طه",
            "الأنبياء", "الحج", "المؤمنون", "النور", "الفرقان", "الشعراء", "النمل", "القصص", "العنكبوت", "الروم",
            "لقمان", "السجدة", "الأحزاب", "سبأ", "فاطر", "يس", "الصافات", "ص", "الزمر", "غافر",
            "فصلت", "الشورى", "الزخرف", "الدخان", "الجاثية", "الأحقاف", "محمد", "الفتح", "الحجرات", "ق",
            "الذاريات", "الطور", "النجم", "القمر", "الرحمن", "الواقعة", "الحديد", "المجادلة", "الحشر", "الممتحنة",
            "الصف", "الجمعة", "المنافقون", "التغابن", "الطلاق", "التحريم", "الملك", "القلم", "الحاقة", "المعارج",
            "نوح", "الجن", "المزمل", "المدثر", "القيامة", "الإنسان", "المرسلات", "النبأ", "النازعات", "عبس",
            "التكوير", "الانفطار", "المطففين", "الانشقاق", "البروج", "الطارق", "الأعلى", "الغاشية", "الفجر", "البلد",
            "الشمس", "الليل", "الضحى", "الشرح", "التين", "العلق", "القدر", "البينة", "الزلزلة", "العاديات",
            "القارعة", "التكاثر", "العصر", "الهمزة", "الفيل", "قريش", "الماعون", "الكوثر", "الكافرون", "النصر",
            "المسد", "الإخلاص", "الفلق", "الناس"};
    static final String[] EN = {"Al-Fatihah", "Al-Baqarah", "Al Imran", "An-Nisa", "Al-Ma'idah", "Al-An'am", "Al-A'raf", "Al-Anfal", "At-Tawbah", "Yunus",
            "Hud", "Yusuf", "Ar-Ra'd", "Ibrahim", "Al-Hijr", "An-Nahl", "Al-Isra", "Al-Kahf", "Maryam", "Taha",
            "Al-Anbiya", "Al-Hajj", "Al-Mu'minun", "An-Nur", "Al-Furqan", "Ash-Shu'ara", "An-Naml", "Al-Qasas", "Al-Ankabut", "Ar-Rum",
            "Luqman", "As-Sajdah", "Al-Ahzab", "Saba", "Fatir", "Ya-Sin", "As-Saffat", "Sad", "Az-Zumar", "Ghafir",
            "Fussilat", "Ash-Shura", "Az-Zukhruf", "Ad-Dukhan", "Al-Jathiyah", "Al-Ahqaf", "Muhammad", "Al-Fath", "Al-Hujurat", "Qaf",
            "Adh-Dhariyat", "At-Tur", "An-Najm", "Al-Qamar", "Ar-Rahman", "Al-Waqi'ah", "Al-Hadid", "Al-Mujadilah", "Al-Hashr", "Al-Mumtahanah",
            "As-Saff", "Al-Jumu'ah", "Al-Munafiqun", "At-Taghabun", "At-Talaq", "At-Tahrim", "Al-Mulk", "Al-Qalam", "Al-Haqqah", "Al-Ma'arij",
            "Nuh", "Al-Jinn", "Al-Muzzammil", "Al-Muddaththir", "Al-Qiyamah", "Al-Insan", "Al-Mursalat", "An-Naba", "An-Nazi'at", "Abasa",
            "At-Takwir", "Al-Infitar", "Al-Mutaffifin", "Al-Inshiqaq", "Al-Buruj", "At-Tariq", "Al-A'la", "Al-Ghashiyah", "Al-Fajr", "Al-Balad",
            "Ash-Shams", "Al-Layl", "Ad-Duha", "Ash-Sharh", "At-Tin", "Al-Alaq", "Al-Qadr", "Al-Bayyinah", "Az-Zalzalah", "Al-Adiyat",
            "Al-Qari'ah", "At-Takathur", "Al-Asr", "Al-Humazah", "Al-Fil", "Quraysh", "Al-Ma'un", "Al-Kawthar", "Al-Kafirun", "An-Nasr",
            "Al-Masad", "Al-Ikhlas", "Al-Falaq", "An-Nas"};

    static final class Reciter {
        final String en, ar, server;
        Reciter(String en, String ar, String server) { this.en = en; this.ar = ar; this.server = server; }
        String url(int surah) { return server + String.format(Locale.US, "%03d.mp3", surah); }
    }

    /** Used until the full list has been downloaded once. */
    static final Reciter[] FALLBACK = {
            new Reciter("Mahmoud Khalil Al-Hussary", "محمود خليل الحصري", "https://cdn.mp3quran.net/audio/mahmoud-husary/r1/"),
            new Reciter("Mohammed Siddiq Al-Minshawi", "محمد صديق المنشاوي", "https://cdn.mp3quran.net/audio/muhammad-minshawi/r1/"),
            new Reciter("Maher Al-Muaiqly", "ماهر المعيقلي", "https://cdn.mp3quran.net/audio/maher-muaiqly/r1/"),
            new Reciter("Idrees Abkar", "إدريس أبكر", "https://cdn.mp3quran.net/audio/idrees-abkar/r1/"),
            new Reciter("Mohammed Ayyub", "محمد أيوب", "https://cdn.mp3quran.net/audio/muhammad-ayyub/r1/"),
    };

    static boolean excluded(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.contains("afasy") || n.contains("afasi") || n.contains("عفاسي");
    }

    private static List<Reciter> list;

    /** Every full-Quran Hafs murattal reciter on mp3quran.net, cached on the box. Call off the UI thread. */
    static synchronized List<Reciter> reciters(File cacheDir) {
        if (list != null) return list;
        File en = new File(cacheDir, "reciters_eng.json"), ar = new File(cacheDir, "reciters_ar.json");
        try {
            if (!en.exists() || System.currentTimeMillis() - en.lastModified() > 7L * 86400000) {
                Files.write(en.toPath(), MainActivity.get("https://mp3quran.net/api/v3/reciters?language=eng").getBytes(StandardCharsets.UTF_8));
                Files.write(ar.toPath(), MainActivity.get("https://mp3quran.net/api/v3/reciters?language=ar").getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) { }
        List<Reciter> out = new ArrayList<>();
        try {
            Map<Integer, String> arNames = new HashMap<>();
            try {
                JSONArray a = new JSONObject(new String(Files.readAllBytes(ar.toPath()), StandardCharsets.UTF_8)).getJSONArray("reciters");
                for (int i = 0; i < a.length(); i++) arNames.put(a.getJSONObject(i).getInt("id"), a.getJSONObject(i).getString("name"));
            } catch (Exception ignored) { }
            JSONArray r = new JSONObject(new String(Files.readAllBytes(en.toPath()), StandardCharsets.UTF_8)).getJSONArray("reciters");
            for (int i = 0; i < r.length(); i++) {
                JSONObject o = r.getJSONObject(i);
                String name = o.getString("name"), arName = arNames.containsKey(o.getInt("id")) ? arNames.get(o.getInt("id")) : "";
                if (excluded(name) || excluded(arName)) continue;
                JSONArray ms = o.getJSONArray("moshaf");
                for (int j = 0; j < ms.length(); j++) {
                    JSONObject m = ms.getJSONObject(j);
                    if (m.optInt("rewaya_id") != 1 || m.optInt("surah_total") != 114) continue;
                    if (!m.optString("name").toLowerCase(Locale.ROOT).contains("murattal")) continue;
                    String server = m.getString("server");
                    if (excluded(server)) continue;
                    if (!server.endsWith("/")) server += "/";
                    out.add(new Reciter(name, arName, server));
                    break;
                }
            }
        } catch (Exception ignored) { }
        if (out.size() < 5) { out.clear(); for (Reciter f : FALLBACK) out.add(f); }
        list = out;
        return out;
    }

    /** Recitations copied to the box by the PC's "Download Quran" script: Music/Quran/English name - Arabic name/001.mp3 */
    static List<Reciter> local(int surah) {
        List<Reciter> out = new ArrayList<>();
        File root = new File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC), "Quran");
        File[] dirs = root.listFiles(File::isDirectory);
        if (dirs == null) return out;
        for (File d : dirs) {
            if (excluded(d.getName())) continue;
            if (!new File(d, String.format(Locale.US, "%03d.mp3", surah)).canRead()) continue;
            // folder name: "English name (mp3quran id)"; the Arabic name comes from the cached reciter list
            String name = d.getName(), ar = "";
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(.*)\\((\\d+)\\)\\s*$").matcher(name);
            if (m.matches()) { name = m.group(1).trim(); String a = arabicName(Integer.parseInt(m.group(2))); if (a != null) ar = a; }
            out.add(new Reciter(name, ar, Uri.fromFile(d).toString() + "/"));
        }
        return out;
    }

    private static Map<Integer, String> arNames;
    static File cacheDir;

    private static synchronized String arabicName(int id) {
        if (arNames == null) {
            arNames = new HashMap<>();
            try {
                JSONArray a = new JSONObject(new String(Files.readAllBytes(new File(cacheDir, "reciters_ar.json").toPath()), StandardCharsets.UTF_8)).getJSONArray("reciters");
                for (int i = 0; i < a.length(); i++) arNames.put(a.getJSONObject(i).getInt("id"), a.getJSONObject(i).getString("name"));
            } catch (Exception ignored) { }
        }
        return arNames.get(id);
    }

    static Reciter pick(List<Reciter> all, Reciter not, Random random) {
        for (int i = 0; i < 10; i++) {
            Reciter r = all.get(random.nextInt(all.size()));
            if (not == null || !r.server.equals(not.server)) return r;
        }
        return all.get(0);
    }
}
