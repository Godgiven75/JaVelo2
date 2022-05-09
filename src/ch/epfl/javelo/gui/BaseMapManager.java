package ch.epfl.javelo.gui;

import ch.epfl.javelo.projection.PointWebMercator;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;

import java.io.IOException;

/**
 * Gère l'affichage et l'interaction avec le fond de carte
 * @author Tanguy Dieudonné (326618)
 * @author Nathanaël Girod (329987)
 */
public final class BaseMapManager {
    private final TileManager tileManager;
    private final WaypointsManager waypointsManager;
    private final ObjectProperty<MapViewParameters> mapViewParametersP;
    private ObjectProperty<Point2D> mousePositionP;
    private final Pane pane;
    private final Canvas canvas;
    private boolean redrawNeeded;
    private int currentZoomLevel;
    private static final int PIXELS_IN_TILE = 256;


    public BaseMapManager(TileManager tileManager, WaypointsManager waypointsManager,
                          ObjectProperty<MapViewParameters> mapViewParametersP) {
        this.tileManager = tileManager;
        this.waypointsManager = waypointsManager;
        this.mapViewParametersP = mapViewParametersP;
        this.currentZoomLevel = mapViewParametersP.get().zoomLevel();
        this.canvas = new Canvas();
        this.pane = new Pane(canvas);
        this.mousePositionP = new SimpleObjectProperty<>(new Point2D(0, 0));
        pane.setPickOnBounds(false);

        addBindings();
        addListeners();
        addMouseEventsManager();
    }

    /**
     * Retourne le panneau JavaFX affichant le fond de carte
     * @return le panneau JavaFX affichant le fond de carte
     */
    public Pane pane() {
        return pane;
    }

    private void redrawIfNeeded() {
        if (!redrawNeeded) return;
        redrawNeeded = false;
        try {

            MapViewParameters mvp = mapViewParametersP.get();

            double xImage = mvp.xImage();
            double yImage = mvp.yImage();

            // Index de la première tuile que l'on va dessiner (celle qui contient
            // le coin en haut à gauche donné dans les MapViewParameters)
            int firstXIndex = Math.floorDiv( (int) xImage, PIXELS_IN_TILE);
            int firstYIndex = Math.floorDiv( (int) yImage, PIXELS_IN_TILE);

            //Nombre de tuiles sur l'axe horizontal
            int tilesInWidth = (int) canvas.getWidth() / PIXELS_IN_TILE + 1;
            //Nombre de tuiles sur l'axe vertical
            int tilesInHeight = (int) canvas.getHeight() / PIXELS_IN_TILE + 1;

            PointWebMercator topLeft = mvp.pointAt(mvp.xImage(), mvp.yImage());
            double topLeftX = mvp.viewX(topLeft);
            double topLeftY = mvp.viewY(topLeft);

            // Coordonnées du pixel correspondant au coin en haut à gauche de la
            // première tuile à dessiner sur le canevas
            double firstX =  topLeftX + firstXIndex * PIXELS_IN_TILE;
            double firstY = topLeftY + firstYIndex * PIXELS_IN_TILE;

            int xIndex = firstXIndex;
            double x = firstX;
            for (int i = 0; i <= tilesInWidth; i += 1) {
                int yIndex = firstYIndex;
                double y = firstY;
                for (int j = 0; j <= tilesInHeight; j += 1) {
                    TileManager.TileId tileId =
                            new TileManager.TileId(currentZoomLevel, xIndex, yIndex++);
                    Image image = tileManager.imageForTileAt(tileId);
                    canvas.getGraphicsContext2D()
                            .drawImage(image, x, y);
                    y += PIXELS_IN_TILE;
                }
                xIndex++;
                x += PIXELS_IN_TILE;
            }
        } catch (IOException ignored) {}  // Ne devrait jamais se produire

    }
    private void redrawOnNextPulse() {
        redrawNeeded = true;
        Platform.requestNextPulse();
    }
    private void addBindings() {
        canvas.widthProperty()
                .bind(pane.widthProperty());
        canvas.heightProperty()
                .bind(pane.heightProperty());
    }
    private void addMouseEventsManager() {

        pane.setOnMouseDragged(event -> {
            Point2D previousPosition = mousePositionP.get();
            Point2D currentPosition = new Point2D(event.getX(), event.getY());
            if (!currentPosition.equals(previousPosition)) {
                mousePositionP.set(currentPosition);
                Point2D offset = previousPosition.subtract(currentPosition);
                MapViewParameters mvp = mapViewParametersP.get();
                Point2D topLeft = mvp.topLeft();
                Point2D newTopLeft = topLeft.add(offset);
                mapViewParametersP.set(mvp.withMinXY(newTopLeft.getX(), newTopLeft.getY()));
            }
        });
        // On enregistre dans une propriété la position de la souris lors de
        // l'appui afin de pouvoir calculer le défilement
        pane.setOnMousePressed(event -> {
            Point2D currentPosition = new Point2D(event.getX(), event.getY());
            mousePositionP.set(currentPosition);

        });

        pane.setOnMouseReleased(event -> {
            if (event.isStillSincePress()) {
                MapViewParameters mvp = mapViewParametersP.get();
                // Tenter de s'en convaincre !
                PointWebMercator pwm = mvp.pointAt(
                        -event.getX(), -event.getY());
                waypointsManager.addWayPoint(pwm.x(), pwm.y());
            }
        });

        SimpleLongProperty minScrollTime = new SimpleLongProperty();
        pane.setOnScroll(e -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime < minScrollTime.get()) return;
            minScrollTime.set(currentTime + 250);
            double zoomDelta = Math.signum(e.getDeltaY());
            int newZoomLevel = currentZoomLevel + (int) zoomDelta;
            if (! (0 <= newZoomLevel  && newZoomLevel <= 19)) return;
            MapViewParameters currentMvp = mapViewParametersP.get();
            double currentMouseX = e.getX();
            double currentMouseY = e.getY();
            double currentTopLeftX = currentMvp.xImage();
            double currentTopLeftY = currentMvp.yImage();
            PointWebMercator pwm = PointWebMercator.of(currentZoomLevel, currentTopLeftX, currentTopLeftY);
            PointWebMercator mousePwm = currentMvp.pointAt(currentMouseX, currentMouseY);
            double x = pwm.xAtZoomLevel(newZoomLevel);
            double y = pwm.yAtZoomLevel(newZoomLevel);
            MapViewParameters newMvp = new MapViewParameters(newZoomLevel, x, y);
            PointWebMercator newMousePwm = newMvp.pointAt(currentMouseX, currentMouseY);
            double newMousePwmX = newMousePwm.xAtZoomLevel(newZoomLevel);
            double newMousePwmY = newMousePwm.yAtZoomLevel(newZoomLevel);
            double offsetX = newMousePwmX  - mousePwm.xAtZoomLevel(newZoomLevel);
            double offsetY = newMousePwmY  - mousePwm.yAtZoomLevel(newZoomLevel);
            mapViewParametersP.set(newMvp.withMinXY(x + offsetX, y + offsetY));
            currentZoomLevel = newZoomLevel;
        });
    }

    private void addListeners() {
        canvas.sceneProperty().addListener((p, oldS, newS) -> {
            assert oldS == null;
            newS.addPreLayoutPulseListener(this::redrawIfNeeded);
        });
        canvas.widthProperty().addListener((p) -> redrawOnNextPulse());
        canvas.heightProperty().addListener((p) -> redrawOnNextPulse());
        mapViewParametersP.addListener((p) -> redrawOnNextPulse());
    }

}