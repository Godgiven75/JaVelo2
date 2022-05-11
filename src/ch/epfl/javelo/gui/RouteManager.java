package ch.epfl.javelo.gui;

import ch.epfl.javelo.projection.PointCh;
import ch.epfl.javelo.projection.PointWebMercator;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Classe publique finale gérant l'affichage de l'itinéraire et (une partie de)
 * l'interaction avec lui.
 *
 * @author Tanguy Dieudonné (326618)
 * @author Nathanaël Girod (329987)
 */
public final class RouteManager {
    private final Pane pane;
    private RouteBean routeBean;
    private ReadOnlyObjectProperty<MapViewParameters> mapViewParametersP;
    private Consumer<String> errorConsumer;
    private final static int HIGHLIGHTED_POINT_POSITION = 1;

    public RouteManager(RouteBean routeBean,
                        ReadOnlyObjectProperty<MapViewParameters> mvp,
                        Consumer<String> errorConsumer) {
        this.routeBean = routeBean;
        this.mapViewParametersP = mvp;
        this.errorConsumer = errorConsumer;
        this.pane = new Pane();
        pane.setPickOnBounds(false);
        addMouseEventsManager();
        addListeners();
    }

    private void addMouseEventsManager() {

        if (pane.getChildren().size() > 1) {
            Node highlightedPosition = pane.getChildren().get(HIGHLIGHTED_POINT_POSITION);
        }


    }
    private void addListeners() {
        routeBean.routeProperty().addListener((p) -> drawPane());
    }


    /**
     * Retourne le panneau JavaFX contenant la ligne représentant l'itinéraire
     * et le disque de mise en évidence.
     *
     * @return le panneau JavaFX contenant la ligne représentant l'itinéraire
     * et le disque de mise en évidence
     */
    public Pane pane() {
        return pane;
    }
    private void drawPane() {
        if (routeBean.route() != null) {
            MapViewParameters mvp = mapViewParametersP.get();
            List<PointCh> routeBeanItineraryPoints = routeBean.route().points();
            List<Node> paneChildren = pane.getChildren();

            double[] polylinePoints = new double[routeBeanItineraryPoints.size()];
            for (int i = 0; i < routeBeanItineraryPoints.size(); i += 2) {
                PointCh pch = routeBean.route().points().get(i);
                PointWebMercator pwm = PointWebMercator.ofPointCh(pch);
                double x = mvp.viewX(pwm);
                double y = mvp.viewY(pwm);
                polylinePoints[i] = x;
                polylinePoints[i + 1] = y;
            }
            System.out.println(polylinePoints.length);
            int c = 0;
            for (double polylinePoint : polylinePoints) {
                if (polylinePoint != 0) c++;
            }
            System.out.println(c);
            //System.out.println(" bonjour" + polylinePoints[polylinePoints.length - 1]);
            //System.out.println(polylinePoints[polylinePoints.length - 2]);
            Polyline itineraryGUI = new Polyline(polylinePoints);
            itineraryGUI.setId("route");
            pane.getChildren().clear();
            //itineraryGUI.setLayoutX();
            //itineraryGUI.setLayoutY();
            paneChildren.add(itineraryGUI);


            double highlightedPosition = routeBean.highlightedPosition();
            PointWebMercator highlightedPoint =
                    PointWebMercator.ofPointCh(routeBean.route().pointAt(highlightedPosition));
            Circle highlightedPositionGUI = new Circle();
            highlightedPositionGUI.setCenterX(mvp.viewX(highlightedPoint));
            highlightedPositionGUI.setCenterY(mvp.viewY(highlightedPoint));
            highlightedPositionGUI.setRadius(5f);
            highlightedPositionGUI.setId("highlighted");
            paneChildren.add(highlightedPositionGUI);
        }

    }

}