package ht.michel;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;

import dev.langchain4j.model.chat.response.ChatResponse;

import dev.langchain4j.model.output.TokenUsage;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import ht.michel.outil.meteo.MeteoTool;

import java.util.Scanner;

public class Test7 {
    interface AssistantMeteo {
        String chat(String userMessage);
    }

    public static void main(String[] args) {
        String geminiKey = System.getenv("GEMINI_KEY");
        if (geminiKey == null) {
            System.out.println("La variable d'environnement GEMINI_KEY n'est pas définie.");
            return;
        }


        // Mettre une température qui ne dépasse pas 0,3.
        // Le RAG sert à mieux contrôler l'exactitude des informations données par le LLM
        // et il est donc logique de diminuer la température.
        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(geminiKey)
                .modelName("gemini-flash-latest")
                .temperature(0.3)
                .build();

        // Création de l'assistant conversationnel, avec une mémoire.
        // L'implémentation de Assistant est faite par LangChain4j.
        // L'assistant gardera en mémoire les 10 derniers messages.
        // La base vectorielle en mémoire est utilisée pour retrouver les embeddings.
        AssistantMeteo assistant =
                AiServices.builder(AssistantMeteo.class)
                        .chatModel(model)
                        .chatMemory(MessageWindowChatMemory.withMaxMessages(20)) // L'utilisation des outils génère des messages
                        .tools(new MeteoTool())  // Ajout de l'outil
                        .build();



        System.out.println("Assistant météo prêt ! Posez vos questions.");
        //conversationAvec(assistant);
        System.out.println("==============Partie 1 — Test de base avec coordonnées GPS========================");
        System.out.println("Question 1");
        String question = "J'ai prévu d'aller aujourd'hui à la ville dont la latitude est 48.85 et la longitude est 2.35 pour un séjour de 3 jours. Est-ce que tu me conseilles de mettre un parapluie dans ma valise ?";

        String reponse = assistant.chat(question);
        // Affiche la réponse du LLM.
        System.out.println(reponse);
        //===============================================================

        System.out.println("Question 2");
        String question2 = "Finalement, je ne vais partir que demain. Est-ce que tu me conseilles de prendre un parapluie ?";

        String reponse2 = assistant.chat(question2);
        // Affiche la réponse du LLM.
        System.out.println(reponse2);

        //===============================================================


        System.out.println("===============Partie 2 — Question hors météo===================================");
        System.out.println("Question 3");
        String question3 = "Quelle est la capitale de l'Espagne ?";

        String reponse3 = assistant.chat(question3);
        // Affiche la réponse du LLM.
        System.out.println(reponse3);

        //===============================================================
        System.out.println("Question 4");
        String question4 = "J'ai prévu d'aller aujourd'hui à Paris pour un séjour de 3 jours. Est-ce que tu me conseilles de mettre un parapluie dans ma valise ?";

        String reponse4 = assistant.chat(question4);
        // Affiche la réponse du LLM.
        System.out.println(reponse4);

        //===============================================================
        System.out.println("Question 5");
        String question5 = "J'ai prévu d'aller aujourd'hui à Zzyzx pour un séjour de 3 jours. Est-ce que tu me conseilles de mettre un parapluie dans ma valise ?";

        String reponse5 = assistant.chat(question5);
        // Affiche la réponse du LLM.
        System.out.println(reponse5);

        System.out.println("===============Partie 3 — Avec logging activé===================================");
        //===============================================================
        System.out.println("Question 6");
        String question6 = "J'ai prévu d'aller aujourd'hui à la ville de Zzyzx pour un séjour de 3 jours. Est-ce que tu me conseilles de mettre un parapluie dans ma valise ?";

        String reponse6 = assistant.chat(question6);
        // Affiche la réponse du LLM.
        System.out.println(reponse6);


        //===============================================================


    }

    private static void conversationAvec(AssistantMeteo assistant) {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("==================================================");
                System.out.println("Votre question (ou 'fin' pour quitter) : ");
                String question = scanner.nextLine();
                if (question.isBlank()) continue;
                if ("fin".equalsIgnoreCase(question)) break;
                System.out.println("==================================================");
                String reponse = assistant.chat(question);
                System.out.println("Assistant : " + reponse);
            }
        }
    }
}