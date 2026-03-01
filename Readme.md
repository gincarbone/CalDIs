# CalDis
CalDis e' un'app Android (Kotlin + Jetpack Compose) per tracciare alimentazione e bilancio calorico in modo veloce, visivo e intelligente.

## Funzionalita' principali
- Acquisizione pasti principalmente da fotocamera (flusso AI-first).
- Stima calorie da foto con Gemini (range min/max per alimento + totale).
- Correzione manuale calorie prima del salvataggio.
- Categoria pasto suggerita automaticamente in base all'ora dello scatto:
  - Colazione, Pranzo, Cena, Snack.
- Gestione pasti e attivita' giornaliere (aggiunta, modifica, eliminazione).
- Dashboard giornaliera con budget, consumate, bruciate e residuo.
- Calendario mensile con indicatori visivi e semaforo rispetto al budget.
- Statistiche avanzate:
  - grafico `Settimanale`
  - grafico `Giornaliero`
  - linea soglia tratteggiata con valore in basso
  - marker motivazionale (faccina) quando i consumi sono sotto soglia.
- Ripartizione calorie per categoria (donut + dettaglio categorie).
- Budget dinamico con motore BMR/TDEE da Impostazioni.
- Pasti e attivita' ricorrenti.
- Export report PDF mensile.
- Condivisione APK.

## Flusso AI foto
1. L'utente apre il flusso foto (anche dal `+` principale).
2. La fotocamera puo' aprirsi automaticamente nella stessa pagina di anteprima.
3. La foto viene normalizzata (orientamento EXIF) e ridimensionata.
4. Gemini analizza l'immagine e restituisce JSON strutturato.
5. L'utente puo' regolare categoria e calorie finali.
6. Salvataggio nel diario.

### Ottimizzazioni AI implementate
- Selezione modello Gemini disponibile per API key (popup).
- Prompt ottimizzato e piu' sintetico per ridurre latenza.
- Telemetria tempi risposta AI (rete+inferenza/parsing).
- Supporto riferimento scala (es. carta) e peso stimato opzionale a fasce.

## Motore BMR/TDEE
In `Impostazioni` e' disponibile il calcolatore automatico budget calorico:
- Selezione sesso con icona.
- Eta', altezza e peso con controllo numerico grande (`-`, valore editabile, `+`).
- Livello attivita' a tendina.
- Obiettivo a tendina (deficit/mantenimento/surplus).
- Salvataggio automatico del budget giornaliero calcolato.

## Stack tecnico
- Kotlin
- Jetpack Compose (Material 3)
- Room (persistenza locale)
- MVVM + StateFlow
- Coroutines
- Coil
- Gemini API (`generativeai`)

## Build & install
```bash
cd android
./gradlew assembleDebug
./gradlew installDebug
```

## Configurazione Gemini
1. Ottieni una API key da `https://aistudio.google.com`.
2. Apri app -> `Impostazioni`.
3. Inserisci API key.
4. Carica modelli disponibili e seleziona un modello Flash.

## Note repository
- File locali sensibili/esclusi dal tracking:
  - `api.key`
  - `commands.txt`
  - directory build/cache.
