package ht.michel;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;

import java.util.Map;

public class Test3 {
    public static void main(String[] args) {
        String geminiKey = System.getenv("GEMINI_KEY");
        if (geminiKey == null) {
            System.out.println("La variable d'environnement GEMINI_KEY n'est pas définie.");
            return;
        }

        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(geminiKey)
                .modelName("gemini-flash-latest")
                .temperature(0.7)
                .build();

        // Définir un template de prompt avec une variable {{texte}}
        PromptTemplate template = PromptTemplate.from(
                "Traduis le texte suivant en anglais : {{texte}}"
        );

        // Texte à traduire
        String texteATraduire = "Bonjour, comment allez-vous ? J'espère que vous passez une bonne journée.";

        // Créer le prompt en remplaçant la variable
        Prompt prompt = template.apply(Map.of("texte", texteATraduire));

        System.out.println("Texte original : " + texteATraduire);
        System.out.println("Prompt envoyé  : " + prompt.text());
        System.out.println();

        // Envoyer au modèle
        String reponse = model.chat(prompt.text());
        System.out.println("Traduction : " + reponse);

        // Test avec un autre texte
        System.out.println("\n--- Deuxième traduction ---");
        String texte2 = "Le soleil brille aujourd'hui et les oiseaux chantent.";
        Prompt prompt2 = template.apply(Map.of("texte", texte2));
        String reponse2 = model.chat(prompt2.text());
        System.out.println("Texte original : " + texte2);
        System.out.println("Traduction : " + reponse2);

    }
}