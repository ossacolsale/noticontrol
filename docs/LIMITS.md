# Limiti noti

- L'inventario copre i package visibili a NotiControl nel profilo corrente. Sulle app altrui la lettura del permesso di notifica richiede Android 13 o successivo e la relativa dichiarazione nel package. Un permesso concesso può coesistere con canali disattivati.
- La vista **Suoni** è vuota quando mancano canali leggibili. Il backend standard può leggere i canali propri, se esistono. NotiControl non crea canali artificiali per popolare l'elenco.
- La coda apre pagine Android su azione esplicita. Un'apertura riuscita indica l'avvio della pagina, senza attestare il cambio di impostazioni. Per scritture dirette, modifiche collettive, snapshot, ripristino e accesso a profili di lavoro serve una capacità aggiuntiva da verificare.
- MinSdk 26, compileSdk e targetSdk 36. API 36 è la piattaforma stabile installata e supportata dalla toolchain usata. Il comportamento su API 37 e One UI sul Galaxy A34 resta da verificare su un dispositivo con la versione effettivamente installata.
