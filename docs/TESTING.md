# Verifiche

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

I test JVM coprono priorità del blocco globale, canali mancanti, stati sintetici, filtro, selezione visibile dopo il riordino, raggruppamento per URI e avanzamento della coda manuale.

## Risultati

- **Superati** — compilazione e lint con JDK 17 e SDK 36. Lint con 0 errori e 16 avvisi. Otto test JVM superati, inclusa la classificazione con inventario parziale.
- **Superata** — prova su emulatore Android 11, API 30. Installazione, avvio a freddo, ricerca, filtro, selezione dopo riordino, apertura guidata di due pagine notifiche distinte, percorso di ritorno, stato vuoto di **Suoni** e inclusione delle app di sistema. La prova ha aperto le pagine senza cambiare impostazioni di notifica. L'APK finale si è avviato conservando la preferenza per le app di sistema.
- **Osservato su Galaxy A34 5G** — Android 16, API 36, proprietà One UI `80500`. Due app con `POST_NOTIFICATIONS` concesso comparivano entrambe come **Da verificare**. Per la prima, le impostazioni native mostravano il consenso attivo e tre categorie visibili abilitate. Per la seconda, One UI mostrava il consenso spento e un avviso di sospensione avanzata. La sezione **Suoni** di NotiControl risultava vuota. Nel log recente del processo, durante la consultazione, erano assenti indicatori di crash o diniego di accesso.
- **Verificato il listener su emulatore API 30** — con un’app di prova dotata di due canali, la vista Suoni mostrava il canale della notifica presente nel gruppo silenzioso. Il canale inutilizzato era assente. La revoca ha rimosso il risultato dalla schermata. Sul Galaxy A34 aggiornato, con accesso concesso, verificati il nuovo filtro e la presenza di un gruppo sonoro con un canale osservato dopo l’aggiornamento dell’elenco.
- **Non eseguiti** — Android 13, 14, 15 e 17, cambio e rilettura del permesso runtime, verifica degli URI delle suonerie e profili di lavoro.

## Prova su dispositivo

1. Apri **App**, cerca un package visibile e controlla i conteggi dei filtri.
2. Dal menu **Opzioni** mostra le app di sistema. Seleziona due app, cambia ordinamento e controlla che la selezione mantenga l'identità. Applica un filtro e controlla il numero di elementi visibili nella selezione.
3. Conferma **Apri impostazioni** su due app. Controlla la prima pagina Android, torna in NotiControl e apri la successiva.
4. Apri **Suoni** e controlla che in assenza di canali accessibili venga indicato come raggiungere le impostazioni native.
5. Su Android 13 o successivo, prova anche il cambio manuale del permesso di un'app di test e controlla la rilettura al rientro. Sul Galaxy controlla le categorie e il percorso applicabile alla versione One UI osservata.

Le prove di modifica collettiva di canali, ripristino, Shizuku e profili di lavoro richiedono un backend dedicato e un ambiente compatibile.
