# Architektura

[Polski](architecture.pl.md) | [English](architecture.en.md)

Ten dokument stanowi normatywny kontrakt architektoniczny repozytorium. Opisuje zasady, które musi spełniać każdy element systemu, jeżeli zostaje dodany. Nie jest opisem stanu implementacji, backlogiem ani kolejnością prac. Brak danego mechanizmu w repozytorium nie osłabia reguł, które zaczynają go dotyczyć w chwili dodania odpowiadającego mu kodu.

## 1. Cel i priorytety

- KISS ma pierwszeństwo przed abstrakcją, która nie rozwiązuje konkretnego problemu.
- Kod produkcyjny jest pisany w Kotlinie i uruchamiany na Java 21.
- System jest monolitem modularnym z jawnymi granicami kontekstów ograniczonych i warstw.
- Kierunek zależności jest częścią architektury i musi być egzekwowany automatycznie tam, gdzie może go sprawdzić ArchUnit.
- CQRS-lite rozdziela zapis i odczyt technicznie oraz semantycznie, bez osobnych wdrożeń lub baz danych.
- Polecenia, zapytania i zdarzenia tworzą stabilne kontrakty wewnętrzne. Nie wprowadza się ich wersjonowania bez problemu zgodności, który tego wymaga.
- Każdy przypadek użycia realizujący zapis ma jeden oczywisty punkt zarządzania transakcją i `flush`.
- Konwencja ma pierwszeństwo przed ręczną konfiguracją, o ile nie zaciera odpowiedzialności ani wyboru implementacji.
- Kod domenowy ma być testowalny z deterministycznym czasem i bez infrastruktury uruchomieniowej.
- Mikroserwisy, broker wiadomości, Redis i podobna infrastruktura wymagają konkretnego uzasadnienia biznesowego lub operacyjnego. Nie są domyślnym sposobem separacji modułów.

## 2. Konteksty ograniczone i własność

Każda zdolność biznesowa, pojęcie domenowe, tabela, migracja i kontrakt ma jednego właściciela.

- `Client` jest właścicielem przestrzeni tenanta, członkostwa klienta, ról tenantowych i reguł dostępu wewnątrz klienta.
- `User` jest właścicielem współdzielonej między tenantami tożsamości użytkownika oraz uwierzytelniania, w tym mechanizmów OTP.
- `SharedKernel` zawiera wyłącznie stabilne mechanizmy i kontrakty rzeczywiście współdzielone, takie jak obsługa poleceń i zdarzeń, rejestrowanie zdarzeń domenowych, czas, dziennik zdarzeń, kontrakty zdarzeń integracyjnych, outbox i obsługa przetwarzania asynchronicznego.

`SharedKernel` nie jest miejscem dla pojęć biznesowych tylko dlatego, że pojawiają się w więcej niż jednym module. Współdzielenie wymaga jednego stabilnego znaczenia. `SharedKernel` nie może zależeć od żadnego kontekstu biznesowego.

Kontekst jest właścicielem swojego modelu domenowego, schematu bazy danych, SQL, migracji oraz publicznych kontraktów udostępnianych innym kontekstom. Żaden kontekst nie odczytuje ani nie modyfikuje bezpośrednio modelu domenowego lub tabel należących do innego kontekstu.

## 3. Pakiety i struktura katalogów

Pakiet bazowy aplikacji to:

```text
com.bartoszcichecki.pragmaticjvmdemo
```

Kod kontekstu jest zorganizowany według warstw:

```text
src/main/kotlin/com/bartoszcichecki/pragmaticjvmdemo/
├── sharedkernel/
├── client/
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── ui/
└── user/
    ├── domain/
    ├── application/
    ├── infrastructure/
    └── ui/
```

Wewnątrz kontekstu stosuje się następujący szablon, rozwijany tylko w zakresie wymaganym przez rzeczywisty kod:

```text
{boundedcontext}/
├── domain/
│   └── {aggregate}/
│       ├── event/
│       ├── factory/
│       ├── outside/
│       └── repository/
├── application/
│   └── {aggregate}/
│       ├── command/
│       │   └── {action}/
│       └── query/
│           └── dto/
├── infrastructure/
│   ├── {aggregate}/
│   │   ├── persistence/
│   │   ├── query/
│   │   └── outside/
│   └── configuration/
└── ui/
    ├── http/
    │   └── api/
    ├── input/
    └── console/
```

Nazwy pakietów Kotlin są zapisywane małymi literami, a nazwy typów zgodnie z konwencjami Kotlin. Pakiet agregatu powstaje tylko dla istniejącego agregatu. Puste pakiety, typy zastępcze i abstrakcje „na później” są niedozwolone.

Migracje Flyway są grupowane według właściciela, na przykład:

```text
src/main/resources/db/migration/{boundedcontext}/
src/main/resources/db/migration/sharedkernel/
```

## 4. Warstwy i kierunek zależności

### 4.1 Domain

- Zawiera agregaty, encje, value objects, fabryki, polityki i usługi domenowe, zdarzenia domenowe oraz porty repozytoriów i Outside.
- Nie zależy od `application`, `infrastructure` ani `ui`.
- Może zależeć od Kotlin/JDK, własnego kontekstu oraz odpowiednich kontraktów domenowych `SharedKernel`.
- Minimalne adnotacje wymagane wyłącznie przez mapowanie persystencji są dopuszczalne, ale nie mogą wpływać na publiczne API ani zachowanie domeny.
- Nie zależy od usług aplikacyjnych Springa, Spring MVC, Spring `JdbcClient`, implementacji repozytoriów ani adapterów integracyjnych.

### 4.2 Application

- Orkiestruje przypadki użycia za pomocą modelu Domain i portów należących do bieżącego kontekstu.
- Zależy od Domain oraz od kontraktów własnej warstwy Application.
- Nie zależy od implementacji Infrastructure ani od UI.
- Nie zna i nie wstrzykuje portów Outside. Decyzje biznesowe wymagające zewnętrznego stanu tylko do odczytu pozostają w Domain.
- Nie wykonuje SQL i nie manipuluje encjami JPA z innego kontekstu.

### 4.3 Infrastructure

- Implementuje repozytoria, query services, Outside, publikację komunikatów i pozostałe porty techniczne.
- Zawiera JPA/Hibernate, Spring `JdbcClient`, SQL, integracje zewnętrzne, transport zdarzeń oraz konfigurację Springa należącą do modułu.
- Może zależeć od kontraktów Domain i Application.
- Nie zawiera reguł biznesowych.

### 4.4 UI

- Zawiera adaptery wejściowe, w tym kontrolery HTTP i komendy konsolowe.
- Waliduje format i kompletność danych transportowych, mapuje input na polecenie lub zapytanie aplikacyjne i mapuje wynik na odpowiedź transportową.
- Nie zawiera reguł biznesowych, nie otwiera transakcji i nie uzyskuje bezpośredniego dostępu do JPA, `JdbcClient` ani SQL.

### 4.5 Odczyt danych z innego kontekstu (ACL)

- Kontekst A nie wykonuje SQL względem tabel należących do kontekstu B.
- Jeżeli A potrzebuje danych B, adapter w Infrastructure kontekstu A wywołuje publiczny kontrakt zapytania udostępniony przez B.
- A definiuje własny wynik lub DTO odpowiadający jego potrzebie i mapuje odpowiedź B we własnym adapterze Infrastructure. Typy B nie są reeksportowane do Domain ani Application kontekstu A.
- Adapter jest warstwą antykorupcyjną. W monolicie modularnym wywołanie pozostaje synchroniczne i wewnątrz procesu; nie wymaga sieci ani serializacji.

### 4.6 Zapis inicjowany między kontekstami

- Jeżeli przypadek użycia w A musi zainicjować zmianę należącą do B, Application kontekstu A zależy od portu zdefiniowanego przez A.
- Implementacja portu w Infrastructure kontekstu A może wysłać publiczne polecenie B przez wewnętrzną magistralę poleceń i, jeśli to konieczne, pobrać wynik przez publiczny kontrakt zapytania B.
- Domain i Application kontekstu A nie importują typów Domain ani Application kontekstu B. Obcy kontrakt i jego mapowanie pozostają w adapterze Infrastructure kontekstu A.
- Kontekst nie modyfikuje agregatu ani nie wywołuje repozytorium innego kontekstu bezpośrednio.

## 5. Model domenowy

### 5.1 Agregaty, encje i value objects

- Agregat wyznacza granicę spójności i egzekwuje wszystkie niezmienniki, które muszą być zachowane atomowo.
- Stan agregatu może być zmieniany wyłącznie przez jawne zachowanie korzenia agregatu. Publiczne settery i omijanie zachowania przez mapowanie aplikacyjne są niedozwolone.
- Konstruktor lub fabryka tworzy wyłącznie poprawny agregat. Każda operacja odrzuca zmianę, która narusza niezmiennik; nie wolno utrwalać przejściowo niepoprawnego stanu.
- Encja ma stabilną tożsamość i cykl życia kontrolowany przez korzeń agregatu. Encja wewnętrzna nie ma własnego repozytorium, dopóki sama nie stanowi korzenia odrębnego agregatu.
- Value object jest niemutowalny, porównywany po wartości i sam waliduje swoje ograniczenia. Prymityw nie zastępuje value objectu, jeżeli pojęcie ma reguły lub semantykę domenową.
- Reguła obejmująca wiele danych agregatu należy do agregatu. Reguła domenowa, która nie przynależy naturalnie do jednej encji, należy do fabryki, polityki lub usługi domenowej, a nie do handlera.

### 5.2 Hermetyzacja agregatów

- Agregaty ujawniają zachowanie, nie stan wewnętrzny.
- Publiczne właściwości lub gettery ujawniające stan nie są dodawane na potrzeby orkiestracji, odpowiedzi API ani testów.
- Wymagania hydratacji JPA mogą korzystać z dostępu prywatnego lub przeznaczonego wyłącznie dla persystencji, ale nie mogą tworzyć publicznego API odczytu agregatu.
- Zdarzenia domenowe są kontraktem faktów wynikających ze zmiany stanu i zawierają dane potrzebne odbiorcom.
- Odczyty UI i API przechodzą przez query services i DTO, nie przez agregaty.

### 5.3 Outside

Outside jest kontrolowanym, ukierunkowanym na odczyt oknem domeny na stan, którego domena nie posiada.

- Port Outside należy do Domain i może być używany przez agregaty, fabryki, polityki oraz usługi domenowe.
- Application nie zna Outside i nie przekazuje danych technicznych tylko po to, by wykonać za domenę decyzję.
- Outside udostępnia pytania istotne biznesowo, takie jak bieżący czas, stan uprawnień, skróty, liczniki, limity lub fakty z innych kontekstów.
- Outside nie powoduje zewnętrznych biznesowych skutków ubocznych. Może rejestrować zdarzenie domenowe w kolektorze pamięciowym, ale nie może zmieniać innego agregatu ani systemu zewnętrznego.
- Dostęp między kontekstami przez Outside jest wyłącznie odczytowy i przechodzi przez ACL w Infrastructure.
- Infrastructure implementuje Outside i deleguje do mechanizmów `SharedKernel` lub publicznych query services właściciela danych.

### 5.4 Polityka jako usługa domenowa

- Polityka jest usługą domenową; jej nazwa nie daje jej dodatkowych uprawnień architektonicznych.
- Polityka powinna być czysta, gdy wywołujący ma wszystkie wymagane dane domenowe.
- Gdy decyzja wymaga zewnętrznego stanu tylko do odczytu, polityka może zależeć od Outside albo wąskiego domenowego portu odczytu. Nie przenosi się decyzji do Application wyłącznie po to, aby polityka pozostała pozornie czysta.
- Application przekazuje biznesowy input. Polityka pobiera potrzebny stan i egzekwuje regułę.

```text
handler: policy.assertCanAddItems(aggregateId, requestedCount)
policy:  currentCount = outside.countItems(aggregateId)
         require(currentCount + requestedCount <= limit)
```

### 5.5 Czas w domenie

- Znaczniki czasu domeny, takie jak `createdAt`, `updatedAt`, `statusChangedAt` i `occurredAt`, używają `java.time.Instant`.
- Domena pobiera bieżący `Instant` przez Outside. Application i UI nie przekazują technicznego `now` ani czasu utworzenia wyłącznie dla testowalności.
- Produkcyjna implementacja Outside pobiera czas ze wstrzykniętego `java.time.Clock`. Agregat nie zależy bezpośrednio od `Clock`.
- Kod Domain nie wywołuje `Instant.now()` i nie tworzy zegara systemowego.
- Testy sterują czasem przez atrapę Outside albo stały lub modyfikowalny `Clock` używany przez implementację Outside.
- Backend i baza danych przechowują jednoznaczne chwile UTC. Mapowanie JPA, JDBC i Flyway musi zachować `Instant` bez niejednoznaczności strefy czasowej.
- Dziennik zdarzeń, outbox, dzierżawy i workery korzystają ze wstrzykniętego `Clock`; PostgreSQL nie jest źródłem czasu aplikacji.
- Konwersja chwili na lokalną strefę użytkownika należy do UI. Model domenowy i read modele nie zmieniają semantyki zapisanej chwili.
- Zakres dat będący biznesowym inputem jest value objectem walidowanym w Domain. UI przelicza lokalny zakres na jednoznaczne chwile przed przekazaniem go do backendu.

## 6. CQRS-lite

CQRS-lite oznacza rozdzielenie odpowiedzialności zapisu i odczytu w jednym systemie oraz jednej bazie danych. Nie oznacza event sourcingu ani osobnej infrastruktury dla zapytań.

### 6.1 Polecenia i handlery

- Każdy przypadek użycia zmieniający stan domeny jest reprezentowany przez polecenie.
- Polecenie jest niemutowalną wartością Kotlin i nie zawiera logiki biznesowej.
- Handler obsługuje jeden przypadek użycia i orkiestruje fabryki, repozytoria, polityki i agregaty.
- Walidacja biznesowa pozostaje w Domain. Handler nie powiela niezmienników i nie modyfikuje stanu z pominięciem zachowania agregatu.
- Agregat rejestruje swoje zdarzenia domenowe przez Outside. Handler nie tworzy ani nie rejestruje ich w imieniu agregatu.
- Handlery domyślnie zwracają `Unit`.
- Jeżeli przypadek użycia wymaga minimalnego wyniku biznesowego, polecenie może mieć typowany wynik. Wynik nie może być agregatem, encją JPA ani read modelem i jest udostępniany wywołującemu dopiero po pomyślnym zatwierdzeniu transakcji.
- Polecenia są wysyłane przez centralny, typowany dispatcher. UI, subskrybenci i adaptery między kontekstami nie wywołują handlerów bezpośrednio.

### 6.2 Repozytoria i transakcja zapisu

- Port repozytorium należy do Domain i operuje językiem domenowym oraz korzeniem agregatu.
- Repozytorium istnieje dla korzenia agregatu, nie dla każdej encji. Nie jest wykorzystywane do budowania odpowiedzi odczytowych.
- Adapter repozytorium należy do Infrastructure i używa JPA/Hibernate do ładowania i utrwalania agregatów.
- Repozytorium dołącza lub utrwala zmiany, ale nie wywołuje `flush()`, nie wykonuje `commit` i nie otwiera transakcji.
- Każde polecenie najwyższego poziomu ma jedną granicę transakcji Springa wokół centralnego dispatchera lub jego dekoratora. Kontroler i handler nie definiują konkurencyjnej granicy.
- Zmiany JPA, dziennik zdarzeń, synchroniczna obsługa zdarzeń domenowych i zapisy outboxa kończą się w tej samej transakcji. Błąd któregokolwiek elementu wycofuje całość.
- Centralny przepływ koordynuje opróżnienie kolektora zdarzeń oraz jeden jawny punkt `flush`. Zdarzenia z wycofanej transakcji nie mogą zostać uznane za opublikowane.
- Techniczny wyjątek od jednej granicy wymaga ADR i musi pozostać poza regułami biznesowymi agregatu.

### 6.3 Zapytania i read modele

- Przypadek użycia tylko do odczytu jest udostępniany przez query service należący do właściciela danych.
- Implementacja query service używa SQL przez Spring `JdbcClient`, a nie ładowania encji przez JPA/Hibernate.
- Zapytanie zwraca niemutowalny, przeznaczony dla danego przypadku użycia DTO lub read model; nigdy agregat ani encję JPA.
- Zapytanie nie powoduje skutków ubocznych i nie otwiera domenowej ścieżki zapisu.
- SQL, mapowanie i DTO należą do kontekstu będącego właścicielem danych.
- Niezależny read model jest wymagany, gdy kształt lub wydajność odczytu nie odpowiada modelowi zapisu. Nie dodaje się go bez przypadku użycia, który go potrzebuje.

### 6.4 Techniczne operacje SQL

Bezpośredni zapis SQL poza JPA jest dopuszczalny dla mechanizmów technicznych `SharedKernel`, takich jak dziennik zdarzeń, atomowe przejęcie outboxa i rekordy idempotencji. Jest to jawny wyjątek, a nie zwykła ścieżka zapisu w kontekście biznesowym.

## 7. Zdarzenia i skutki uboczne

### 7.1 Zdarzenia domenowe

Każde zdarzenie domenowe agregatu zawiera:

- identyfikator agregatu jako value object właściwy dla kontekstu;
- `occurredAt` jako `Instant`;
- dane właściwe dla zdarzenia, potrzebne jego odbiorcom.

Nazwy pól są spójne w obrębie kontekstu. Wewnętrzne zdarzenia domenowe nie są wersjonowane.

Zdarzenie domenowe jest synchronicznym kontraktem wewnątrz procesu. Domena rejestruje je przez Outside, centralny przepływ polecenia zapisuje je w dzienniku zdarzeń i rozsyła przez synchroniczną magistralę zdarzeń wewnątrz transakcji polecenia. Nie jest elementem kolejki asynchronicznej ani kontraktem trwałego dostarczenia między procesami.

### 7.2 Zdarzenia integracyjne

- Zdarzenie integracyjne jest kontraktem odrębnym od zdarzenia domenowego.
- Służy do asynchronicznej komunikacji technicznej między modułami lub procesami, gdy odbiorca nie powinien uczestniczyć w transakcji polecenia.
- Payload zawiera typy proste i proste struktury JSON. Nie zawiera agregatów, encji JPA ani frameworkowych typów transportowych.
- Publikacja zapisuje rekord outboxa w bieżącej transakcji bazy danych i nie rozsyła zdarzenia w pamięci.
- Trwały rekord zawiera techniczny identyfikator zdarzenia, stabilną nazwę zdarzenia, payload JSON i `createdAt` ze wstrzykniętego `Clock`.
- Synchroniczny handler w Application może przekształcić zdarzenie domenowe w integracyjne. Jeżeli celem jest publikacja asynchroniczna, używa publishera zdarzeń integracyjnych zamiast wysyłać dodatkowe polecenie wyłącznie w celu dotarcia do outboxa.
- Handlery synchroniczne i subskrybenci asynchroniczni są beanami Springa wykrywanymi przez jawne, typowane kontrakty i wstrzykiwanie przez konstruktor.

### 7.3 Subskrybenci i skutki uboczne

- Synchroniczny subskrybent nie modyfikuje encji JPA ani repozytorium bezpośrednio.
- Jeżeli reakcja synchroniczna wymaga zmiany domeny, subskrybent wysyła dedykowane polecenie. Polecenie uczestniczy w transakcji najwyższego poziomu.
- Zmiana należąca do innego kontekstu przechodzi przez jego publiczny kontrakt zgodnie z regułami ACL.
- Nietransakcyjny zewnętrzny skutek uboczny, który ma być wykonany asynchronicznie, jest inicjowany zdarzeniem integracyjnym zapisanym w outboxie.
- Handler skutku ubocznego musi być idempotentny biznesowo, jeżeli ponowne dostarczenie może powtórzyć operację zewnętrzną.

## 8. Transactional outbox i przetwarzanie asynchroniczne

Jeżeli zdarzenie integracyjne ma być opublikowane asynchronicznie, musi zostać utrwalone w transactional outbox w tej samej transakcji co zmiana domenowa.

- `shared.async_outbox` jest trwałą kolejką i magazynem stanu zdarzeń integracyjnych.
- Worker Spring Boot odpytuje PostgreSQL. Domyślny model nie wymaga brokera wiadomości, Redisa ani `LISTEN/NOTIFY`.
- Worker przejmuje partię jednym atomowym `UPDATE ... FROM (SELECT ... FOR UPDATE SKIP LOCKED) ... RETURNING` i zapisuje token właściciela.
- Rekord kwalifikuje się do przejęcia, gdy nie został przetworzony, nie osiągnął limitu prób oraz nie ma aktywnego przejęcia albo jego dzierżawa wygasła.
- Domyślna wielkość partii wynosi 50, limit pięć prób, czas dzierżawy pięć minut, a opóźnienie między pustymi odpytywaniami pięć sekund. Wartości są konfigurowalne.
- Przejęcie zwiększa licznik prób. Błąd zapisuje ograniczony opis, zwalnia przejęcie i pozostawia rekord dostępny, dopóki limit prób nie został osiągnięty.
- Po pięciu nieudanych próbach rekord nie jest automatycznie przejmowany. Oddzielna kolejka dead-letter nie jest częścią tego modelu.
- Payload jest deserializowany według stabilnej nazwy zdarzenia i przekazywany wyłącznie subskrybentom zgodnym z jego typem.
- `shared.async_consumption` przechowuje stan przejęcia i idempotencji dla kombinacji zdarzenia, subskrybenta i handlera.
- Przed wywołaniem handlera worker atomowo tworzy albo przejmuje rekord `processing`. Rekord `processed` jest pomijany, a ważna dzierżawa innego workera powoduje ponowienie później.
- Po sukcesie worker zapisuje `processed` wyłącznie przy zachowanej własności. Po błędzie zwalnia własne przejęcie konsumpcji i rekord outboxa do ponowienia.
- Rekord outboxa otrzymuje stan przetworzony dopiero po sukcesie wszystkich pasujących subskrybentów i przy zachowanej własności rekordu.
- Polecenie wysłane przez subskrybenta asynchronicznego działa we własnej transakcji workera.
- Techniczna idempotencja zapobiega ponownemu wykonaniu handlera oznaczonego jako `processed`, ale nie zastępuje biznesowej idempotencji zewnętrznego skutku ubocznego. Proces może zakończyć się po skutku ubocznym, lecz przed zapisaniem sukcesu.

## 9. Persystencja i migracje

- JPA/Hibernate obsługuje zapis agregatów; Spring `JdbcClient` obsługuje read modele.
- Spring Data może być używany wewnątrz adaptera repozytorium, ale jego interfejsy i typy nie zastępują domenowego portu repozytorium i nie wychodzą poza Infrastructure.
- Szczegóły mapowania JPA nie mogą osłabiać hermetyzacji agregatu, przenosić reguł biznesowych do encji persystencji ani odwracać kierunku zależności.
- Techniczne identyfikatory bazy danych nie zastępują domenowych typów identyfikatorów na granicy Domain.
- Migracja Flyway należy do kontekstu będącego właścicielem zmienianych obiektów schematu. Migracje mechanizmów współdzielonych należą do `SharedKernel`.
- Wersje migracji są unikatowe w połączonej historii Flyway aplikacji.
- Start aplikacji lub jawne zadanie Gradle stosuje jeden uporządkowany zestaw oczekujących migracji ze wszystkich zarejestrowanych lokalizacji. Katalog kontekstu oznacza własność, nie osobne wykonanie.

## 10. Wstrzykiwanie zależności i konfiguracja

- Standardem jest wstrzykiwanie przez konstruktor Springa. Wstrzykiwanie przez pola jest niedozwolone.
- Skanowanie komponentów rozpoczyna się w `com.bartoszcichecki.pragmaticjvmdemo` i respektuje granice pakietów.
- Typy Domain nie używają adnotacji komponentów Springa. Konfiguracja Infrastructure może jawnie wystawić fabrykę, politykę lub usługę domenową jako bean, zachowując wstrzykiwanie przez konstruktor i niezależność typu domenowego od Springa.
- Konfiguracja Springa, mapowanie persystencji, adaptery i powiązania portów należą do Infrastructure kontekstu, którego dotyczą.
- Jawna konfiguracja beanów jest wymagana, gdy konwencja nie wyraża wyboru implementacji, kwalifikowanej kolekcji, dekoratora, wartości środowiskowej albo konfiguracji biblioteki.
- Globalna konfiguracja obejmuje wyłącznie elementy rzeczywiście wspólne. Szczegóły modułu pozostają w module.
- Produkcyjny bean nie otrzymuje szerszej widoczności ani mutowalności wyłącznie na potrzeby testów. Testy używają kompozycji przez konstruktor, atrap lub konfiguracji testowej Springa.

## 11. HTTP i trasy platformowe

- HTTP jest adapterem wejściowym. API używa JSON i prefiksu ścieżki `/api`.
- Publiczny kontrakt endpointu obejmuje ścieżkę, metodę HTTP, input, status odpowiedzi i payload JSON. Zmiana któregokolwiek elementu wymaga aktualizacji testów zachowania i świadomego przeglądu zgodności.
- Nazwa trasy jest wewnętrznym kontraktem routingu i bezpieczeństwa, nie publicznym kontraktem HTTP.
- Prefiks nazwy `platform_` jest zarezerwowany dla endpointów dostępnych wyłącznie na poziomie platformy.
- Endpoint platformowy nie wymaga wyboru aktywnego klienta, ale ogólne kontrole żądania i źródła są wykonywane przed pominięciem obsługi tenanta.
- Endpoint platformowy wymaga uwierzytelnionego administratora platformy; inny uwierzytelniony użytkownik otrzymuje HTTP 403.
- Status administratora jest ustalany podczas uwierzytelniania z jawnie skonfigurowanej allowlisty i nigdy nie jest przyjmowany z danych żądania.
- Test oparty na metadanych routingu Springa sprawdza, że nazwa zawierająca `platform` zaczyna się od `platform_`.
- Dodanie lub zmiana trasy wymaga przeglądu nazewnictwa platformowego, mapowania ról, zasad pomijania tenanta i handlerów zależnych od trasy.

## 12. Reguły ArchUnit

Testy ArchUnit są wykonywalnym odpowiednikiem reguł zależności. Muszą obejmować następujące ograniczenia, gdy w repozytorium istnieje kod, którego dotyczy reguła:

- klasy `domain` nie zależą od `application`, `infrastructure` ani `ui` oraz nie zależą od Spring MVC ani `JdbcClient`;
- klasy `application` nie zależą od `infrastructure` ani `ui` i nie zależą od portów Outside;
- klasy `ui` nie uzyskują dostępu do typów persystencji ani `JdbcClient`;
- klasy `infrastructure` nie zawierają zależności zwrotnej z innych warstw i nie są importowane przez Domain ani Application;
- `SharedKernel` nie zależy od kontekstów biznesowych;
- Domain i Application jednego kontekstu nie importują Domain ani Application innego kontekstu;
- dozwolona zależność między kontekstami znajduje się w Infrastructure konsumenta i wskazuje publiczny kontrakt właściciela;
- nie występują cykle zależności między kontekstami ani warstwami.

Reguła może tolerować brak pakietu w pustym module, ale musi automatycznie objąć pierwszą klasę dodaną do tego pakietu. Wyjątki techniczne, takie jak dopuszczone adnotacje mapowania w Domain, mają być wąsko ograniczone, jawne i testowalne. Własność tabel i treść SQL, których nie da się wiarygodnie ocenić z bytecode, podlegają obowiązkowemu review i ukierunkowanym testom integracyjnym. Kontrole zależne od metadanych czasu wykonania, na przykład nazewnictwo tras, są realizowane przez test kontekstu Springa, jeżeli analiza statyczna nie wystarcza.

## 13. Strategia testów

- **Testy jednostkowe Domain:** JUnit 6 sprawdza zachowanie agregatów, value objects, fabryk i polityk, w tym niezmienniki, istotne ścieżki błędów i rejestrowane zdarzenia. Testy używają atrap Outside i deterministycznego `Instant`.
- **Testy integracyjne:** Spring Boot Test i Testcontainers z PostgreSQL sprawdzają adaptery repozytoriów, mapowanie JPA, zapytania `JdbcClient`, migracje, dziennik zdarzeń, granice transakcji oraz zachowanie outboxa, jeżeli zmiana dotyczy tych mechanizmów.
- **Testy zachowania:** JUnit 6 i Spring Boot Test obejmują co najmniej pozytywną ścieżkę kompletnego przepływu biznesowego przez UI, Application, Domain i Infrastructure oraz istotne zachowania błędne wynikające z reguł przypadku użycia.
- **Testy architektury:** ArchUnit sprawdza statyczne granice warstw i kontekstów, a ukierunkowane testy kontekstu Springa — konwencje widoczne dopiero w czasie wykonania.

Oddzielny test mapowania nie jest wymagany dla każdego agregatu, jeżeli miarodajny test integracyjny lub zachowania wykonuje utrwalenie, `flush`, ponowne wczytanie i zachowanie. Dedykowany test jest wymagany, gdy mapping nie ma takiego pokrycia albo jest nietrywialny.

Konwencje testów zachowania:

- scenariusze używają czytelnych aliasów zamiast surowych UUID;
- **Given** przygotowuje stan przez polecenia lub przypadki użycia Application, nigdy przez HTTP;
- **When** wykonuje testowaną akcję HTTP;
- **Then** weryfikuje stan przez query service/read model oparty na `JdbcClient`; HTTP w tej fazie służy wyłącznie do asercji odpowiedzi i mapowania błędów;
- współdzielone przygotowanie może używać małego rejestru mapującego aliasy na identyfikatory, gdy powtarzalność uzasadnia taką abstrakcję;
- testy sprawdzają obserwowalne zachowanie i kontrakty, nie prywatne szczegóły implementacji.

## 14. Bramki jakości

Gradle Wrapper jest jedynym repozytoryjnym punktem wejścia do budowania i kontroli jakości kodu JVM, a konfiguracja budowania używa Kotlin DSL. Każda zmiana uruchamia bramki odpowiednie do swojego zakresu, w następującej kolejności:

1. formatowanie i lintowanie Kotlin;
2. analizę statyczną detekt oraz kompilację;
3. testy architektury ArchUnit;
4. testy jednostkowe i integracyjne;
5. testy zachowania, jeżeli zmiana wpływa na UI lub pełny przepływ.

Każda bramka musi zakończyć się pełnym wynikiem i poprawnym kodem wyjścia przed uruchomieniem następnej. Po błędzie naprawia się przyczynę i ponawia wszystkie dotknięte bramki. Zbiorcze zadanie Gradle nie może pomijać testów integracyjnych lub zachowania ani ukrywać źródła błędu.

Zmiana wprowadzająca element objęty regułą architektury lub poziomem testów musi równocześnie dostarczyć odpowiednią automatyczną kontrolę. Dla zmiany wyłącznie dokumentacyjnej minimalną bramką jest:

```text
git diff --check
```

## 15. Zmiana architektury

- Decyzja sprzeczna z tym kontraktem wymaga osobnego ADR opisującego kontekst, alternatywy i konsekwencje.
- Zaakceptowana zmiana architektury aktualizuje równocześnie obie wersje językowe dokumentu i odpowiadające jej testy ArchUnit lub inne automatyczne kontrole.
- Odstępstwo lokalne nie może być ukryte jako szczegół implementacji. Musi mieć wąski zakres i jawne uzasadnienie obok kodu lub w ADR.
- Konkretne zadania implementacyjne, kolejność prac i status realizacji należą do backlogu, nie do tego dokumentu.
