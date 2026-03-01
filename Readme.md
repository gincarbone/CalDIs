

App CalDis - Calorie & Dieta Intelligente
Struttura (32 file creati)
Stessa architettura MVVM dell'app Risparmio, adattata al tracciamento alimentare.

Mapping Risparmio -> CalDis
Concetto	Risparmio	CalDis
Entrata	Spesa (EUR)	Pasto (kcal)
Uscita	Entrata (EUR)	Attivita' fisica (kcal bruciate)
Fissi in	Uscite fisse	Pasti ricorrenti
Fissi out	Entrate fisse	Attivita' ricorrenti
Budget	EUR/giorno	kcal/giorno (default 2000)
Colori	Blu/Viola	Verde/Teal
Schermate
Splash - Gradient verde/teal, icona ristorante
Calendario - Griglia con pallini arancione (pasti) e verdi (attivita'), semaforo budget calorico, gauge calorie rimaste, forecast mensile
Dashboard - Budget giornaliero kcal, consumate/rimaste oggi, pasti e attivita' ricorrenti, ultimi 10 movimenti
Dettaglio Giorno - CRUD pasti e attivita' + bottone fotocamera per stima AI
Statistiche - Donut chart per categoria cibo, trend 6 mesi, breakdown categorie
Stima da Foto (NUOVO) - Selezione foto -> analisi Gemini Vision -> lista alimenti con range kcal -> conferma e salvataggio
Impostazioni - API Key Gemini, budget calorico personalizzabile, export PDF, condividi app
Feature AI (Gemini Vision)
Flusso: foto piatto -> Gemini 2.0 Flash -> JSON con stima per alimento -> conferma -> salva come pasto
API key configurabile dalle Impostazioni (SharedPreferences)
Range min-max per ogni alimento identificato
Per buildare

cd caldis/android
./gradlew assembleDebug
Per configurare Gemini
Ottieni API key gratuita da aistudio.google.com
Aprire l'app -> Impostazioni -> inserisci API Key -> Salva