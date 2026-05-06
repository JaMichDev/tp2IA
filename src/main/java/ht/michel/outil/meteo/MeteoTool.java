package ht.michel.outil.meteo;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Scanner;
import java.net.URI;

public class MeteoTool {
    @Tool("""
    Retourne true si la probabilité d'avoir de la pluie pour la ville dont on donne la latitude et la longitude
    est importante durant une période de nbJours jours. La période commence nbJourDebut jours après aujourd'hui.
    Par exemple, si nbJoursDebut = 1 et nbJours = 2, la période de 2 jours commence demain (1 jour après aujourd'hui). 
    Si nbJoursDebut = 0 et nbJours = 3, la période de 3 jours commence aujourd'hui.
    Retourne false sinon.
    """)
    public boolean isPluiePrevue(@P("Latitude") double latitude, @P("Longitude") double longitude,
                                 @P("Nombre de jours après aujourd'hui pour indiquer le début de la période") int nbJoursDebut,
                                 @P("Nombre de jours de la période") int nbJours) {
        // Seuil au-delà duquel la probabilité d'avoir de la pluie est suffisante pour prendre un parapluie.
        // A modifier éventuellement, en particulier pour faire des tests.
        double seuilPluie = 25.0;

        try {
            // Construire l'URL de l'API météo en utilisant les coordonnées et la période spécifiée
            String baseUrl = "https://api.open-meteo.com/v1/forecast";
            int finPeriode = Math.min(nbJoursDebut + nbJours, 7); // L'API fournit des prévisions pour 7 jours maximum
            String apiUri = String.format(Locale.US,
                    "%s?latitude=%f&longitude=%f&daily=precipitation_probability_max&forecast_days=%d",
                    baseUrl, latitude, longitude, finPeriode);

            // Ouvrir une connexion HTTP
            HttpURLConnection connection = (HttpURLConnection) new URI(apiUri).toURL().openConnection();
            connection.setRequestMethod("GET");

            // Lire la réponse
            Scanner scanner = new Scanner(connection.getInputStream());
            String response = scanner.useDelimiter("\\A").next();
            scanner.close();

            // Parser la réponse JSON qui a le format suivant :
            // {
            //   "latitude": 41.875,
            //   "longitude": 12.5,
            //   "generationtime_ms": 0.123,
            //   ...
            //   "daily": {
            //     "time": ["2024-06-01", "2024-06-02", "2024-06-03", ...],
            //     "precipitation_probability_max": [20, 50, 80, ...]
            //   }
            // }
            Gson gson = new Gson();
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

//            System.out.println("Réponse de l'API météo : " + response);

            // Extraire le tableau precipitation_probability_max
            JsonObject daily = jsonResponse.getAsJsonObject("daily");
            int[] precipitationProbabilities = gson.fromJson(
                    daily.get("precipitation_probability_max"),
                    int[].class
            );

            // Vérifier si au moins une probabilité dépasse le seuil pour la pluie pendant la période spécifiée
            // On commence à vérifier à partir de nbJoursAvantDebut jours avant le début de la période
            for (int i = nbJoursDebut; i < finPeriode; i++) {
                if (precipitationProbabilities[i] > seuilPluie) {
                    return true;
                }
            }
            // Si aucune probabilité de pluie ne dépasse le seuil, on retourne false
            return false;
        } catch (IOException e) {
            // En cas d'erreur, on considère qu'on ne peut pas déterminer la météo
            throw new RuntimeException("Erreur lors de la récupération de la météo pour ("
                    + latitude + ", " + longitude + ") : " + e.getMessage());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
