# Architettura

- Il modulo `app` usa Kotlin, Compose e Material 3. La UI mostra lo stato del `InventoryViewModel` e mantiene la selezione per identità. Alla conferma fotografa gli elementi selezionati visibili nell'ordine corrente.
- `AndroidInventory` enumera i package visibili. `CapabilityDetector` decide per package quali dati esporre e distingue dati mancanti da liste vuote. Lettura e icone operano fuori dal thread della UI. `Inventory.kt` contiene classificazione, raggruppamento, selezione e avanzamento della coda verificabili sulla JVM.
- Le preferenze locali conservano filtro, ordinamento e visibilità delle app di sistema. La coda di apertura vive nella schermata. Al rientro dalle impostazioni l'app rilegge l'inventario.
- Gli intent nativi ricevono solo il package e, quando noto, l'ID di canale. La coda registra aperture specifiche, generiche e non disponibili. Android gestisce le modifiche effettuate dall'utente. Il manifest non richiede accesso alle notifiche, visibilità completa dei package o accesso alla rete.
