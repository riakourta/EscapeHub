### Sprint 0 - Υλοποίηση Frontend Таυτοποίησης & Προφίλ (Κιούσης)
- **Σκοπός χρήσης AI:** Δημιουργία αρχικού σκελετού για τις σελίδες `login.html`, `profile.html`, τον HTTP client (`apiClient.js`), τη λογική ταυτοποίησης (`auth.js`) και τα αυτοματοποιημένα unit tests (`UI_Tests.js`).
- **Τι απορρίφθηκε / διορθώθηκε (Εντοπισμός σφάλματος AI):**
    1. Στην αρχική πρόταση του AI υπήρχε hardcoded fallback τιμή για το JWT token (`dummy-jwt-token-until-auth-ready`) σε περίπτωση που το backend δεν επέστρεφε token, κάτι που αποτελούσε κίνδυνο ασφαλείας και κακή πρακτική. Απορρίφθηκε και αντικαταστάθηκε από την `extractAndSaveJwtToken()`, η οποία πετάει αυστηρό Error αν λείπει το token.
    2. Αρχικά το AI χρησιμοποίησε το πεδίο `maskColor`, το οποίο διορθώθηκε σε `avatarColor` μετά από έλεγχο συμβατότητας με το `UpdateProfileDto.java` του backend.
    3. Αφαιρέθηκαν τα hardcoded routes και επιλογές από τη JavaScript και μεταφέρθηκαν δυναμικά σε `data-*` attributes και παραμετροποιήσιμο configuration (`API_CONFIG`).
- **Τρόπος επαλήθευσης:** Εκτέλεση 6 αυτοματοποιημένων unit tests μέσω Node.js (`node src/main/resources/static/UI_Tests.js`), τα οποία επιβεβαιώνουν την απόρριψη κενών στοιχείων και την απόρριψη απόκρισης χωρίς έγκυρο JWT token.
- ### Sprint 1 - Ολοκλήρωση Login & Προφίλ 6 Χρωμάτων (Κιούσης - Issue #2)
- **Σκοπός χρήσης AI:** Δημιουργία της σελίδας ταυτοποίησης (`login.html`), αναβάθμιση του `profile.html` με επιλογή 6 χρωμάτων και ευθυγράμμιση των `apiClient.js` και `auth.js` με τα REST endpoints και το WebSocket Lobby.
- **Τι απορρίφθηκε / διορθώθηκε (Εντοπισμός σφάλματος AI):**
  1. Στην αρχική υλοποίηση, το `apiClient.js` χρησιμοποιούσε τα keys `jwt_token` και `player_avatarColor` στο `localStorage`, τα οποία δεν ταίριαζαν με τα `jwtToken`, `username` και `playerColor` που διαβάζει το `lobbySocket.js`. Εντοπίστηκε κατά τον έλεγχο διασύνδεσης και διορθώθηκε.
  2. Αφαιρέθηκαν οι hardcoded τιμές και επιλογές χρωμάτων από τη JavaScript (`auth.js`) και μεταφέρθηκαν δυναμικά σε `data-color` και `data-*` attributes στο HTML.
- **Τρόπος επαλήθευσης:** Εκτέλεση 6 αυτοματοποιημένων unit tests μέσω Node.js (`node src/main/resources/static/UI_Tests.js` - όλα PASS).
## Sprint 1 - PostgreSQL Integration, Profile Customization & Tests (Ροζαλία Πολυγυρινού)
- **Εργαλείο AI:** Gemini
- **Περιγραφή Χρήσης:**
  - Ζητήθηκε βοήθεια για τη ρύθμιση του `Dockerfile`, του `docker-compose.yml` και του `application.properties` ώστε η εφαρμογή να συνδέεται με βάση δεδομένων PostgreSQL.
  - Ενσωματώθηκε η σχέση `@OneToOne` μεταξύ `User` και `Profile`, η αυτόματη δημιουργία προφίλ κατά την εγγραφή (`AuthService.register`), και το validation των 6 επιτρεπόμενων χρωμάτων (`RED`, `BLUE`, `GREEN`, `YELLOW`, `PURPLE`, `PINK`) και του nickname στο `ProfileService` / `ProfileController`.
  - Ενημερώθηκαν τα unit και controller tests (`AuthTests`, `ProfileTests`) και προστέθηκε η ρύθμιση `maven-surefire-plugin` (`-Dnet.bytebuddy.experimental=true`) στο `pom.xml`.
- **Επαλήθευση & Τροποποιήσεις:**
  - Προσαρμόστηκαν οι constructors των `AuthService` και `ProfileController` για σωστό dependency injection, ορίστηκε το χρώμα `PINK` αντί για `ORANGE` σε συνεννόηση με το frontend, και επαληθεύτηκε τοπικά η σύνδεση με PostgreSQL 16 (μέσω Postgres.app) και η επιτυχής εκτέλεση όλων των tests (`./mvnw test` -> 13/13 passed).