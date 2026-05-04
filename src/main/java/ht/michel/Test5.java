package ht.michel;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

public class Test5 {
    public static void main(String[] args) {

        // Interface du service IA (implémentée automatiquement par LangChain4j)
        interface Assistant {
            String chat(String userMessage);
        }

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
        EmbeddingModel embeddingModel = GoogleAiEmbeddingModel.builder()
            .apiKey(geminiKey)
            .modelName("gemini-embedding-001")
            .outputDimensionality(300)
            .timeout(Duration.ofSeconds(60))
            .build();



        // --- Méthode 1 : chemin absolu (FileSystemDocumentLoader) ---
        // Path absolutePath = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "infos.txt");
        // Document documentAbsolu = FileSystemDocumentLoader.loadDocument(absolutePath);

        // --- Méthode 2 : depuis le classpath (ClassPathDocumentLoader) ---
        // Ancien code (non utilisé) :
        // Document document = FileSystemDocumentLoader.loadDocument("src/main/resources/infos.txt");
        // Le fichier doit être dans src/main/resources/ pour être dans le classpath
        Document document = ClassPathDocumentLoader.loadDocument("infos.txt");
        
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        // Ancien code (non utilisé) :
        // EmbeddingStoreIngestor.ingest(document, embeddingStore);
        EmbeddingStoreIngestor.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .build()
            .ingest(document);

        // Création de l'assistant avec RAG
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
            .contentRetriever(EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build())
                .build();

        // Test 1
        String question = "Comment s'appelle le chat de Pierre ?";
        System.out.println("Question : " + question);
        System.out.println("Réponse  : " + assistant.chat(question));

        System.out.println();

        // Pause pour éviter le rate-limiting de l'API Gemini
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        // Test 2
        String question2 = "Pierre appelle son chat. Qu'est-ce qu'il pourrait dire ?";
        System.out.println("Question : " + question2);
        System.out.println("Réponse  : " + assistant.chat(question2));


    }
}