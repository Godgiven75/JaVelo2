package ch.epfl.javelo.data;

import ch.epfl.javelo.Functions;
import ch.epfl.javelo.projection.PointCh;

import java.io.IOException;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

/**
 * Représente le graphe JaVelo.
 *
 * @author Tanguy Dieudonné (326618)
 * @author Nathanaël Girod (329987)
 */
public final  class Graph {
    private final GraphNodes nodes;
    private final GraphSectors sectors;
    private final GraphEdges edges;
    private final List<AttributeSet> attributeSets;

    /**
     * Construit le graphe avec les noeuds, secteurs, arêtes et ensembles
     * d'attributs donnés.
     *
     * @param nodes les noeuds du graphe
     * @param sectors les secteurs du graphe
     * @param edges les arêtes du graphe
     * @param attributeSets un ensembles d'attributs donnés
     */
    public Graph (GraphNodes nodes, GraphSectors sectors, GraphEdges edges,
                  List<AttributeSet> attributeSets) {
        this.nodes = nodes;
        this.sectors = sectors;
        this.edges = edges;
        this.attributeSets = List.copyOf(attributeSets);
    }

    /**
     * Retourne le graphe JaVelo obtenu à partir des fichiers se trouvant dans
     * le répertoire donné.
     *
     * @param basePath le chemin du répertoire où se trouvent les fichiers
     *
     * @return retourne le graphe JaVelo obtenu à partir des fichiers se trouvant
     * dans le répertoire donné
     *
     * @throws IOException en cas d'erreur d'entrée/sortie, p. ex. si l'un des
     * fichiers attendu n'existe pas
     */
    public static Graph loadFrom(Path basePath) throws IOException {

        Path nodesPath = basePath.resolve("nodes.bin");
        IntBuffer nodesBuffer = mappedBuffer(nodesPath).asIntBuffer();

        GraphNodes nodes = new GraphNodes(nodesBuffer);


        Path sectorsPath = basePath.resolve("sectors.bin");
        ByteBuffer sectorsBuffer = mappedBuffer(sectorsPath);

        GraphSectors sectors = new GraphSectors(sectorsBuffer);


        Path edgesPath = basePath.resolve("edges.bin");
        ByteBuffer edgesBuffer =  mappedBuffer(edgesPath);

        Path profileIdsPath = basePath.resolve("profile_ids.bin");
        IntBuffer profileIds  = mappedBuffer(profileIdsPath).asIntBuffer();

        Path elevationsPath = basePath.resolve("elevations.bin");
        ShortBuffer elevations =  mappedBuffer(elevationsPath).asShortBuffer();

        GraphEdges edges = new GraphEdges(edgesBuffer, profileIds ,elevations);


        Path attributesPath = basePath.resolve("attributes.bin");
        LongBuffer attributes = mappedBuffer(attributesPath).asLongBuffer();

        List<AttributeSet> attributeSets = new ArrayList<>();

        long[] attributeSetsArray =  new long[attributes.capacity()];
        attributes.get(attributeSetsArray);

        for (long l : attributeSetsArray) {
            attributeSets.add(new AttributeSet(l));
        }

        return new Graph(nodes, sectors, edges, attributeSets);
    }

    private static MappedByteBuffer mappedBuffer(Path filePath) throws IOException {
        try (FileChannel channel = FileChannel.open(filePath)) {
            return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        }
    }

    /**
     * Retourne le nombre total de noeuds dans le graphe.
     *
     * @return le nombre total de noeuds dans le graphe
     */
    public int nodeCount() {
        return nodes.count();
    }

    /**
     * Retourne la position du noeud d'identité donnée.
     *
     * @param nodeId l'identité du noeud
     *
     * @return la position du noeud d'identité donnée
     */
    public PointCh nodePoint(int nodeId) {
        return new PointCh(nodes.nodeE(nodeId), nodes.nodeN(nodeId));
    }

    /**
     * Retourne le nombre d'arêtes sortant du noeud d'identité donnée.
     *
     * @param nodeId l'identité du noeud
     *
     * @return le nombre d'arêtes sortant du noeud d'identité donnée
     */
    public int nodeOutDegree(int nodeId) {
        return nodes.outDegree(nodeId);
    }

    /**
     * Retourne l'identité de la edgeIndex-ième arête sortant du noeud d'identité nodeId.
     *
     * @param nodeId l'identité du noeud
     * @param edgeIndex l'indice d'une des arêtes sortantes du noeud
     *
     * @return l'identité de la edgeIndex-ième arête sortant du noeud d'identité nodeId
     */
    public int nodeOutEdgeId(int nodeId, int edgeIndex) {
        return nodes.edgeId(nodeId, edgeIndex);
    }

    /**
     * Retourne l'identité du noeud se trouvant le plus proche du point donné,
     * à la distance maximale donnée (en mètres), ou -1 si aucun noeud ne
     * correspond à ces critères.
     *
     * @param point le point donné
     * @param searchDistance la distance maximale de recherche
     *
     * @return l'identité du noeud se trouvant le plus proche du point donné,
     * à la distance maximale donnée (en mètres), ou -1 si aucun noeud ne
     * correspond à ces critères
     */
    public int nodeClosestTo(PointCh point, double searchDistance) {
        List<GraphSectors.Sector> closeSectors = sectors.sectorsInArea(point, searchDistance);

        double minDistance = searchDistance * searchDistance, distance;
        // Si le noeud ne correspond à aucun des critères, -1 sera retourné
        int closestNodeId = -1;

        for (GraphSectors.Sector s : closeSectors) {
            for (int nodeId = s.startNodeId(); nodeId < s.endNodeId(); nodeId++) {
                distance = point.squaredDistanceTo(nodePoint(nodeId));

                if (distance < minDistance) {
                    minDistance = distance;
                    closestNodeId = nodeId;
                }
            }
        }

        return closestNodeId;
    }

    /**
     * Retourne l'identité du noeud destination de l'arête d'identité donnée.
     *
     * @param edgeId l'identité de l'arête
     *
     * @return l'identité du noeud destination de l'arête d'identité donnée
     */
    public int edgeTargetNodeId(int edgeId) {
        return edges.targetNodeId(edgeId);
    }

    /**
     * Retourne vrai ssi l'arête d'identité donnée va dans le sens contraire de
     * la voie OSM dont elle provient.
     *
     * @param edgeId l'identité de l'arête
     *
     * @return vrai ssi l'arête d'identité donnée va dans le sens contraire de
     * la voie OSM dont elle provient
     */
    public boolean edgeIsInverted(int edgeId) {
        return edges.isInverted(edgeId);
    }

    /**
     * Retourne l'ensemble des attributs OSM attachés à l'arête d'identité donnée.
     *
     * @param edgeId l'identité de l'arête
     *
     * @return l'ensemble des attributs OSM attachés à l'arête d'identité donnée
     */
    public AttributeSet edgeAttributes(int edgeId) {
        int attributeSetId = edges.attributesIndex(edgeId);
        return attributeSets.get(attributeSetId);
    }

    /**
     * Retourne la longueur, en mètres, de l'arête d'identité donnée.
     *
     * @param edgeId l'identité de l'arête
     *
     * @return la longueur, en mètres, de l'arête d'identité donnée
     */
    public double edgeLength(int edgeId) {
        return edges.length(edgeId);
    }

    /**
     * Retourne le dénivelé positif total de l'arête d'identité donnée.
     *
     * @param edgeId l'identité de l'arête
     *
     * @return le dénivelé positif total de l'arête d'identité donnée
     */
    public double edgeElevationGain(int edgeId) {
        return edges.elevationGain(edgeId);
    }

    /**
     * Retourne le profil en long de l'arête d'identité donnée, sous la forme
     * d'une fonction; si l'arête ne possède pas de profil, alors cette fonction
     * doit retourner Double.NaN pour n'importe quel argument.
     *
     * @param edgeId l'identité de l'arête
     *
     * @return le profil en long de l'arête d'identité donnée, sous la forme
     * d'une fonction; si l'arête ne possède pas de profil, alors cette fonction
     * doit retourner Double.NaN pour n'importe quel argument
     */
    public DoubleUnaryOperator edgeProfile(int edgeId) {
        float[] samples = edges.profileSamples(edgeId);
        return edges.hasProfile(edgeId) ?
                Functions.sampled(samples, edgeLength(edgeId))
                : Functions.constant(Double.NaN);
    }
}
