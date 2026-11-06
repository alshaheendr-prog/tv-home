package com.family.tvhome;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.TypefaceSpan;
import android.widget.TextView;

/**
 * A Latin (Inter) label that may hold an Arabic half, such as "VERSE OF THE DAY · آية اليوم". Inter has no Arabic,
 * so Android would fill it from a system Naskh in another weight; here every Arabic run is drawn in IBM Plex Arabic,
 * the family's Arabic face. Whole runs only (from the first Arabic letter to the last, with the spaces between), so
 * letters are never split and always join.
 */
class MixText extends TextView {
    static Typeface arabic, arabicSemi, quran;
    private boolean semi, ours;

    MixText(Context c) { super(c); }

    void boldArabic(boolean b) { semi = b; }

    /** A view switched to an Arabic face (the Quran title in Amiri) keeps that face for its Arabic: drop our spans. */
    @Override
    public void setTypeface(Typeface tf) {
        super.setTypeface(tf);
        if (ours && arabicFace(tf)) setText(getText().toString());
    }

    private static boolean arabicFace(Typeface tf) { return tf != null && (tf == arabic || tf == arabicSemi || tf == quran); }

    @Override
    public void setText(CharSequence text, BufferType type) {
        ours = false;
        if (arabic == null || text == null || text instanceof Spanned || !hasArabic(text) || arabicFace(getTypeface())) { super.setText(text, type); return; }
        SpannableString s = new SpannableString(text);
        Typeface face = semi && arabicSemi != null ? arabicSemi : arabic;
        int i = 0, n = text.length();
        while (i < n) {
            if (!isArabic(text.charAt(i))) { i++; continue; }
            int end = i;
            for (int j = i; j < n; j++) {
                char ch = text.charAt(j);
                if (isArabic(ch)) end = j + 1;
                else if (!(ch == ' ' || Character.isDigit(ch) || ch == '،' || ch == '؟')) break; // stop at Latin letters and punctuation
            }
            s.setSpan(new TypefaceSpan(face), i, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            i = end;
        }
        ours = true;
        super.setText(s, BufferType.SPANNABLE);
    }

    static boolean isArabic(char c) {
        return (c >= '؀' && c <= 'ۿ') || (c >= 'ݐ' && c <= 'ݿ') || (c >= 'ﭐ' && c <= '﷿') || (c >= 'ﹰ' && c <= '﻿');
    }

    private static boolean hasArabic(CharSequence t) {
        for (int i = 0; i < t.length(); i++) if (isArabic(t.charAt(i))) return true;
        return false;
    }
}
