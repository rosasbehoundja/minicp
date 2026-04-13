package minicp.examples;

import minicp.engine.constraints.Circuit;
import minicp.engine.constraints.Element1D;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.util.io.InputReader;
import minicp.search.SearchStatistics;
import minicp.util.exception.NotImplementedException;

import static minicp.cp.BranchingScheme.*;
import static minicp.cp.Factory.*;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.IntStream;


/*
* Problème du voyageur de commerce
*/
public class TSPv2 extends OptimizationProblem {

    public final int n; // Nombre total de villes
    public final int[][] distanceMatrix; // Matrice des distances entre les villes
    public IntVar[] successors; // Tableau des successeurs
    public IntVar totalDist; // Variable représentant la distance totale du circuit
    String instance; // Nom de l'instance du problème

    public TSPv2(String instancePath) {
        InputReader reader = new InputReader(instancePath); // Lire le contenu du fichier
        instance = reader.getFilename(); // Récupérer le nom de l'instance
        n = reader.getInt(); // Récupérer le nombre de villes
        distanceMatrix = reader.getMatrix(n, n); // Récupérer la matrice des distances entre les villes
    }

    @Override
    public void buildModel() {
        Solver cp = makeSolver(false); // Initialiser le solveur
        successors = makeIntVarArray(cp, n, n); // Créer un tableau de variables pour les successeurs
        IntVar[] distSuccessors = makeIntVarArray(cp, n, 1000); // Créer un tableau de distances entre les successeurs
        cp.post(new Circuit(successors)); // Ajouter la contrainte de circuit

        for (int i = 0; i < n; i++) {
            cp.post(new Element1D(distanceMatrix[i], successors[i], distSuccessors[i])); // Ajouter la contrainte d'élément pour chaque ville
        }
        totalDist = sum(distSuccessors);
        objective = cp.minimize(totalDist); // Définir l'objectif de minimisation de la distance totale

        // Définir une stratégie de recherche simple basée sur le premier échec pour le DFS
        dfs = makeDfs(cp, () -> {
            IntVar xs = selectMin(successors,
                    xi -> xi.size() > 1, // Sélectionner les variables qui ne sont pas encore fixées
                    xi -> xi.size()); // Sélectionner la variable avec le plus petit domaine
            if (xs == null)
                return EMPTY; // Si toutes les variables sont fixées, retourner une procédure vide
            else {
                int v = xs.min(); // Récupérer la valeur minimale du domaine
                return branch(() -> xs.getSolver().post(equal(xs, v)), // Branche pour fixer la variable à la valeur minimale)
                        () -> xs.getSolver().post(notEqual(xs, v))); // Branche pour exclure la valeur minimale
            }
        });

        // TODO implement the search and remove the NotImplementedException
        dfs.onSolution(() -> System.out.println("Solution trouvée avec une distance totale de: " + totalDist)); // Afficher la distance totale à chaque solution trouvée

    }

    public String toString() {
        return "TSP(" + instance + ')';
    }

    public static void main(String[] args) {
        // instance gr17 https://people.sc.fsu.edu/~jburkardt/datasets/tsp/gr17_d.txt
        // instance fri26 https://people.sc.fsu.edu/~jburkardt/datasets/tsp/fri26_d.txt
        // instance p01 (adapted from) https://people.sc.fsu.edu/~jburkardt/datasets/tsp/p01_d.txt
        // the other instances are located at data/tsp/ and adapted from https://lopez-ibanez.eu/tsptw-instances
        String instance = "data/tsp/tsp_61.txt";
        TSPv2 tsp = new TSPv2(instance);
        tsp.buildModel();
        // stops at the first solutions using the exact search
        SearchStatistics stats = tsp.solve(true);
        System.out.println(stats);
    }
}
