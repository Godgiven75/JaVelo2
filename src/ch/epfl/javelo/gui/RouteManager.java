package ch.epfl.javelo.gui;

import ch.epfl.javelo.projection.PointCh;
import ch.epfl.javelo.projection.PointWebMercator;
import ch.epfl.javelo.routing.Route;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;

import java.util.*;
import java.util.List;


/**
 * Classe publique finale gérant l'affichage de l'itinéraire et (une partie de)
 * l'interaction avec lui.
 *
 * @author Tanguy Dieudonné (326618)
 * @author Nathanaël Girod (329987)
 */
public final class RouteManager {
    private final Pane pane;
    private final RouteBean routeBean;
    private final ReadOnlyObjectProperty<MapViewParameters> mapViewParametersP;
    private final Polyline routePolyline;
    private final Circle highlightedPositionC;
    private static final float CIRCLE_RADIUS = 5f;

    /**
     * Construit une nouvelle instance de RouteManger.
     *
     * @param routeBean le bean JavaFX de l'intinéraire
     * @param mapViewParametersReadOnlyObjectProperty une propriété accessible
     * en lecture seule contenant les paramètres d'affichage de la carte
     */
    public RouteManager(RouteBean routeBean,
                        ReadOnlyObjectProperty<MapViewParameters> mapViewParametersReadOnlyObjectProperty) {
        this.routeBean = routeBean;
        this.mapViewParametersP = mapViewParametersReadOnlyObjectProperty;
        this.pane = new Pane();
        pane.setPickOnBounds(false);
        routePolyline = new Polyline();
        highlightedPositionC = new Circle();
        highlightedPositionC.setVisible(false);
        addGUIElements();
        addMouseEventsManager();
        addListeners();
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

    // Méthode permettant d'ajouter la polyligne de l'initéraire ainsi que
    // le cercle correspondant à la position mise en évidence (méthode au nom
    // trop équivoque ? )
    private void addGUIElements() {
        routePolyline.setId("route");
        highlightedPositionC.setId("highlight");
        highlightedPositionC.setRadius(CIRCLE_RADIUS);
        pane.getChildren().add(routePolyline);
        pane.getChildren().add(highlightedPositionC);
    }

    private void addMouseEventsManager() {
        highlightedPositionC.setOnMousePressed(e -> {
            Point2D localToParent = highlightedPositionC.localToParent(e.getX(), e.getY());
            PointCh p = mapViewParametersP.get()
                    .pointAt(localToParent.getX(), localToParent.getY())
                    .toPointCh();
            double position = routeBean.highlightedPosition();
            int closestNode = routeBean.route().nodeClosestTo(position);
            Waypoint w = new Waypoint(p, closestNode);
            int index = routeBean.indexOfNonEmptySegmentAt(position);
            routeBean.waypoints().add(index + 1 , w);
        });
    }

    private void addListeners() {
        routeBean.routeProperty().addListener(p -> {
            if (!hasItinerary()) {
                routePolyline.setVisible(false);
                highlightedPositionC.setVisible(false);
                return;
            }
            highlightedPositionC.setVisible(true);
            Route route = routeBean.route();
            // L'emballage des doubles est-il un problème ?
            List<Double> points = new ArrayList<>();
            route.points().forEach(pointCh -> {
                MapViewParameters mvp = mapViewParametersP.get();
                PointWebMercator pwm = PointWebMercator.ofPointCh(pointCh);
                points.add(mvp.viewX(pwm));
                points.add(mvp.viewY(pwm));
            });
            routePolyline.setLayoutX(0);
            routePolyline.setLayoutY(0);
            routePolyline.getPoints().setAll(points);
            routePolyline.setVisible(true);
            // Pas entièrement sûr de la modularisation highlightPosition
            highlightPosition(mapViewParametersP.get());
        });

        mapViewParametersP.addListener((p, o, n) -> {
            int oldZoomLevel = o.zoomLevel();
            int newZoomLevel = n.zoomLevel();

            if (newZoomLevel == oldZoomLevel) {
                Point2D oldTopLeft = o.topLeft();
                Point2D newTopLeft = n.topLeft();
                Point2D offset = newTopLeft.subtract(oldTopLeft);
                routePolyline.setLayoutX(routePolyline.getLayoutX() - offset.getX());
                routePolyline.setLayoutY(routePolyline.getLayoutY() - offset.getY());
                highlightedPositionC.setCenterX(highlightedPositionC.getCenterX() - offset.getX());
                highlightedPositionC.setCenterY(highlightedPositionC.getCenterY() - offset.getY());
                highlightedPositionC.setVisible(true);
            } else {
                if (hasItinerary()) {
                    highlightedPositionC.setVisible(false);
                    List<Double> pointsAtNewZoomLevel = new ArrayList<>();
                    routeBean.route().points().forEach(pointCh -> {
                        PointWebMercator pwm = PointWebMercator.ofPointCh(pointCh);
                        pointsAtNewZoomLevel.add(n.viewX(pwm));
                        pointsAtNewZoomLevel.add(n.viewY(pwm));
                    });
                    highlightPosition(n);
                    routePolyline.setLayoutX(0);
                    routePolyline.setLayoutY(0);
                    routePolyline.getPoints().setAll(pointsAtNewZoomLevel);
                }
            }
        });

        routeBean.highlightedPositionProperty().addListener((p, o, n) -> {
            highlightPosition(mapViewParametersP.get());
        });
    }

    private void highlightPosition(MapViewParameters mvp) {
        double highlightedPosition = routeBean.highlightedPositionProperty().get();
        if (Double.isNaN(highlightedPosition))
            return;
        PointWebMercator highlightedPoint =
                PointWebMercator.ofPointCh(routeBean.route().pointAt(highlightedPosition));
        highlightedPositionC.setCenterX(mvp.viewX(highlightedPoint));
        highlightedPositionC.setCenterY(mvp.viewY(highlightedPoint));
    }

    private boolean hasItinerary() {
        return routeBean.route() != null;
    }
}