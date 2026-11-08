## QuickStack — zarządzanie ekwipunkiem w stylu Terrarii

Wciśnij **Z** i patrz, jak łupy same lecą do skrzyń, w których już takie leżą. QuickStack przenosi do Minecrafta
znane z Terrarii *szybkie odkładanie do pobliskich skrzyń*, a razem z nim zablokowane sloty, sortowanie,
wytwarzanie z pobliskich skrzyń i garść drobnych usprawnień, dzięki którym magazyn przestaje być udręką.

### ✨ Szybkie odkładanie do pobliskich skrzyń
Wciśnij **Z** (albo przycisk w ekwipunku). Ekwipunek się zamyka, przedmioty wyskakują przed tobą i jeden po drugim
lecą do pobliskich skrzyń, beczek i skrzyń shulkerowych, które już zawierają ten sam przedmiot. Skrzynie unoszą
wieka, gdy przedmioty do nich trafiają.

### 🔒 Zablokowane sloty
**Alt + klik** na slot blokuje go. Zablokowane sloty nigdy się nie opróżniają: ani przy szybkim odkładaniu, ani przy
„Włóż wszystko”, ani przy sortowaniu. Pasek szybkiego dostępu jest domyślnie zablokowany; zbroi i drugiej ręki mod
w ogóle nie rusza.

### 📦 Przyciski skrzyni
Każda skrzynia, beczka i skrzynia shulkerowa dostaje rząd małych przycisków:
- **Odłóż do tej skrzyni** — tylko przedmioty, które już w niej są
- **Uzupełnij** — weź ze skrzyni przedmioty, które nosisz, zostawiając po 1 z każdego (do ustawienia)
- **Włóż wszystko** / **Zabierz wszystko**
- **Sortuj** skrzynię
- **Wyklucz** tę skrzynię z odkładania do pobliskich

### 🧹 Sortowanie
Sortuj ekwipunek lub dowolną skrzynię przyciskiem, klawiszem albo **środkowym przyciskiem myszy**. Według kategorii,
nazwy lub ilości. Niepełne stosy są łączone.

### 🖱️ Przeciąganie z Shift
Przytrzymaj **Shift + lewy przycisk myszy** i przeciągnij po slotach: każdy slot, nad którym przejedziesz, trafia do
drugiego ekwipunku jak po Shift + kliknięciu. Zablokowane sloty są pomijane.

### 🎡 Kółko myszy, po jednym przedmiocie
**Kółko w górę** nad stosem bierze jeszcze jeden przedmiot, **kółko w dół** odkłada jeden z powrotem — jak kliknięcia
prawym przyciskiem, tylko szybciej.

### 🛠️ Wytwarzanie z pobliskich skrzyń
Książka receptur liczy przedmioty w skrzyniach wokół ciebie, a siatka wytwarzania bierze z nich brakujące składniki.
Resztki wracają do skrzyni, z której przyszły, gdy zamkniesz stół rzemieślniczy.

### 🔍 Szukanie przedmiotów
**F6** otwiera siatkę ze wszystkim, co leży w skrzyniach wokół ciebie. Pisz, aby filtrować, kliknij przedmiot,
a skrzynie, które go zawierają, rozbłysną kolumną iskier widoczną przez ściany.

### ↩️ Cofnij
Odłożyłeś nie to, co trzeba? Przypisz klawisz *Cofnij* i odzyskaj ostatnie odłożenie.

---

### ⚙️ Ustawienia
Wszystko zmienisz w grze: przyciskiem z zębatką w ekwipunku, w Mod Menu (Fabric) lub na liście modów (NeoForge).
W grze jednoosobowej druga karta ustala limity świata: zasięg, prędkość lądowania, wytwarzanie z pobliskich skrzyń,
skrzynie z modów.

### 🚀 Wydajność
Wszystkie przenosiny wykonuje serwer małymi porcjami na tick, a latające przedmioty to tylko efekt wizualny po
stronie klienta, więc nic nie może zginąć ani się zduplikować. Przetestowano na **2 352 pełnych skrzyniach**
w promieniu 16 bloków: planowanie trwa około 7 ms, a żaden tick serwera nie przekroczył 5 ms podczas lądowania
36 stosów.

### 📋 Wymagania
- Minecraft **26.3**
- **Fabric** (z Fabric API) lub **NeoForge**
- Instalacja **na kliencie i na serwerze** (w grze jednoosobowej działa od razu)
- Opcjonalnie: Mod Menu

Skrzynie z modów działające jak zwykłe pojemniki (18+ slotów) są obsługiwane automatycznie.
