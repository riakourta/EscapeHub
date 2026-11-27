\# EscapeHub



Συνεργατικό παιχνίδι απόδρασης σε μουσείο, με Spring Boot,

PostgreSQL και συγχρονισμό παικτών μέσω WebSockets.



\## Απαιτήσεις



\- Docker Desktop εγκατεστημένο και σε λειτουργία.

\- Browser.

\- Σύνδεση στο Internet για την πρώτη λήψη των Docker images.



Δεν απαιτείται τοπική εγκατάσταση Java, Maven ή PostgreSQL.



\## Εκκίνηση



Από τον φάκελο του project:



```bash

docker compose up -d

```



Ανοίξτε στον browser:



http://localhost:8080/login.html



Το Compose ξεκινά την εφαρμογή και την PostgreSQL.

Η εφαρμογή περιμένει να είναι έτοιμη η βάση.



Το έτοιμο image της εφαρμογής είναι:



```text

ghcr.io/riakourta/escapehub:latest

```



Η ανώνυμη λήψη προϋποθέτει ότι το package έχει δημοσιευτεί

και η ορατότητά του έχει οριστεί σε Public.



\## Δοκιμή multiplayer



1\. Δημιουργήστε τουλάχιστον τρεις διαφορετικούς λογαριασμούς.

2\. Χρησιμοποιήστε ξεχωριστά προφίλ browser ή, για παράδειγμα,

&#x20;  Chrome, ανώνυμη περιήγηση και Edge.

3\. Επιλέξτε nickname και διαφορετικό χρώμα για κάθε παίκτη.

4\. Μπείτε όλοι στο lobby και ξεκινήστε την αποστολή.



Οι απλές καρτέλες του ίδιου προφίλ browser μοιράζονται

τα στοιχεία σύνδεσης μέσω localStorage.



Όλοι οι παίκτες πρέπει να συνδέονται στον ίδιο server.

Ξεχωριστές εγκαταστάσεις Docker έχουν ξεχωριστή κατάσταση παιχνιδιού.



\## Τοπικό build από τον κώδικα



Για να δοκιμάσετε τις τοπικές αλλαγές:



```bash

docker compose up -d --build

```



\## Ενημέρωση στην τελευταία δημοσιευμένη έκδοση



```bash

docker compose pull

docker compose up -d

```



\## Διακοπή



```bash

docker compose down

```



Τα δεδομένα λογαριασμών παραμένουν στο PostgreSQL volume.

Η ενεργή κατάσταση του παιχνιδιού διατηρείται στη μνήμη

της εφαρμογής και χάνεται με την επανεκκίνησή της.



\## Έλεγχος λειτουργίας



```bash

docker compose ps

docker compose logs --tail=100 app

```



Οι θύρες 8080 και 5432 πρέπει να είναι διαθέσιμες στον υπολογιστή.



\## CI και δημοσίευση image



Το GitHub Actions workflow βρίσκεται στο:



```text

.github/workflows/ci.yml

```



Σε push και pull request εκτελεί:



1\. Build και αυτοματοποιημένα tests με Java 17 και PostgreSQL.

2\. Docker image build, εφόσον περάσει το πρώτο job.



Σε push στη main δημοσιεύει επιπλέον στο GitHub Container Registry:



\- latest

\- tag με το SHA του commit



Μετά την πρώτη δημοσίευση, ο διαχειριστής πρέπει να ορίσει

το package ως Public από τα Package settings στο GitHub.



\## Τοπικές ρυθμίσεις



Τυχόν docker-compose.override.yml αφορά μόνο την εγκατάσταση

του συγκεκριμένου υπολογιστή και δεν χρειάζεται να μεταφερθεί

στον υπολογιστή αξιολόγησης.



Οι προεπιλεγμένες ρυθμίσεις βάσης προορίζονται για τοπική επίδειξη.

