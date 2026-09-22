<p align="center">
  <img src="app/src/main/res/drawable-nodpi/whtz_title.png" alt="Whtz" width="380">
</p>

<p align="center">
  <img src="app/src/main/res/drawable-nodpi/main_icon.png" alt="Whtz mascot" width="150">
</p>

<p align="center"><em>A MTG card printer and companion app for unnecessarily large decks.</em></p>

---

## What is Whtz?

Whtz is an Android app for **solo playtesting / goldfishing Commander (EDH) decks**. It models
one player's board — all the game zones — and, as you play, **prints real physical proxy cards on a
Bluetooth thermal printer** (built for the NETUM NT-861, works with compatible ESC/POS printers).

Draw your opening hand, cast spells, and the moment a card hits the battlefield Whtz prints a
tidy black‑bordered proxy for it. It's a fast way to physically play a brewed deck without sleeving
up a stack of paper proxies by hand.

## How it works

1. **Download the card database (once).** On first launch Whtz pulls the Scryfall bulk data —
   ~30,000 Oracle cards, plus **rulings** and **tokens** — into a local Room database. Everything is
   searchable **offline** afterward; only card art is fetched on demand at print time.
2. **Import a deck.** Paste a decklist; Whtz resolves each card against the local database (falling
   back to the Scryfall API for anything missing) and saves it. Decks can be edited or deleted later.
3. **Play.** Start a game from a deck. Whtz tracks one player's zones — **Library, Hand, Graveyard,
   Exile, Battlefield, Command** — with the usual actions: draw / draw 7 / mill / exile‑from‑top,
   scry & surveil (drag cards to the top or bottom/graveyard), shuffle, wheel, move cards between
   zones, and cast your commander (with commander tax tracked automatically). An **action log** lets
   you revert the game to any earlier point.
4. **Print.** Tap **Print** on a card and Whtz rasterizes it to a clean, card‑sized image and sends
   it to the printer over Bluetooth. Art is dithered (halftone) for the thermal head; name, mana
   cost, type, rules text and P/T are rendered as crisp black text inside a black border.

## Features

**Printing**
- Card‑sized, black‑bordered proxies with dithered art and legible text, normalized so every card
  prints at a consistent size (long rules text shrinks to fit; short cards are padded).
- **Type‑aware casting:** instants & sorceries go to the graveyard, permanents to the battlefield.
- **Adventures:** choose to cast the creature (→ battlefield) or the adventure spell (→ exile).
- **Double‑faced cards:** flip the preview to see either side; pick which side to print.
- **"Print one, mark the rest"** — reprint duplicates or just mark them onto the battlefield.
- 58 mm / 80 mm paper, adjustable print darkness, and a card‑sized test print.

**Tokens & rulings**
- A searchable, offline **token library** you can print from directly.
- **Print tokens** a card creates, straight from its actions.
- **Print rulings** for a card as a tidy text slip.

**Browsing & play aids**
- **Visual card view** — a swipeable image carousel per zone with a fast scrubber; tap a card for its
  actions. Images load lazily to stay light on memory.
- Collapsible **search + filters** (by type, rarity), plus **batch select / select‑all** for moving
  or printing many cards at once.
- **Privacy mode** blanks the board when you hand someone the device — with buttons that still let an
  opponent inspect your Library, Graveyard, and Exile.

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Hilt** for dependency injection
- **Room** for the offline card / token / ruling database
- **OkHttp** + **kotlinx.serialization** for the Scryfall API and bulk import
- **Coil** for card image loading
- **DantSu ESC/POS** for Bluetooth thermal printing

## Getting started

**Requirements:** Android Studio, an Android device on **8.0+ (API 26+)**, and a Bluetooth ESC/POS
thermal printer (e.g. NETUM NT‑861).

1. Open the project in Android Studio and run it on a device.
2. On first launch, tap **Download card database** and let the import finish (it runs offline after).
3. Pair your printer in Android's Bluetooth settings, then open **Printer settings** in Whtz, grant
   the Bluetooth permission, pick your printer and paper width, and run a test print.
4. **Import a deck**, start a game, and print away.

## Notes

Whtz is a personal playtesting tool and is **not affiliated with or endorsed by Wizards of the Coast**.
Card data and images are provided by [Scryfall](https://scryfall.com); Magic: The Gathering and all
card content are © Wizards of the Coast. Print proxies for personal playtesting only.
