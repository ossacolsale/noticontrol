# AGENTS.md — NotiControl

## Mandato

- App Android nativa per inventariare e gestire le impostazioni delle notifiche. La [macro guida di prodotto](docs/PRODUCT.md) definisce requisiti, priorità e accettazione.
- Priorità a sicurezza e correttezza, poi a perimetro, compatibilità, semplicità e consumo di risorse. La direttiva di efficienza governa profondità di documentazione, test e architettura.
- Rispondi in italiano. Comunica risultati significativi, decisioni e blocchi. Chiedi chiarimenti solo per ambiguità sostanziali, operazioni distruttive, modifiche non previste a dati reali o costi rilevanti.

## Esecuzione

- Controlla `git status --short --branch` e i file pertinenti prima di modificare. Preserva il lavoro esistente. Parti da ricerche mirate, riusa contesto verificato e lavora direttamente senza agenti secondari.
- Completa il più piccolo intervento coerente con l'obiettivo. Riutilizza API e convenzioni del progetto. Aggiungi dipendenze, livelli e automazioni solo per esigenze attuali. Segnala separatamente problemi estranei.
- Consulta solo le sezioni pertinenti di [README.md](README.md), della macro guida e delle note tecniche eventualmente collegate nel README. Conserva requisiti, vincoli, istruzioni e prove nei rispettivi documenti. Aggiorna solo quelli interessati.
- Privilegia API pubbliche e percorsi nativi. Consulta Android Developers e AOSP per dubbi che cambiano una scelta concreta. Prima di usare un backend privilegiato verifica contratto, prerequisiti, profilo, revoca e rilettura. Rappresenta le capacità mancanti nell'interfaccia.
- Mantieni privacy offline, consenso per le modifiche e protezione dei dati. Evita contenuti delle notifiche, log sensibili, credenziali e artefatti locali in Git.

## Verifica e pubblicazione

- Scegli i controlli pertinenti in base a comportamento e rischio. Per un incremento Android significativo compila e verifica la logica interessata. L'ultima build significativa vale come verifica finale se il codice resta invariato. Verifica permessi, persistenza e integrità quando coinvolti. Riporta separatamente prove non eseguite, errori ambientali e regressioni.
- Diagnostica prima di riprovare. Preferisci al massimo due tentativi motivati, poi rivaluta la strategia. Prosegui sui blocchi critici pertinenti a build, sicurezza o perdita di dati. Evita suite ripetute, prove indisponibili e ritocchi facoltativi.
- A fine di **ogni ciclo di sviluppo**, controlla il diff, includi solo i file del ciclo, crea un commit descrittivo e fai push normale al ramo remoto tracciato. Questa pubblicazione Git è autorizzata. Riconcilia eventuali divergenze senza riscrivere la cronologia condivisa. Verifica l'esito e riporta commit e ramo, oppure il blocco preciso. Per tag, release e deployment serve autorizzazione specifica.
- Fermati quando l'obiettivo è implementato, verificato in modo proporzionato e privo di blocchi pertinenti. In chiusura riporta modifiche, verifiche superate, prove mancanti, limiti e azioni richieste.

Struttura operativa adattata da [AgentForge Universal AGENTS](https://github.com/ossacolsale/AgentForge-Universal-AGENTS.md/blob/main/AGENTS.md).
