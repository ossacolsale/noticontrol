# NotiControl

App Android offline per consultare le impostazioni di notifica delle app visibili e raggiungere rapidamente i comandi di Android. Funziona senza root, Shizuku, accesso alle notifiche o permessi aggiuntivi.

In **App** puoi cercare, filtrare e selezionare più app. **Apri impostazioni** avvia un percorso guidato con una pagina Android per app. Applica le modifiche in Android, torna a NotiControl e tocca **Apri successiva**. La vista **Suoni** raggruppa per URI i canali letti con le autorizzazioni disponibili. Da **Suoni → Apri accesso alle notifiche** puoi autorizzare il rilevamento facoltativo dei canali usati dalle notifiche attualmente presenti. **Aggiorna elenco** rilegge il profilo corrente. I risultati vivono in memoria. Per vedere anche le categorie inattive apri le impostazioni native dell’app. Il filtro **Permesso consentito** identifica le app abilitate con permesso concesso e inventario dei canali incompleto. Le modifiche collettive automatiche e il ripristino richiedono un backend privilegiato verificato, assente in questa versione.

## Build e installazione

Servono JDK 17, Android SDK Platform 36, Build Tools 35.0.0 e Android SDK Platform Tools. Per la prima build Gradle deve poter scaricare le dipendenze. Usa `ANDROID_HOME` o il file locale `local.properties` per indicare il percorso dell'SDK.

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Sul Galaxy A34 5G, controlla prima Android e One UI in **Impostazioni → Informazioni sul telefono → Informazioni software**. Collega il telefono con il debug USB attivo e autorizza il computer. In alternativa, copia l'APK sul telefono e aprilo con un gestore file autorizzato a installare APK. Su One UI, se la pagina dell'app omette le categorie, cerca **Gestisci categorie di notifica per ogni app** in **Impostazioni → Notifiche → Impostazioni avanzate**. [Guida Samsung](https://www.samsung.com/uk/support/mobile-devices/what-to-do-if-notifications-are-not-showing-or-there-is-no-sound-on-my-samsung-galaxy-device/)

## Documentazione

- [Macro guida di prodotto](docs/PRODUCT.md) — obiettivo, requisiti e criteri di accettazione.
- [Fattibilità](docs/FEASIBILITY.md) — capacità, permessi e vincoli Android.
- [Architettura](docs/ARCHITECTURE.md) — modello, dati e flusso verso Android.
- [Verifiche](docs/TESTING.md) e [limiti noti](docs/LIMITS.md).
