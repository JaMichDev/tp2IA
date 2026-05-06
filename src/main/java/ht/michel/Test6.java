package ht.michel;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;
import java.util.Scanner;


import java.time.Duration;

public class Test6 {

    // Interface du service IA (implémentée automatiquement par LangChain4j)
    interface Assistant {
        String chat(String userMessage);
    }


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
                .modelName("gemini-flash-latest") // Température basse pour le RAG (on veut des réponses précises)
                .temperature(0.3)
                .build();

        // Modèle d'embeddings requis pour indexer le document dans l'EmbeddingStore
        //.outputDimensionality(300)  par .outputDimensionality(768)
        // Le modèle gemini-embedding-001 supporte des dimensions spécifiques.
        // La valeur 300 risque de causer une erreur. j'utilise 768 (valeur par défaut recommandée) :
        EmbeddingModel embeddingModel = GoogleAiEmbeddingModel.builder()
            .apiKey(geminiKey)
            .modelName("gemini-embedding-001")
            .outputDimensionality(768)
            .timeout(Duration.ofSeconds(60))
            .build();


        // Chargement du PDF (mettre le nom exact de ton fichier PDF)
        System.out.println("Chargement du PDF en cours...");
        Document document = ClassPathDocumentLoader.loadDocument("ml.pdf");
        
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // Ancien code (ingestion batch, plus sensible aux timeouts reseau) :
        // EmbeddingStoreIngestor.builder()
        //     .embeddingStore(embeddingStore)
        //     .embeddingModel(embeddingModel)
        //     .build()
        //     .ingest(document);


        // 1361 segments
        //DocumentSplitter splitter = DocumentSplitters.recursive(1000, 100);

        // Réduction du nombre de segments : 3000 caractères par segment
        DocumentSplitter splitter = DocumentSplitters.recursive(3000, 300);

        List<TextSegment> segments = splitter.split(document);

        System.out.println("Indexation en cours (" + segments.size() + " segments)...");

        // Boucle d'indexation
        //int batchSize = 80;
        int batchSize = 95; //Augmentation du batch size
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            Response<Embedding> embedding = embedWithRetry(embeddingModel, segment, 5);
            embeddingStore.add(embedding.content(), segment);

            if ((i + 1) % 100 == 0 || i + 1 == segments.size()) {
                System.out.println("Segments indexés: " + (i + 1) + "/" + segments.size());
            }

            // Pause entre chaque requête
            try {
                Thread.sleep(500);
                //Thread.sleep(700);
            } catch (InterruptedException ignored) {}

            // Pause longue toutes les 90 requêtes
            if ((i + 1) % batchSize == 0 && i + 1 < segments.size()) {
                System.out.println("Pause de 15 secondes pour respecter le rate limit...");
                try { Thread.sleep(10_000); } catch (InterruptedException ignored) {}
            }
        }

        //
         //         for (int i = 0; i < segments.size(); i++) {
         //             TextSegment segment = segments.get(i);
         //             Response<Embedding> embedding = embedWithRetry(embeddingModel, segment, 3);
         //             embeddingStore.add(embedding.content(), segment);
         //
         //             if ((i + 1) % 20 == 0 || i + 1 == segments.size()) {
         //                 System.out.println("Segments indexes: " + (i + 1) + "/" + segments.size());
         //             }
         //
         //             try {
         //                 Thread.sleep(120);
         //             } catch (InterruptedException ignored) {
         //                 Thread.currentThread().interrupt();
         //             }
         //         }
        //

        System.out.println("PDF chargé !\n");

        // Création de l'assistant avec RAG
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
            // Ancien code (non utilise) :
            // .contentRetriever(EmbeddingStoreContentRetriever.from(embeddingStore))
            .contentRetriever(EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build())
                .build();

        // Conversation interactive
        conversationAvec(assistant);
    }




    private static Response<Embedding> embedWithRetry(EmbeddingModel embeddingModel, TextSegment segment, int maxAttempts) {
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return embeddingModel.embed(segment);
            } catch (RuntimeException e) {
                lastException = e;
                if (attempt == maxAttempts) break;

                // Extraire le délai suggéré par l'API (ex: "retryDelay": "58s")
                long waitMs = extractRetryDelay(e.getMessage());
                System.out.println("Rate limit, tentative " + attempt + "/" + maxAttempts
                        + ", attente de " + waitMs/1000 + "s...");
                try { Thread.sleep(waitMs); } catch (InterruptedException ignored) {}
            }
        }
        throw lastException;
    }

    private static long extractRetryDelay(String errorMessage) {
        if (errorMessage == null) return 5000L;
        // Cherche un pattern comme "retryDelay": "58s"
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"retryDelay\":\\s*\"(\\d+)s\"")
                .matcher(errorMessage);
        if (m.find()) {
            return (Long.parseLong(m.group(1)) + 2) * 1000L; // +2s de marge
        }
        return 10_000L; // défaut 10s
    }


    //
      //     private static Response<Embedding> embedWithRetry(EmbeddingModel embeddingModel, TextSegment segment, int maxAttempts) {
     //         RuntimeException lastException = null;
     //
      //         for (int attempt = 1; attempt <= maxAttempts; attempt++) {
     //             try {
     //                 return embeddingModel.embed(segment);
     //             } catch (RuntimeException e) {
     //                 lastException = e;
     //                 if (attempt == maxAttempts) {
     //                     break;
     //                 }
     //                 int backoffMs = attempt * 1000;
     //                 System.out.println("Timeout/reseau sur embedding, tentative " + attempt + "/" + maxAttempts + ", nouvelle tentative dans " + backoffMs + " ms...");
     //                 try {
     //                     Thread.sleep(backoffMs);
     //                 } catch (InterruptedException ignored) {
     //                     Thread.currentThread().interrupt();
     //                     break;
     //                 }
     //             }
     //         }
     //
      //         throw lastException;
     //     }
     //


    private static void conversationAvec(Assistant assistant){
            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.println("==================================================");
                    System.out.println("Posez votre question (ou 'fin' pour quitter) : ");
                    String question = scanner.nextLine();
                    if (question.isBlank()) continue;
                    if ("fin".equalsIgnoreCase(question)) break;
                    System.out.println("==================================================");
                    String reponse = assistant.chat(question);
                    System.out.println("Assistant : " + reponse);
                    System.out.println("==================================================");
                }
            }
    }

}