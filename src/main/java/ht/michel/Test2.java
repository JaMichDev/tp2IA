package ht.michel;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.TokenUsage;

public class Test2 {
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

        String question = "Quelle est la capitale de la France ?";
        UserMessage userMessage = UserMessage.from(question);

        // chat() retourne un ChatResponse qui contient la réponse et les infos de tokens
        ChatResponse response = model.chat(userMessage);

        AiMessage aiMessage = response.aiMessage();
        System.out.println("Réponse : " + aiMessage.text());

        // Affichage des tokens
        TokenUsage tokenUsage = response.tokenUsage();
        int tokensEntree = tokenUsage.inputTokenCount();
        int tokensSortie = tokenUsage.outputTokenCount();
        int tokensTotal = tokenUsage.totalTokenCount();

        System.out.println("\n--- Coût en tokens ---");
        System.out.println("Tokens entrée  : " + tokensEntree);
        System.out.println("Tokens sortie  : " + tokensSortie);
        System.out.println("Tokens total   : " + tokensTotal);

        // Coût approximatif pour gemini-2.0-flash (en dollars)
        // Entrée : $0.075 par million de tokens
        // Sortie : $0.30 par million de tokens
        double coutEntree = (tokensEntree / 1_000_000.0) * 0.075;
        double coutSortie = (tokensSortie / 1_000_000.0) * 0.30;
        double coutTotal = coutEntree + coutSortie;

        System.out.printf("\nCoût total de la requête : $%.8f%n", coutTotal);

        if (coutTotal > 0) {
            long nbRequetesPourUnDollar = (long) (1.0 / coutTotal);
            System.out.printf("Nombre de requêtes similaires pour dépenser $1 : %,d%n", nbRequetesPourUnDollar);
        }
    }
}