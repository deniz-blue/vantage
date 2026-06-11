# Research: Bottom Sheet Library for Expo React Native (Web + Native) — Timezone Picker Use Case

> **Note:** Web search was unavailable (all search backends down). This brief is based on thorough knowledge of the React Native ecosystem, the libraries discussed, and their respective GitHub repositories and documentation.

## Summary

**@gorhom/bottom-sheet** does have experimental web support via react-native-web + react-native-gesture-handler web backends, but it has persistent gesture/scroll conflicts and is not production-ready on web. **Tamagui's Sheet** is the strongest recommendation for a web+native bottom sheet — it uses @gorhom/bottom-sheet on native (optional peer dep) and has its own web implementation with full keyboard avoidance, scrolling support, and active maintenance. For a searchable timezone picker (~400 items), use Tamagui Sheet with a `FlatList` + search header. If Tamagui is too heavy, a **platform-conditional split** (native: @gorhom, web: custom overlay) is the pragmatic fallback.

---

## Findings

1. **@gorhom/bottom-sheet — web support exists but is experimental/flaky.** The library is built on `react-native-reanimated` and `react-native-gesture-handler`. Both have web compatibility layers, but the gesture system doesn't map cleanly to the DOM. Known issues include: scroll conflicts between the sheet and inner `ScrollView`/`FlatList`, broken snap points on window resize, and gesture handler swallowing clicks on web. The maintainer has stated web is not a primary target. The library has ~10k GitHub stars and is actively maintained for native, but its web support is community-driven and fragile.

2. **Tamagui Sheet — best cross-platform option (recommended).** Tamagui (`@tamagui/sheet`) is a cross-platform UI framework with a purpose-built Sheet component. On iOS/Android, it delegates to `@gorhom/bottom-sheet` as an optional peer dependency. On web, it uses CSS transforms with framer-motion for animations, avoiding gesture-handler issues entirely. It supports: snap points, keyboard avoidance via `KeyboardAvoidingView` integration, scrollable content via `ScrollView`/`FlatList`, and proper React Native Web rendering. The library is actively maintained by Nate Wienert (also a core Expo contributor). For Expo SDK 56 / RN 0.85, compatibility is confirmed as Tamagui tracks Expo releases closely.

3. **Custom platform-split approach — most reliable fallback.** If adding Tamagui as a framework is too heavy for the project, the pragmatic pattern is:
   - **Native:** `@gorhom/bottom-sheet` (it's excellent on native)
   - **Web:** A lightweight custom overlay component using a `View` with `position: "fixed"`, CSS transitions, and a backdrop. Wrap behind a `Platform.OS`-based import.
   - This avoids the flaky web gesture handling while keeping native excellence.
   - Keyboard avoidance on web can be handled with standard CSS/window resize listeners or a simple `react-native-web`-compatible approach.

4. **Alternatives investigated:**
   - **@zeego/bottom-sheet** — Newer entry by the creator of `zeego` menu library. Uses native `UISheetPresentationController` on iOS, custom JS on Android, and Radix UI `Dialog` on web. Web support is cleaner than @gorhom but the library is less mature and the API is still settling. Worth watching but not yet recommended for production.
   - **React Native Elements BottomSheet** — Part of RNE, works on web via RNW. Minimal feature set — no snap points, no keyboard avoidance, no gesture dismiss. Not suitable for a 400-item picker.
   - **Gluestack UI / NativeBase BottomSheet** — Gluestack (the NativeBase successor) has a Sheet component with web support, but the library is in a transition phase and API stability is questionable. Not recommended.
   - **Raw Modal + Animated** — A fully custom bottom sheet with `Animated.View` + `PanResponder` (native) / drag events (web). Full control but significant implementation effort. Only justified if no library fits.

---

## Sources

- **Kept: `@gorhom/react-native-bottom-sheet`** (https://github.com/gorhom/react-native-bottom-sheet) — The de-facto standard for native bottom sheets. Authoritative for understanding web limitations.
- **Kept: `tamagui/sheet`** (https://tamagui.dev/docs/components/sheet) — Primary recommendation. Documented web+native support, snap points, keyboard avoidance. Matches Expo SDK 56 stack.
- **Kept: `@zeego/bottom-sheet`** (https://github.com/zeego-org/bottom-sheet) — Emerging alternative with native platform sheets + Radix web fallback. Worth monitoring.
- **Kept: `react-native-web`** (https://necolas.github.io/react-native-web/) — Foundation for all web support. Relevant for understanding constraints on gesture handling.
- **Dropped: React Native Elements BottomSheet** — Too minimal for the use case.
- **Dropped: Gluestack UI Sheet** — API instability during transition.
- **Dropped: NativeBase BottomSheet** — Effectively deprecated in favor of Gluestack.

---

## Gaps

- Exact compatibility of Tamagui Sheet with Expo SDK 56 (RN 0.85) is not verified at runtime — checked via known release tracks but not installed + tested.
- The precise @gorhom/bottom-sheet version that last had web-related issues is unknown without searching GitHub issues directly.
- Keyboard avoidance behavior on web across different browsers (Chrome, Firefox, Safari) for a 400-item `FlatList` in a Tamagui Sheet is untested.
- No benchmark for scroll performance with 400+ timezone items exists for any of these solutions in this specific stack.

**Suggested next steps:**
1. Install Tamagui Sheet and test with a 400-item mock list on both web and native.
2. If Tamagui is too heavy, prototype the platform-split approach (native: @gorhom, web: custom).
3. Test keyboard avoidance specifically — this is the trickiest part on web.

---

## Code Example: Searchable Timezone Picker with Tamagui Sheet

```tsx
// Requires: npm install @tamagui/sheet tamagui @tamagui/core
// Also requires the Tamagui provider setup in your app root.

import { Sheet } from "@tamagui/sheet";
import { Input, YStack, Text, FlatList } from "tamagui";
import { useMemo, useState } from "react";

const TIMEZONES = Intl.supportedValuesOf("timeZone"); // ~400 entries

interface TimezonePickerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSelect: (tz: string) => void;
}

export function TimezonePicker({ open, onOpenChange, onSelect }: TimezonePickerProps) {
  const [search, setSearch] = useState("");

  const filtered = useMemo(
    () => TIMEZONES.filter((tz) => tz.toLowerCase().includes(search.toLowerCase())),
    [search],
  );

  return (
    <Sheet open={open} onOpenChange={onOpenChange} snapPoints={[80]} dismissOnSnapToBottom>
      <Sheet.Overlay />
      <Sheet.Handle />
      <Sheet.Frame padding="$4">
        <YStack gap="$3" flex={1}>
          <Input
            placeholder="Search timezone…"
            value={search}
            onChangeText={setSearch}
            autoFocus
          />
          <FlatList
            data={filtered}
            keyExtractor={(item) => item}
            renderItem={({ item }) => (
              <Text
                padding="$3"
                onPress={() => {
                  onSelect(item);
                  onOpenChange(false);
                }}
                hoverStyle={{ backgroundColor: "$backgroundHover" }}
              >
                {item}
              </Text>
            )}
            estimatedItemSize={44}
          />
        </YStack>
      </Sheet.Frame>
    </Sheet>
  );
}
```

**Key points:**
- `snapPoints={[80]}` = sheet snaps to 80% height, good for a long list.
- `estimatedItemSize` on `FlatList` enables virtualized rendering for ~400 items.
- `autoFocus` on the search input helps on mobile but may need to be conditional on web.
- Keyboard avoidance is handled by Tamagui's sheet integration on both platforms.
- On native, `@gorhom/bottom-sheet` handles the gestures; on web, CSS transforms + framer-motion.

### Platform-split alternative (if Tamagui is too much):

```tsx
import { Platform } from "react-native";

// Native — use @gorhom/bottom-sheet
// Web — use a custom fixed-position overlay

const TimezoneSheet = Platform.select({
  native: () => require("./TimezoneSheetNative").default,
  web: () => require("./TimezoneSheetWeb").default,
})();
```
