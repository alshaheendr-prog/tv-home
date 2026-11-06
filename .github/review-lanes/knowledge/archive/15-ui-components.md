# Lane 15 knowledge: UI components and states: archive

Past findings and the full source library for this lane. Search this file (grep the area, file or PR) when a review needs history or a source; you don't need to read it in full. New dated Past findings and new sources are appended here after each review.

## Past findings
### PR #23 (2026-11-06, update 2: visual polish)
- 2026-11-06, PR #23 (update 2): no Must. Should: (1) `pressDip` restores the scale it read on DOWN, so OK during the focus grow freezes the item at an in-between size (cancelling a ViewPropertyAnimator drops its end action); (2) the calendar reading-guide fill (8% white, no edge) is about 1.26:1, nearly invisible, and it is the panel's only focusable thing; (3) Kids parent and action rows at 1.03 lose their left and right white edge to the ScrollView clip (home was fixed, Kids not). Nice: "Choose reciter" hides the saved choice; the 10-minute reminder is still 5 s one line at home vs 8 s three lines in other apps; "Playing ✓" goes back to ▶ while the takbir still plays; the Kids dip uses the default curve. Carried over: Kids `launch()` silent, no dock hold-OK hint.

### PR #22 (2026-11-05, audit update 1)
- 2026-11-05, PR #22: no Must. Should: (1) the pill is placed only when `toast()` starts, so a panel opening or closing mid-message leaves it over the All apps header (80,56 vs 110,56) or bottom centre over the home row; (2) the resting home PIN pad's alpha 0.4 also dims the gold focus edge to about 2.6:1 (from 9.8:1); (3) calendar to All apps cuts both ways (apps alpha 1 + `bringToFront` at once on Back, apps `GONE` at once on open). Nice: two wordings for the learn-row "not running" state, focus jumps from "Any reciter" when the list fills in, no offline line for reciters, Kids Right at the last value is silent, PIN line changes height after a PIN mismatch.

- 2026-11-05, PR #21 (docs only): Should: the PIN rest state from PR #20 was missing from the inventory (added, with a States checklist item); "Kids focus inverts, which matches the tv-material default" left out tv-material's 1.1 scale (reworded; Kids one-cue focus added to Known risks). Copy seeds for a later polish PR (hand to lane 10): the Kids setup line says "can't send **him** back" (`KidsActivity.java` ~`:1745`), should be "the kids"; "Too many tries. Wait 30 seconds" lacks its final full stop.

Seed findings from the creation research (not yet reported on a PR; confirm before raising):
- Should: PIN "set" copy "Hold OK on YouTube to change it later" (`MainActivity.java` ~`:2148`) is wrong: holding OK on the YouTube tile arranges the row; the PIN is changed in Parent settings ("Change the YouTube PIN").
- Should: calendar rows are focusable and scale but OK does nothing (~`:3284`).
- Should: no press feedback on most controls (see Facts).
- Should: Kids `launch()` fails silently.
- Nice: 10-minute reminder looks like two different components (home pill vs adhan Baloo card with another gold).
- Nice: `closeAppMenu()` cuts out while the menu faded in.
### Whole-app audit (2026-11-05, main at d772bcc, no PR)
- 2026-11-05, whole-app audit on `d772bcc`: Must: the PIN pad opens behind Parent settings ("Change the YouTube PIN", equal elevation 30, parent added later); the message pill (elevation 0) is hidden under every panel, so All apps, Parent settings and Quran messages are lost; remote-button rows are dead on OK when RemoteKeys isn't running. Should: wrong PIN has no words; resting pad looks live; PIN "set" copy is wrong; calendar rows look like buttons; no press states; reciter list has no loading state; app menu uses GlassFrame, not PanelBg; Kids single-cue focus; Kids value rows ignore OK; the adhan Settings screen is off-family (and mentions the removed Projectivy); the 10-minute reminder is two components; pill time is too short for two sentences. Nice: dock hold-OK hint, "›" on instant actions, player and reciter panels cut on closePanels, Up silent in arrange, stale magnify comment, no Kids PIN success flash. The seed findings from creation are all confirmed on main.


## Source library
All opened 2026-11-05; all tv-material and M3 numbers re-verified 2026-11-05 (PR #21).
- [tv-focus] https://developer.android.com/design/ui/tv/guides/styles/focus-system: states, scale 1.025/1.05/1.1, glow 2 to 32 dp, use-case matrix.
- [tv-buttons] https://developer.android.com/design/ui/tv/guides/components/buttons (also read via the developer.android.google.cn mirror): button types, 1.1 focus scale, shapes, label rules, one primary action.
- [tv-cards] https://developer.android.com/design/ui/tv/guides/components/cards: card variants, aspect ratios, widths, scrim on compact cards.
- [tv-lists] https://developer.android.com/design/ui/tv/guides/components/lists: list items are not buttons, selection by icon not background.
- [design-for-tv] https://developer.android.com/design/ui/tv/guides/foundations/design-for-tv: "instant and distinct feedback" on D-pad presses.
- [tv-navigation] https://developer.android.com/training/tv/start/navigation: uniform focus scheme, padding for highlights, state-list drawables, Back and Cancel rules.
- [tvm-button] https://raw.githubusercontent.com/androidx/androidx/androidx-main/tv/tv-material/src/main/java/androidx/tv/material3/ButtonDefaults.kt: inverted focus colours, 40% disabled, 1.1 scale, min size.
- [tvm-surface] https://raw.githubusercontent.com/androidx/androidx/androidx-main/tv/tv-material/src/main/java/androidx/tv/material3/SurfaceDefaults.kt: surface scales, disabled 0.4, selected 0.5.
- [tvm-listitem] https://raw.githubusercontent.com/androidx/androidx/androidx-main/tv/tv-material/src/main/java/androidx/tv/material3/ListItemDefaults.kt: list item 1.05 scale, selected 0.4, min heights.
- [tvm-card] https://raw.githubusercontent.com/androidx/androidx/androidx-main/tv/tv-material/src/main/java/androidx/tv/material3/Card.kt: card 1.1 scale, 3 dp focus border, text alphas.
- [tvm-iconbutton] https://raw.githubusercontent.com/androidx/androidx/androidx-main/tv/tv-material/src/main/java/androidx/tv/material3/IconButtonDefaults.kt: icon button sizes.
- [m3-state-tokens] https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/StateTokens.kt: state-layer opacities.
- [m3-filled-button-tokens] https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/FilledButtonTokens.kt: disabled opacities.
- [mdc-snackbar] https://raw.githubusercontent.com/material-components/material-components-android/master/docs/components/Snackbar.md: brief messages, one at a time.
- [aosp-view] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/core/java/android/view/View.java: `onKeyDown` sets pressed on confirm keys for clickable views.
- [hig-focus] https://developer.apple.com/design/human-interface-guidelines/focus-and-selection (read via .../tutorials/data/design/human-interface-guidelines/focus-and-selection.json): five tvOS focus states, focus vs selection.
- [hig-buttons] https://developer.apple.com/design/human-interface-guidelines/buttons (JSON): always a press state, one or two prominent buttons, style not size.
- [hig-collections] https://developer.apple.com/design/human-interface-guidelines/collections (JSON): padding for focus effects, animate changes.
- [hig-feedback] https://developer.apple.com/design/human-interface-guidelines/feedback (JSON): match feedback to importance, explain failures.
- [hig-loading] https://developer.apple.com/design/human-interface-guidelines/loading (JSON): show something at once, placeholders, progress.
- [hig-alerts] https://developer.apple.com/design/human-interface-guidelines/alerts (JSON): alerts sparingly, actionable only.
- [hig-tvos] https://developer.apple.com/design/human-interface-guidelines/designing-for-tvos (JSON): focus "gently highlight and expand" items.
- [nng-visibility] https://www.nngroup.com/articles/visibility-system-status/: feedback for every action.
- [nng-response-times] https://www.nngroup.com/articles/response-times-3-important-limits/: 0.1 / 1 / 10 s limits.
- [nng-empty] https://www.nngroup.com/articles/empty-state-interface-design/: empty-state guidelines.
- [nng-errors] https://www.nngroup.com/articles/error-message-guidelines/: visibility, wording, recovery.
- [nng-ivn] https://www.nngroup.com/articles/indicators-validations-notifications/: indicators vs validations vs notifications; toasts poor for errors.
- [nng-clickable] https://www.nngroup.com/articles/clickable-elements/: signifiers for interactive elements.
- [nng-modal] https://www.nngroup.com/articles/modal-nonmodal-dialog/: when to interrupt with a modal.
- [nng-consistency] https://www.nngroup.com/articles/consistency-and-standards/: internal and external consistency.
- [frost-inventory] https://bradfrost.com/blog/post/interface-inventory/: interface inventory method.
- [wcag-focus-appearance] https://www.w3.org/WAI/WCAG22/Understanding/focus-appearance.html: 2 px perimeter, 3:1 focused vs unfocused.
- [wcag-non-text] https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html: 3:1 for state indicators, inactive exempt.
- [smashing-tv] https://www.smashingmagazine.com/2025/09/designing-tv-principles-patterns-practical-guidance/: three focus styles, hold for contextual menu, dim behind menus.
- [aosp-keyguard] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/packages/SystemUI/res-keyguard/values/strings.xml (`kg_too_many_failed_attempts_countdown`, "Try again in # seconds.") and https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/packages/SystemUI/src/com/android/keyguard/KeyguardAbsKeyInputViewController.java (`handleAttemptLockout` turns entry off): how Android shows a lock-out. Checked 2026-11-05.
- [uxm-toasts] https://uxmovement.com/forms/best-practices-to-make-your-toasts-usable-and-accessible/: toasts 2 to 5 s, 3 s ideal, 1 to 2 lines. Checked 2026-11-05.
- [dsp-toasts] https://designsystemproblems.pages.dev/accessibility-compliance/toast-notification-accessibility: unofficial; 3 s plus 1 s per 3 words. Checked 2026-11-05.
- Also useful: https://www.nngroup.com/videos/empty-states-in-application-design-guidelines/ (short video summary of the empty-state article).
- [webos-notifications] https://webostv.developer.lge.com/develop/guides/notifications: TV toasts are an overlay at the bottom of the screen, last about 5 s, appear one at a time in order, and need no action. Checked 2026-11-05.
- [uxcel-toasts] https://app.uxcel.com/glossary/toasts: 3–7 s by message length, consistent placement. Checked 2026-11-05.
- [tv-focus] re-checked 2026-11-05: "enters a 'pressed' state until the remote button is released"; disabled = "lower prominence"; scale "can vary based on their size" (the page doesn't require identical indicators).
- https://android.jlelse.eu/polishing-ui-android-statelistanimator-7b74a06b85a5: a `StateListAnimator` selector animates properties (translationZ, scale) per state, which is a cheap way to give every control the same press dip. Checked 2026-11-05.
- Tried and failed: m2.material.io confirmation-acknowledgement (needs JavaScript); NN/g's toast article (not found by search).

- [tvm-button] re-checked 2026-11-05: `focusedDisabledBorder` is 1.5 dp at onSurfaceVariant 20%, so tv-material keeps a visible focus cue on disabled buttons.
- [mdc-snackbar] re-checked 2026-11-05: anchor snackbars above bottom navigation (`setAnchorView`).
- [mdc-motion] https://raw.githubusercontent.com/material-components/material-components-android/master/docs/theming/Motion.md: shared axis for parent/child moves, where backward is the mirror of forward; fade through for unrelated moves. Checked 2026-11-05.

- [aosp-vpa] https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/core/java/android/view/ViewPropertyAnimator.java: `animatePropertyBy` cancels the running animator for the same property (the whole animator if no properties are left); `onAnimationCancel` removes its `withEndAction` runnable, so a cancelled step never runs its follow-up. Checked 2026-11-06.
- [wcag-focus-visible] https://www.w3.org/WAI/WCAG22/Understanding/focus-visible.html: the focus indicator must be visible and "must not be time limited"; subject to 1.4.11; failure F78 (styling that renders the indicator non-visible). Checked 2026-11-06.
- developer.android.com's Kotlin `ViewPropertyAnimator` reference page didn't render its member docs through the fetcher on 2026-11-06. Read the AOSP source instead.

## Open questions to research next
- Which press look suits our gold-glass style best: a 0.96 dip, a brief fill flash, or tv-material's "scale back to 1.0"? Mock all three on the box at 3 m.
- Should Kids keep the inverted white focus (tv-material's focused colours, but without its 1.1 scale) while home uses gold glass, or should home buttons invert too? Find Google TV launcher screenshots or specs for its own buttons (not opened yet).
- Does the 3 px gold focus edge keep 3:1 against its unfocused look over the brightest wallpaper clip? Measure on a screenshot.
- What is a good on-screen time for a pill message? Partly answered 2026-11-05: UX Movement gives 2 to 5 s, 3 s ideal, for a 1 to 2 line toast [uxm-toasts]; an unofficial rule of thumb is 3 s plus 1 s per 3 words [dsp-toasts], so the two-sentence pill needs about 6 s, not 3.5 s. Confirm on the TV.
- Should a resting PIN pad dim its keys to 40% like a tv-material disabled button, or keep them live with a softer nudge?
- Should the adhan reminder and the home pill share one design? Ask lane 11 about the two golds and lane 10 about Baloo outside Kids.
- tvOS HIG "Buttons" has no tvOS-specific section any more; find Apple's tvOS button sizes and focus treatments (older HIG or WWDC session) to compare.
- Find NN/g's article on toasts (fetch failed with a client error on 2026-11-05).
- Where should the pill sit while a panel is open: bottom centre, or the panel's own hint pill? Mock both with lane 9.
- Should the resting PIN pad dim to 40% or 50%? Check on the TV at 3 m.
- Should the shared reminder card take 8 s or the adhan's 15 s? Ask lane 12.

- Over a panel, should the pill take the panel's own hint spot instead of bottom centre?
- Is 0x66 digit text enough for a resting PIN key at 3 m?

- Is `0x24FFFFFF` + 2 px `0x59FFFFFF` enough for the calendar reading guide over PanelBg at 3 m, while still not looking like a button? Measure with lane 11.
- Should the press dip move to one shared helper (a `StateListAnimator` or a shared static class) so home, Kids and adhan Settings can't drift apart?
- Should the adhan Settings screen get the same press dip? Its rebuild-and-regrow already answers OK, but it looks different from the dip.
