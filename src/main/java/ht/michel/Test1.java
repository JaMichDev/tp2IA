package ht.michel;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

public class Test1 {
    public static void main(String[] args) {

        // Récupérer la clé API depuis les variables d'environnement
        String geminiKey = System.getenv("GEMINI_KEY");
        if (geminiKey == null) {
            System.out.println("La variable d'environnement GEMINI_KEY n'est pas définie.");
            return;
        }

        // Créer le modèle avec le pattern builder
        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(geminiKey)
                .modelName("gemini-flash-latest") // ←Remplace gemini-2.0-flash par gemini-flash-latest qui a des limites gratuites plus généreuses :
                .temperature(0.7)
                .build();


        // --- Test simple ---
        String reponse1 = model.chat("Quelle est la capitale de la France ?");
        System.out.println("Question : Quelle est la capitale de la France ?");
        System.out.println("Réponse  : " + reponse1);

        System.out.println();

        // --- Test : demander l'heure ---
        String reponse2 = model.chat("Quelle heure est-il ?");
        System.out.println("Question : Quelle heure est-il ?");
        System.out.println("Réponse  : " + reponse2);

        System.out.println();

        // --- Test : se présenter puis demander son nom ---
        // 1ère question : se présenter
        String question1 = "Bonjour, je m'appelle Michel.";
        String reponse3 = model.chat(question1);
        System.out.println("Question : " + question1);
        System.out.println("Réponse  : " + reponse3);

        System.out.println();

        // 2ème question : demander son nom
        // ATTENTION : model.chat() n'a pas de mémoire !
        // Chaque appel est indépendant, le LLM ne se souvient pas de la question précédente.
        String question2 = "Comment je m'appelle ?";
        String reponse4 = model.chat(question2);
        System.out.println("Question : " + question2);
        System.out.println("Réponse  : " + reponse4);

    }
}