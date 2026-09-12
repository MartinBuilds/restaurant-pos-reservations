# restaurant-pos-reservations

Система за управление на ресторант: POS операции и резервации на маси.
Изградена като един Spring Boot **модулен монолит**.

**Автор:** Martin Andonov Kolev

## Общ преглед

Приложението покрива ежедневната работа на ресторант:

- автентикация на персонал и роли
- меню и склад с наличност по рецепти
- приемане на поръчки от сервитьор със сваляне на склад
- кухненски работен поток с известия в реално време
- резервации на маси за клиенти и персонал
- симулирани плащания CASH/CARD и оперативни справки за продажби
- четири браузърни интерфейса по роли (ADMIN, WAITER, COOK, CLIENT)

REST + MySQL са източникът на истина. WebSocket/STOMP съобщенията са само известия.

## Възможности

- Spring Security със сесии и CSRF
- Изолирани интерфейси за ADMIN / WAITER / COOK / CLIENT
- Каталог меню + автоматична наличност от рецепти и склад
- Работен поток на поръчка: ACCEPTED → COOKING → READY → SERVED
- Резервации: наличност, създаване, пренасрочване, отказ с проверка за конфликти
- Симулирани плащания и бонове (не са фискални / не са реален PSP)
- Оперативни справки за продажби за ADMIN
- Опционален профил `demo` за демонстрационни seed данни

## Технологии

- Java 17
- Spring Boot 4.1 (Web MVC, Data JPA, Security, WebSocket)
- Spring Security Messaging (СТОМП оторизация)
- MySQL 8+
- Maven Wrapper
- HTML5 / CSS3 / vanilla JavaScript (ES modules, Fetch API)
- Опционално Node.js/npm **само** за удобни скриптове (`npm run dev:demo`) — самото приложение не ползва Node

В самото приложение няма React/Angular/Vue, Docker, H2, Lombok, Flyway или външни payment SDK-та.

## Архитектура

Виж [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

Накратко: браузърни UI → REST контролери → услуги → хранилища → MySQL. След commit на поръчка се публикуват AFTER_COMMIT събития към STOMP теми; интерфейсите се обновяват през REST.

## Роли и демо входове

| Роля | UI | Имейл | Парола |
|---|---|---|---|
| ADMIN | `/admin` | `maria.adminova@example.com` | `SecurePassword123!` |
| WAITER | `/waiter` | `georgi.stoyanov@example.com` | `SecurePassword123!` |
| COOK | `/kitchen` | `ivan.petkov@example.com` | `SecurePassword123!` |
| CLIENT | `/client` | `elena.dimitrova@example.com` | `SecurePassword123!` |

Обща парола за всички seed акаунти за презентация: **`SecurePassword123!`**

API префикси:

| Роля | API |
|---|---|
| ADMIN | `/api/admin/**` (+ други оперативни зони според конфигурацията) |
| WAITER | `/api/waiter/**`, `/operations/**` |
| COOK | `/api/kitchen/**`, `/operations/**` |
| CLIENT | `/api/client/**` |

Приоритет при пренасочване след вход: ADMIN → WAITER → COOK → CLIENT.

Seed потребителите се създават, когато е зададен `DEMO_USER_PASSWORD` (чрез `smoke-env*.ps1`) и приложението тръгне с профил `demo`. Саморегистрираните клиенти запазват собствените си пароли.

## Основни бизнес потоци

1. **Поръчка:** WAITER създава поръчка → сваля се склад → масата става OCCUPIED → кухнята се известява → COOKING/READY → WAITER маркира SERVED → плащане → поръчката се затваря → масата става AVAILABLE (или пак RESERVED при активна резервация).
2. **Резервация:** CLIENT (или ADMIN) запазва свободен интервал за маса → CONFIRMED → по желание пренасрочване → отказът освобождава слота.
3. **Наличност:** ефективната наличност на артикул = ръчен флаг И рецепта И активни съставки И достатъчен склад.

## База данни

MySQL 8+ с JPA `ddl-auto=update`. Подробности и ER диаграма: [docs/DATABASE.md](docs/DATABASE.md).

## Сигурност

- BCrypt хешове на пароли
- Session cookies + CSRF за променящи HTTP заявки и STOMP CONNECT
- HTTP оторизация по роли
- Оторизация на STOMP теми; входящ business SEND е забранен
- Хешове на пароли никога не се връщат в API JSON
- Безопасни API грешки (без stack traces)

## WebSocket / STOMP

- Endpoint: `/ws` (с автентикация)
- Кухненска тема: `/topic/kitchen/orders` (COOK/ADMIN)
- Сервитьорска тема: `/topic/waiter/orders` (WAITER/ADMIN)
- Тема за маси: `/topic/waiter/tables` (WAITER/ADMIN)
- CLIENT няма оперативни абонаменти
- Известията не са трайни; при reconnect възстановяването е през REST

## Дисклеймер за симулирани плащания

Методите `CASH` и `CARD` са **локални симулации**.

`CARD` означава:

- локална enum стойност
- локален ред за плащане в MySQL
- **без** номер на карта / CVV / титуляр
- **без** платежен доставчик, банкова авторизация или реално движение на пари

Боновете са само оперативни симулационни документи — **не** са фискален бон, данъчна фактура или банков документ.

## Справки за продажби

Оперативни агрегати за ADMIN (обобщение, по артикул, по метод на плащане) върху платена/затворена активност. Не са счетоводни или данъчни декларации.

## UI маршрути

| UI | URL | Бележки |
|---|---|---|
| ADMIN | `/admin` | Vanilla JS модули; само REST |
| WAITER | `/waiter` | Споделени `/operations/**` + STOMP |
| COOK | `/kitchen` | Споделени `/operations/**` + STOMP |
| CLIENT | `/client` | Само REST; без WebSocket |

### Общи UI предпочитания (shell)

И четирите ролеви интерфейса споделят общ слой под `/shared/**`:

- **Езици:** български (`bg`, по подразбиране) и английски (`en`)
- **Теми:** `system` (по подразбиране), `light`, `dark` — CSS променливи чрез `html[data-theme]`
- **Акаунт меню:** долу/странично (или горе) с аватар от инициали, тема, език, изход
- **Текущ потребител:** `GET /api/account/me` (автентикиран) връща `id`, `name`, `email`, `roles` — никога парола/хеш
- **ADMIN странично меню:** свиваемо на десктоп; иконите остават видими при свиване; мобилният drawer остава ползваем
- **Ключове в localStorage (само UI):**
  - `restaurant.ui.theme`
  - `restaurant.ui.language`
  - `restaurant.ui.sidebar.collapsed`

**Не** съхранявайте пароли, session id, CSRF токени или auth токени в `localStorage`. Профилът се зарежда от API за активната сесия и не се пази като credentials.

Споделените статични ресурси (`/shared/**`) са permit-all за ранен theme bootstrap; ролевите HTML/API маршрути остават защитени по роля.

## Локална настройка

### Изисквания

- Java 17+
- MySQL 8+
- Maven Wrapper (включен в проекта)
- Node.js/npm (по желание — само ако предпочитате `npm run …` вместо директно извикване на скриптовете)

### Променливи на средата

| Променлива | Задължителна | Описание |
|---|---|---|
| `DB_URL` | не (има default) | JDBC URL |
| `DB_USERNAME` | не (default `restaurant_app`) | MySQL потребител |
| `DB_PASSWORD` | **да** | MySQL парола (`SecurePassword123!` в примера) |
| `RESTAURANT_TIME_ZONE` | не (default `Europe/Sofia`) | Часова зона за резервации |
| `INITIAL_ADMIN_EMAIL` | по желание | Seed на първи ADMIN |
| `INITIAL_ADMIN_PASSWORD` | по желание | Парола на seed ADMIN (съхранява се като BCrypt) |
| `INITIAL_ADMIN_FULL_NAME` | по желание | Име за показване на seed ADMIN |
| `DEMO_USER_PASSWORD` | само за demo | Обща парола за seed потребители за презентация |

Виж `src/main/resources/application-example.properties` и `smoke-env.example.ps1`. Не комитвайте реални продукционни тайни.

### Настройка на локалния env

```powershell
Copy-Item .\smoke-env.example.ps1 .\smoke-env.ps1
```

Създайте MySQL база `restaurant_management` и потребител `restaurant_app` с парола **`SecurePassword123!`** (същата стойност като в примерния файл).

## Стартиране на приложението

### Препоръчително (демо / презентация)

```powershell
npm run dev:demo
```

Това зарежда `smoke-env.ps1` (или example файла), стартира Spring Boot с профил `demo` и създава презентационните потребители по-горе.

### Нормално стартиране (без demo seed)

```powershell
npm run dev
```

### Еквивалент без npm

```powershell
.\scripts\dev-demo.ps1
# или
.\scripts\dev.ps1
```

Отворете `http://localhost:8080/login` и влезте с която и да е роля от таблицата по-горе.

Каталогът и масите ползват реалистични български имена (идемпотентно). Demo **не** зарежда плащания или голяма история. Ако липсва `DEMO_USER_PASSWORD`, demo потребителите се пропускат (само предупреждение в лога).

## Тестове

```powershell
npm test
```

Или: `.\scripts\test.ps1` / `.\mvnw.cmd clean test`.

Повече подробности: [docs/TESTING.md](docs/TESTING.md).

## API преглед

Виж [docs/API.md](docs/API.md).

## Известни ограничения / бъдеща продукционна работа

Тази академична/демо система **не** включва:

- реален платежен шлюз или фискално устройство
- модули за ДДС/данъчно съответствие
- имейл/SMS известия
- нулиране на парола / анонимна публична резервация
- разпределен message broker / outbox / replay на WebSocket събития
- Flyway/Liquibase миграции
- Docker пакетиране за деплой
- продукционен observability / rate limiting
- външен identity provider

Не третирайте симулираните CARD плащания или боновете като реални финансови или фискални документи.

## Структура на проекта

```text
src/main/java/bg/martinandonov/restaurant/
  common/ security/ user/ account/ menu/ inventory/ diningtable/
  order/ kitchen/ reservation/ payment/ report/ demo/ client/
src/main/resources/static/
  admin/ waiter/ kitchen/ client/ operations/ shared/
docs/
  ARCHITECTURE.md DATABASE.md API.md TESTING.md DEMO.md PRESENTATION_QA.md
```

## Инструкции за презентация / демо

Следвайте [docs/DEMO.md](docs/DEMO.md) и подгответе отговори с [docs/PRESENTATION_QA.md](docs/PRESENTATION_QA.md).

## Допълнителна документация

| Документ | Съдържание |
|---|---|
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Модулен монолит, WebSocket поток, конкурентност |
| [DATABASE.md](docs/DATABASE.md) | Ентитети, ER диаграма, ограничения |
| [API.md](docs/API.md) | API карта по роли |
| [TESTING.md](docs/TESTING.md) | Автоматизирани + smoke тестове |
| [DEMO.md](docs/DEMO.md) | Сценарий за презентация (8–12 мин.) |
| [PRESENTATION_QA.md](docs/PRESENTATION_QA.md) | Въпроси и отговори за защита |
