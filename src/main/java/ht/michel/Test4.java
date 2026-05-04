package ht.michel;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.exception.ModelNotFoundException;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.CosineSimilarity;

import java.time.Duration;

public class Test4 {
    public static void main(String[] args) {
        String geminiKey = System.getenv("GEMINI_KEY");
        if (geminiKey == null) {
            System.out.println("La variable d'environnement GEMINI_KEY n'est pas définie.");
            return;
        }

        // Créer le modèle d'embeddings Gemini
        EmbeddingModel embeddingModel = GoogleAiEmbeddingModel.builder()
                .apiKey(geminiKey)
            .modelName("gemini-embedding-001")
                .outputDimensionality(300)
                .timeout(Duration.ofSeconds(30))
                .build();

        // Couples de phrases à comparer
        String[][] couples = {
                {"Le chat dort sur le canapé.", "Le félin sommeille sur le sofa."},
                {"J'aime manger des pommes.", "La voiture roule vite sur l'autoroute."},
                {"Il fait chaud aujourd'hui.", "La température est élevée ce jour."},
                {"Java est un langage de programmation.", "Python est utilisé pour l'intelligence artificielle."}
        };

        System.out.println("=== Calcul de similarité cosinus entre phrases ===\n");

        try {
            for (String[] couple : couples) {
                String phrase1 = couple[0];
                String phrase2 = couple[1];

                // Générer les embeddings
                Response<Embedding> embedding1 = embeddingModel.embed(TextSegment.from(phrase1));
                Response<Embedding> embedding2 = embeddingModel.embed(TextSegment.from(phrase2));

                // Calculer la similarité
                double similarite = CosineSimilarity.between(
                        embedding1.content(),
                        embedding2.content()
                );

                System.out.println("Phrase 1 : " + phrase1);
                System.out.println("Phrase 2 : " + phrase2);
                System.out.printf("Similarité cosinus : %.4f%n%n", similarite);
            }
        } catch (ModelNotFoundException e) {
            System.out.println("Modele d'embedding introuvable. Essayez avec modelName(\"gemini-embedding-001\").");
            throw e;
        }

    }
}