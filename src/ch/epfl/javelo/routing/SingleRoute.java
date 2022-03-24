package ch.epfl.javelo.routing;

import ch.epfl.javelo.Preconditions;
import ch.epfl.javelo.projection.PointCh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ch.epfl.javelo.routing.RoutePoint.NONE;

/**
 * Représente un itinéraire simple, càd reliant un point de départ à un point d'arrivée, sans point de passage
 * intermédiaire
 */
public final class SingleRoute implements Route {
    private final List<Edge> edges;
    private final double[] nodePositions;

    /**
     * Retourne l'itinéraire simple composé des arêtes données, ou lève IllegalArgumentException si la liste d'arêtes
     * est vide
     * @param edges
     */
    public SingleRoute(List<Edge> edges) {
        Preconditions.checkArgument(!edges.isEmpty());
        this.edges = List.copyOf(edges);
        this.nodePositions = nodePositions();

    }
    private double[] nodePositions() {
        int nbEdges = edges.size();
        double[] nodePositions = new double[nbEdges + 1];
        int nodeId = 0;
        for(Edge e : edges) {
            //if (nodeId == nbEdges - 1) break;
            nodePositions[++nodeId] = e.length() + nodePositions[nodeId - 1];
        }
        return nodePositions;
    }

    /**
     * Retourne l'index du segment de l'itinéraire contenant la position donnée, qui vaut toujours 0 dans le cas d'un
     * itinéraire simple
     * @param position
     * @return l'index du segment de l'itinéraire contenant la position donnée, qui vaut toujours 0 dans le cas d'un
     * itinéraire simple
     */
    @Override
    public int indexOfSegmentAt(double position) {
        return 0;
    }

    /**
     * Retourne la longueur de l'itinéraire, en mètres
     * @return la longueur de l'itinéraire, en mètres
     */
    @Override
    public double length() {
        double totalLength = 0.0;
        for (Edge e : edges) {
            totalLength += e.length();
        }
        return totalLength;
    }

    /**
     * Retourne la totalité des arêtes de l'itinéraire
     * @return la totalité des arêtes de l'itinéraire
     */
    @Override
    public List<Edge> edges() {
        return edges;
    }

    /**
     * Retourne la totalité des points situés aux extrémités des arêtes de l'itinéraire
     * @return la totalité des points situés aux extrémintés des arêtes de l'itinéraire
     */
    @Override
    public List<PointCh> points() {
        List<PointCh> l = new ArrayList<>();
        l.add(edges.get(0).fromPoint()); // Ajout du 1er point de la 1ère arête, puis du point d'arrivée de chaque arête
        for (Edge e : edges) {
            l.add(e.toPoint());
        }
        return l;
    }

    private Edge findEdge(double position) {
        int binarySearchResult = Arrays.binarySearch(nodePositions, position);
        return binarySearchResult >= 0 ?  edges.get(binarySearchResult) : edges.get(-binarySearchResult - 2); // nombre magique!
    }
    /**
     * Retourne le point se trouvant à la position donnée le long de l'itinéraire
     * @param position
     * @return le point se trouvant à la position donnée le long de l'itinéraire
     */
    @Override
    public PointCh pointAt(double position) {
        return findEdge(position).pointAt(position);
    }

    /**
     * Retourne l'altitude à la position donnée le long de l'itinéraire, qui peut valoir NaN si l'arête contenant
     * cette position n'a pas de profil
     * @param position
     * @return l'altitude à la position donnée le long de l'itinéraire, qui peut valoir Nan si l'arête contenant
     * cette position n'a pas de profil
     */
    @Override
    public double elevationAt(double position) {
        int binarySearchResult = Arrays.binarySearch(nodePositions, position);
        int binarySearchIndex = binarySearchResult >= 0 ?  binarySearchResult : -binarySearchResult - 2;

        double anteriorLength = 0;
        for(int i = 0; i < binarySearchIndex; ++i) {
            anteriorLength += edges.get(i).length();
        }
        Edge e = edges.get(binarySearchIndex);
        double actualPosition = position - anteriorLength;
        return e.elevationAt(actualPosition);
    }

    /**
     * Retourne l'identité du noeud appartenant à l'itinéraire et se trouvant le plus proche de la position donnée
     * @param position
     * @return l'identité du noeud appartenant à l'itinéraire et se trouvant le plus proche de la position donnée
     */
    @Override
    public int nodeClosestTo(double position) {
        int binarySearchResult = Arrays.binarySearch(nodePositions, position);

        if (binarySearchResult >= 0 )
            return binarySearchResult;
        int binarySearchIndex = -binarySearchResult - 2;
        Edge e  = edges.get(binarySearchIndex);
        int fromNodeId = e.fromNodeId();
        int toNodeId = e.toNodeId();
        double mean = (nodePositions[fromNodeId] + nodePositions[toNodeId]) / 2.0;
        return position <= mean ? fromNodeId :  toNodeId;
    }

    /**
     * Retourne le point de l'itinéraire se trouvant le plus proche du point de référence donné
     * @param point
     * @return le point de l'itinéraire se trouvant le plus proche du point de référence donné
     */
    @Override
    public RoutePoint pointClosestTo(PointCh point) {
        RoutePoint closestPoint = NONE;
        for (Edge e : edges) {
            closestPoint = closestPoint.min(e.pointAt(e.positionClosestTo(point)), e.positionClosestTo(point), point.distanceTo(e.fromPoint()));
        }
        closestPoint = closestPoint.min(edges.get(edges.size()-1).pointAt(edges.get(edges.size()-1).positionClosestTo(point)), edges.get(edges.size()-1).positionClosestTo(point), point.distanceTo(edges.get(edges.size()-1).fromPoint()));
        return closestPoint;
    }


}
